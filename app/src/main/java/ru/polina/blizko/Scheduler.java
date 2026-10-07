package ru.polina.blizko;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Планирует системные будильники для уведомлений.
 * Список приходит из JavaScript: [{id, at (мс), title, text, ch}].
 */
final class Scheduler {
    private static final String PREFS = "schedule";
    private static final String KEY = "list";

    private Scheduler() { }

    static synchronized void apply(Context c, String json) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        // снять всё, что было запланировано раньше
        try {
            JSONArray old = new JSONArray(sp.getString(KEY, "[]"));
            for (int i = 0; i < old.length(); i++) {
                am.cancel(pending(c, old.getJSONObject(i).getInt("id"), null));
            }
        } catch (Exception ignored) { }
        sp.edit().putString(KEY, json).apply();
        setAll(c, json);
    }

    /** Повторно ставит сохранённый список (после перезагрузки телефона). */
    static synchronized void restore(Context c) {
        String json = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        setAll(c, json);
    }

    private static void setAll(Context c, String json) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        long now = System.currentTimeMillis();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                long at = o.getLong("at");
                if (at <= now) continue;
                set(c, am, at, pending(c, o.getInt("id"), o));
            }
        } catch (Exception ignored) { }
    }

    static void test(Context c) {
        try {
            JSONObject o = new JSONObject();
            o.put("title", "Близко");
            o.put("text", "Уведомления работают 👍");
            o.put("ch", "rem");
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            set(c, am, System.currentTimeMillis() + 5000, pending(c, 1, o));
        } catch (Exception ignored) { }
    }

    private static void set(Context c, AlarmManager am, long at, PendingIntent pi) {
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    private static PendingIntent pending(Context c, int id, JSONObject o) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setAction("ru.polina.blizko.ALARM_" + id);
        if (o != null) {
            i.putExtra("id", id);
            i.putExtra("title", o.optString("title", "Близко"));
            i.putExtra("text", o.optString("text", ""));
            i.putExtra("ch", o.optString("ch", "rem"));
        }
        return PendingIntent.getBroadcast(c, id, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
