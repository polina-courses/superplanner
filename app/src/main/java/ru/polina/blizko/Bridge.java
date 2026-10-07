package ru.polina.blizko;

import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;

/** Методы, доступные из JavaScript как window.Native.* */
public class Bridge {
    private final MainActivity activity;

    Bridge(MainActivity activity) {
        this.activity = activity;
    }

    /** Интерфейс загрузился и работает. */
    @JavascriptInterface
    public void ready() {
        activity.pageReady = true;
    }

    /** Перезагрузить интерфейс (применить скачанное обновление). */
    @JavascriptInterface
    public void reload() {
        activity.runOnUiThread(activity::loadUi);
    }

    /** Проверить обновление интерфейса; ответ придёт в window.__updateResult(1|0|-1). */
    @JavascriptInterface
    public void checkUpdate() {
        activity.runOnUiThread(() -> activity.checkForUpdate(true));
    }

    /** Время последнего обновления интерфейса из интернета, мс (0 — встроенная версия). */
    @JavascriptInterface
    public double uiUpdatedAt() {
        return Updater.updatedAt(activity);
    }

    @JavascriptInterface
    public String appVersion() {
        return Updater.appVersionName(activity);
    }

    /** Открыть ссылку на свежий APK в браузере. */
    @JavascriptInterface
    public void openApk() {
        activity.runOnUiThread(() -> activity.openExternal(Updater.APK_URL));
    }

    @JavascriptInterface
    public String load() {
        return Store.read(activity);
    }

    @JavascriptInterface
    public void save(String json) {
        Store.write(activity, json);
    }

    @JavascriptInterface
    public void schedule(String json) {
        Scheduler.apply(activity.getApplicationContext(), json);
    }

    @JavascriptInterface
    public void test() {
        Scheduler.test(activity.getApplicationContext());
    }

    @JavascriptInterface
    public void share(String text, String name) {
        activity.runOnUiThread(() -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, name);
            i.putExtra(Intent.EXTRA_TEXT, text);
            try {
                activity.startActivity(Intent.createChooser(i, "Сохранить копию"));
            } catch (Exception ignored) { }
        });
    }

    @SuppressWarnings("deprecation")
    @JavascriptInterface
    public void barColor(String hex, boolean dark) {
        activity.runOnUiThread(() -> {
            try {
                int c = Color.parseColor(hex);
                Window w = activity.getWindow();
                w.setStatusBarColor(c);
                w.setNavigationBarColor(c);
                View d = w.getDecorView();
                int f = d.getSystemUiVisibility();
                int light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                f = dark ? (f & ~light) : (f | light);
                d.setSystemUiVisibility(f);
            } catch (Exception ignored) { }
        });
    }
}
