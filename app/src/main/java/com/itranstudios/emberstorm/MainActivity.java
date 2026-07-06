package com.itranstudios.emberstorm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.*;

public class MainActivity extends Activity implements BillingManager.Callback {
    private WebView web;
    private BillingManager billing;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web = new WebView(this);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        web.setBackgroundColor(0xFF050418);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.addJavascriptInterface(new JsBridge(), "Android");
        billing = new BillingManager(this, this);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) { billing.start(); }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/emberstorm.html");
        hideSystemUI();
    }

    class JsBridge {
        @JavascriptInterface
        public void buy(final String sku) {
            runOnUiThread(() -> billing.launchPurchase(MainActivity.this, sku));
        }
    }

    @Override public void onGrant(String sku) { evalJs("window.grantEntitlement&&window.grantEntitlement('" + esc(sku) + "')"); }
    @Override public void onCoins(int n)      { evalJs("window.grantCoins&&window.grantCoins(" + n + ")"); }
    @Override public void onPrices(String j)  { evalJs("window.setPrices&&window.setPrices(" + j + ")"); }
    @Override public void onFailed()          { evalJs("window.purchaseFailed&&window.purchaseFailed()"); }

    private void evalJs(final String js) { runOnUiThread(() -> { if (web != null) web.evaluateJavascript(js, null); }); }
    private static String esc(String s) { return s.replace("\\", "\\\\").replace("'", "\\'"); }

    @Override public void onWindowFocusChanged(boolean hasFocus) { super.onWindowFocusChanged(hasFocus); if (hasFocus) hideSystemUI(); }
    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
    @Override public void onBackPressed() { if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }
    @Override protected void onPause() { super.onPause(); if (web != null) web.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
}
