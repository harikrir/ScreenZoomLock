package com.outsystems.plugin.displaylock;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;

public class DisplayLockPlugin extends CordovaPlugin {

    private static final String TAG = "DisplayLockPlugin";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        enforceFixedDisplayMetrics();
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        // Re-enforce in case the user changed display settings while the app was in the background
        enforceFixedDisplayMetrics();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        enforceFixedDisplayMetrics();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("enforceLock".equals(action)) {
            enforceFixedDisplayMetrics();
            callbackContext.success("Display density locked.");
            return true;
        }
        return false;
    }

    private void enforceFixedDisplayMetrics() {
        final Activity activity = cordova.getActivity();
        if (activity == null) return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Resources res = activity.getResources();
                    Configuration config = res.getConfiguration();
                    DisplayMetrics metrics = res.getDisplayMetrics();

                    // 1. Lock font scale factor to 1.0 (100%)
                    config.fontScale = 1.0f;

                    // 2. Lock densityDpi to the hardware's unscaled physical default
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        config.densityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE;
                        metrics.densityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE;
                        metrics.density = (float) DisplayMetrics.DENSITY_DEVICE_STABLE / (float) DisplayMetrics.DENSITY_DEFAULT;
                        metrics.scaledDensity = metrics.density;
                    }

                    // Apply the forced configuration back to the Activity resources
                    res.updateConfiguration(config, metrics);

                    // 3. Lock native WebView scaling
                    if (webView.getEngine() != null && webView.getEngine().getView() instanceof WebView) {
                        WebView nativeWebView = (WebView) webView.getEngine().getView();
                        WebSettings settings = nativeWebView.getSettings();
                        settings.setTextZoom(100);
                        settings.setSupportZoom(false);
                        settings.setBuiltInZoomControls(false);
                        settings.setDisplayZoomControls(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
