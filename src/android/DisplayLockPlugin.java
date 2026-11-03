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
                enforceScreenZoomLock();
            } catch (Exception e) {
                callbackContext.error("FAILED_TO_INITIATE_LOCK");
            }
            return true;
        }

        return false;
    }

    private void enforceScreenZoomLock() {

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
                int stableDpi = DisplayMetrics.DENSITY_DEVICE_STABLE;
                int currentDpi = activity.getResources().getConfiguration().densityDpi;
                boolean dpiChanged = false;

                // 1. Force native DisplayMetrics and Configuration back to hardware stable DPI
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Resources res = activity.getResources();
                    Configuration config = res.getConfiguration();

                    if (config.densityDpi != stableDpi) {
                        config.densityDpi = stableDpi;

                        DisplayMetrics metrics = res.getDisplayMetrics();
                        metrics.densityDpi = stableDpi;
                        metrics.density = (float) stableDpi / DisplayMetrics.DENSITY_DEFAULT;

                        res.updateConfiguration(config, metrics);
                        dpiChanged = true;
                    }
                }

                // 2. Lock WebView settings against user gestures
                lockWebViewZoomControls();

                // 3. Normalize viewport density in Chromium DOM
                normalizeWebViewportDensity(stableDpi, currentDpi);

                // Build response
                JSONObject response = new JSONObject();
                response.put("status", "SUCCESS");
                response.put("originalDpi", currentDpi);
                response.put("lockedDpi", stableDpi);
                response.put("dpiAdjusted", dpiChanged);

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

    private void lockWebViewZoomControls() {
        try {
            if (webView == null || webView.getEngine() == null) {
                return;
            }

            View engineView = webView.getEngine().getView();
            if (engineView instanceof WebView) {
                WebView nativeWebView = (WebView) engineView;
                WebSettings settings = nativeWebView.getSettings();

                // Keep WebSettings zoom mechanisms disabled
                settings.setSupportZoom(false);
                settings.setBuiltInZoomControls(false);
                settings.setDisplayZoomControls(false);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Enforces viewport density scaling dynamically inside the Chromium DOM
     * so OutSystems layouts recalculate container widths according to hardware baseline DPI.
     */
    private void normalizeWebViewportDensity(int stableDpi, int currentDpi) {
        try {
            if (webView == null) {
                return;
            }

            // Ratio of original user zoom vs standard stable hardware density
            double scaleRatio = (double) stableDpi / (double) (currentDpi > 0 ? currentDpi : stableDpi);

            String js = "(function(){"
                    + "var meta = document.querySelector('meta[name=\"viewport\"]');"
                    + "if(!meta){"
                    + "  meta = document.createElement('meta');"
                    + "  meta.name = 'viewport';"
                    + "  document.head.appendChild(meta);"
                    + "}"
                    + "var ratio = " + scaleRatio + ";"
                    + "meta.content = 'width=device-width, initial-scale=' + ratio.toFixed(4) + ', maximum-scale=' + ratio.toFixed(4) + ', minimum-scale=' + ratio.toFixed(4) + ', user-scalable=no, viewport-fit=cover';"
                    + "window.dispatchEvent(new Event('resize'));"
                    + "})();";

            webView.loadUrl("javascript:" + js);
        } catch (Exception ignored) {
        }
    }
}
