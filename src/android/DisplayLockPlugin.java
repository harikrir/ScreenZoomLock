package com.outsystems.plugin.displaylock;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

public class DisplayLockPlugin extends CordovaPlugin {

    private static final String ACTION_ENFORCE_LOCK = "enforceLock";

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (ACTION_ENFORCE_LOCK.equals(action)) {
            final Activity activity = cordova.getActivity();

            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                callbackContext.error("Activity is not in a valid state");
                return true;
            }

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean isSafeForNativeMetrics = isSafeToModifyNativeMetrics(activity);

                        // 1. Safe Native Font Scale adjustment (only for standard handheld phones)
                        if (isSafeForNativeMetrics) {
                            applySafeNativeFontScale(activity);
                        }

                        // 2. Safe WebView Text Zoom (Universal and isolated to this app's WebView)
                        applyWebViewLock();

                        // 3. Isolated CSS adjustment
                        applyCssTextSizeLock();

                        callbackContext.success(isSafeForNativeMetrics 
                            ? "Full display lock applied" 
                            : "Partial display lock applied (safe fallback)");
                    } catch (Exception e) {
                        // Fail silently or report without crashing the app
                        callbackContext.error("Display lock skipped due to safety check: " + e.getMessage());
                    }
                }
            });

            return true;
        }

        return false;
    }

    /**
     * Determines whether modifying native Activity resources is safe on this device.
     */
    private boolean isSafeToModifyNativeMetrics(Activity activity) {
        // Exclude devices below Android 7.0 (API 24) to avoid runtime instability
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }

        // Do not touch native metrics if the app is currently in Multi-Window / Split-Screen mode
        if (activity.isInMultiWindowMode()) {
            return false;
        }

        // Check if device is a Tablet (sw600dp or larger)
        Configuration config = activity.getResources().getConfiguration();
        boolean isTablet = (config.smallestScreenWidthDp >= 600);
        if (isTablet) {
            // Keep tablet scaling untouched to prevent unexpected UI stretching
            return false;
        }

        return true;
    }

    private void applySafeNativeFontScale(Activity activity) {
        try {
            Resources res = activity.getResources();
            Configuration config = res.getConfiguration();

            // Only update if the user has drifted from standard scale (1.0f)
            if (config.fontScale != 1.0f) {
                config.fontScale = 1.0f;
                DisplayMetrics metrics = res.getDisplayMetrics();
                metrics.scaledDensity = metrics.density * config.fontScale;
                
                // Safe localized update
                res.updateConfiguration(config, metrics);
            }
        } catch (Exception ignored) {
            // If the OEM OS blocks configuration mutation, fail silently
        }
    }

    private void applyWebViewLock() {
        try {
            View engineView = webView.getEngine().getView();
            if (engineView instanceof WebView) {
                WebView nativeWebView = (WebView) engineView;
                WebSettings settings = nativeWebView.getSettings();

                // WebSettings.setTextZoom is safe across all Android versions (API 1+)
                // and strictly scopes to this WebView without touching system settings
                settings.setTextZoom(100);
                settings.setSupportZoom(false);
                settings.setBuiltInZoomControls(false);
                settings.setDisplayZoomControls(false);
            }
        } catch (Exception ignored) {
        }
    }

    private void applyCssTextSizeLock() {
        try {
            String js = "(function(){"
                    + "if (document.getElementById('display-lock-style')) return;"
                    + "var style = document.createElement('style');"
                    + "style.id = 'display-lock-style';"
                    + "style.innerHTML = 'html, body { -webkit-text-size-adjust: 100% !important; text-size-adjust: 100% !important; }';"
                    + "if (document.head) { document.head.appendChild(style); }"
                    + "})();";

            webView.loadUrl("javascript:" + js);
        } catch (Exception ignored) {
        }
    }
}
