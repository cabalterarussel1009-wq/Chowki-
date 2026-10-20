package com.chowki.online;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.VideoView;
import android.widget.ImageView;
import android.os.Handler;
import android.os.Looper;
import android.media.MediaPlayer;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> uploadCallback;
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final String SHOP_URL = "https://chowki.netlify.app/";
    private LinearLayout root;
    private VideoView intro;
    private FrameLayout splash;
    private boolean shopShown = false;
    private MediaPlayer backgroundSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        showSplashWhileLoading();
        showShopWithIntro();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showSplashWhileLoading() {
        splash = new FrameLayout(this);
        splash.setBackgroundColor(0xFFFFFFFF);
        ImageView logoImage = new ImageView(this);
        logoImage.setImageResource(R.drawable.chowki_splash);
        logoImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        splash.addView(logoImage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(splash);
    }

    private void revealShop() {
        if (shopShown) return;
        shopShown = true;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            setContentView(root);
            startBackgroundSound();
        }, 500);
    }

    // Start the supplied video's audio separately, looping automatically inside the app.
    private void startBackgroundSound() {
        if (backgroundSound != null) {
            if (!backgroundSound.isPlaying()) backgroundSound.start();
            return;
        }
        try {
            backgroundSound = MediaPlayer.create(this, R.raw.chowki_background_sound);
            if (backgroundSound != null) {
                backgroundSound.setLooping(true);
                backgroundSound.setVolume(1.0f, 1.0f);
                backgroundSound.start();
            }
        } catch (Exception ignored) { }
    }

    // A muted, looping intro banner at the top of the app; the shop remains visible below it.
    private void showShopWithIntro() {
        intro = new VideoView(this);
        LinearLayout.LayoutParams videoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(175));
        intro.setLayoutParams(videoParams);
        intro.setBackgroundColor(0xFF111111);
        root.addView(intro);

        webView = new WebView(this);
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(webView, webParams);

        intro.setOnPreparedListener(mp -> {
            mp.setVolume(0f, 0f);
            mp.setLooping(true);
            intro.start();
        });
        intro.setOnErrorListener((mp, what, extra) -> {
            intro.setVisibility(View.GONE);
            return true;
        });
        intro.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.chowki_intro));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                revealShop();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if (scheme != null && (scheme.equals("http") || scheme.equals("https"))) {
                    view.loadUrl(uri.toString());
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                if (uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    uploadCallback = null;
                    Toast.makeText(MainActivity.this, "Hindi mabuksan ang file picker.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });
        webView.loadUrl(SHOP_URL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (uploadCallback == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) results = new Uri[]{uri};
                else results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            }
            uploadCallback.onReceiveValue(results);
            uploadCallback = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shopShown) startBackgroundSound();
    }

    @Override
    protected void onPause() {
        if (backgroundSound != null && backgroundSound.isPlaying()) backgroundSound.pause();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (uploadCallback != null) uploadCallback.onReceiveValue(null);
        if (intro != null) { intro.stopPlayback(); }
        if (backgroundSound != null) { backgroundSound.stop(); backgroundSound.release(); backgroundSound = null; }
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
