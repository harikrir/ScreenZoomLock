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
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

public class DisplayLockPlugin extends CordovaPlugin {

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        enforceFixedDisplayMetrics();
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        enforceFixedDisplayMetrics();
    }

    @Override
    public boolean execute(String action,
                           JSONArray args,
                           CallbackContext callbackContext)
            throws JSONException {

        if ("enforceLock".equals(action)) {

            enforceFixedDisplayMetrics();

            callbackContext.success("Display settings locked");

            return true;
        }

        return false;
    }

    private void enforceFixedDisplayMetrics() {

        final Activity activity = cordova.getActivity();

        if (activity == null) {
            return;
        }

        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                try {

                    Resources resources = activity.getResources();

                    Configuration configuration =
                            new Configuration(resources.getConfiguration());

                    DisplayMetrics metrics =
                            resources.getDisplayMetrics();

                    configuration.fontScale = 1.0f;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                        int stableDensity =
                                DisplayMetrics.DENSITY_DEVICE_STABLE;

                        configuration.densityDpi =
                                stableDensity;

                        metrics.densityDpi =
                                stableDensity;

                        metrics.density =
                                (float) stableDensity /
                                DisplayMetrics.DENSITY_DEFAULT;

                        metrics.scaledDensity =
                                metrics.density;
                    }

                    resources.updateConfiguration(
                            configuration,
                            metrics
                    );

                    View view =
                            webView.getEngine().getView();

                    if (view instanceof WebView) {

                        WebView nativeWebView =
                                (WebView) view;

                        WebSettings settings =
                                nativeWebView.getSettings();

                        settings.setTextZoom(100);
                        settings.setSupportZoom(false);
                        settings.setBuiltInZoomControls(false);
                        settings.setDisplayZoomControls(false);
                    }

                    nativeInjectCss();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void nativeInjectCss() {

        try {

            String js =
                    "(function(){" +
                    "var style=document.createElement('style');" +
                    "style.innerHTML='html{-webkit-text-size-adjust:100%!important;text-size-adjust:100%!important;}';" +
                    "document.head.appendChild(style);" +
                    "})();";

            webView.loadUrl("javascript:" + js);

        } catch (Exception ignored) {
        }
    }
}
