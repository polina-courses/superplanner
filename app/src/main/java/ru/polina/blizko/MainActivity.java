package ru.polina.blizko;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Единственный экран: веб-интерфейс + мост к системе (Native). */
public class MainActivity extends Activity {
    private static final int REQ_FILE = 11;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private boolean fromCache;
    volatile boolean pageReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Notifier.ensureChannels(this);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("file".equals(u.getScheme())) return false;
                openExternal(u.toString());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // Страховка: если скачанная версия интерфейса не запустилась — вернуться к встроенной.
                if (fromCache) {
                    ui.postDelayed(() -> {
                        if (!pageReady && fromCache) {
                            Updater.rejectCached(MainActivity.this);
                            fromCache = false;
                            web.loadUrl(Updater.ASSET_URL);
                        }
                    }, 6000);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Резервная копия"), REQ_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(this), "Native");

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        loadUi();
        checkForUpdate(false);
    }

    /** Загрузить интерфейс: скачанную версию, если есть, иначе встроенную. */
    void loadUi() {
        String url = Updater.startUrl(this);
        fromCache = !url.equals(Updater.ASSET_URL);
        pageReady = false;
        web.loadUrl(url);
    }

    /** Проверка обновления интерфейса в фоне. manual — нажата кнопка «Проверить». */
    void checkForUpdate(boolean manual) {
        if (!manual && !Updater.due(this)) return;
        new Thread(() -> {
            int r = Updater.check(getApplicationContext());
            ui.post(() -> {
                if (web == null) return;
                if (manual) web.evaluateJavascript("window.__updateResult&&window.__updateResult(" + r + ")", null);
                else if (r == Updater.UPDATED) web.evaluateJavascript("window.__updateReady&&window.__updateReady()", null);
            });
        }).start();
    }

    void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) { }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && fileCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                result = new Uri[]{data.getData()};
            }
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) {
            web.evaluateJavascript("window.__resume&&window.__resume()", null);
            checkForUpdate(false);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.__back&&window.__back())?'1':'0'", value -> {
            if (!"\"1\"".equals(value)) finish();
        });
    }
}
