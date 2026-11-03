package com.outsystems.plugin.displaylock;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class DisplayLockPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {

        if ("enforceLock".equals(action)) {

            this.callbackContext = callbackContext;

            try {
                enforceDisplaySettings();
            } catch (Exception e) {
                callbackContext.error("FAILED_TO_INITIATE_LOCK");
            }

            return true;
        }

        return false;
    }

    private void enforceDisplaySettings() {

        Activity activity = cordova.getActivity();

        if (activity == null || activity.isFinishing()) {
            if (callbackContext != null) {
                callbackContext.error("ACTIVITY_UNAVAILABLE");
            }
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
            if (callbackContext != null) {
                callbackContext.error("ACTIVITY_DESTROYED");
            }
            return;
        }

        activity.runOnUiThread(() -> {

            try {
                boolean nativeApplied = false;

                // 1. Safe Native Configuration Check (Standard phones only)
                if (isSafeForNativeMetrics(activity)) {
                    nativeApplied = applySafeNativeFontScale(activity);
                }

                // 2. Lock WebView Settings
                applyWebViewLock();

                // 3. Inject CSS text-size lock
                applyCssTextSizeLock();

                // Build Structured JSON Response
                JSONObject response = new JSONObject();
                response.put("status", "SUCCESS");
                response.put("nativeAdjusted", nativeApplied);
                response.put("message", nativeApplied 
                        ? "Full display lock applied (Native + WebView)" 
                        : "WebView display lock applied (Native preserved)");

                if (callbackContext != null) {
                    callbackContext.success(response);
                }

            } catch (Exception e) {
                if (callbackContext != null) {
                    try {
                        JSONObject errorResponse = new JSONObject();
                        errorResponse.put("status", "ERROR");
                        errorResponse.put("message", e.getMessage());
                        callbackContext.error(errorResponse);
                    } catch (Exception ex) {
                        callbackContext.error("EXECUTION_ERROR");
                    }
                }
            }
        });
    }

    private boolean isSafeForNativeMetrics(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }

        if (activity.isInMultiWindowMode()) {
            return false;
        }

        Configuration config = activity.getResources().getConfiguration();
        return config.smallestScreenWidthDp < 600;
    }

    @SuppressWarnings("deprecation")
    private boolean applySafeNativeFontScale(Activity activity) {
        try {
            Resources res = activity.getResources();
            Configuration config = res.getConfiguration();

            if (config.fontScale != 1.0f) {
                config.fontScale = 1.0f;
                DisplayMetrics metrics = res.getDisplayMetrics();
                metrics.scaledDensity = metrics.density * config.fontScale;

                res.updateConfiguration(config, metrics);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void applyWebViewLock() {
        try {
            if (webView == null || webView.getEngine() == null) {
                return;
            }

            View engineView = webView.getEngine().getView();
            if (engineView instanceof WebView) {
                WebView nativeWebView = (WebView) engineView;
                WebSettings settings = nativeWebView.getSettings();

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
            if (webView == null) {
                return;
            }

            String js = "(function(){"
                    + "if(document.getElementById('display-lock-style')) return;"
                    + "var s = document.createElement('style');"
                    + "s.id = 'display-lock-style';"
                    + "s.innerHTML = 'html, body { -webkit-text-size-adjust: 100% !important; text-size-adjust: 100% !important; }';"
                    + "if(document.head){ document.head.appendChild(s); }"
                    + "})();";

            webView.loadUrl("javascript:" + js);
        } catch (Exception ignored) {
        }
    }
}
