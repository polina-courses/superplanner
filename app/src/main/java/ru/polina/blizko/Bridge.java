package ru.polina.blizko;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;

/** Методы, доступные из JavaScript как window.Native.* */
public class Bridge {
    private final Activity activity;

    Bridge(Activity activity) {
        this.activity = activity;
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
