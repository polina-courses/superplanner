package ru.polina.blizko;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/** Каналы и показ уведомлений. */
final class Notifier {
    static final String CH_REM = "rem";
    static final String CH_DAY = "day";

    private Notifier() { }

    static void ensureChannels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel rem = new NotificationChannel(CH_REM, "Напоминания о задачах и событиях",
                NotificationManager.IMPORTANCE_HIGH);
        rem.enableVibration(true);
        NotificationChannel day = new NotificationChannel(CH_DAY, "Сводка дня, дни рождения, люди",
                NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(rem);
        nm.createNotificationChannel(day);
    }

    static void show(Context c, int id, String title, String text, String channel) {
        ensureChannels(c);
        String ch = CH_DAY.equals(channel) ? CH_DAY : CH_REM;
        Intent open = new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, ch)
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(0xFFC2552D)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();
        try {
            c.getSystemService(NotificationManager.class).notify(id, n);
        } catch (SecurityException ignored) { }
    }
}
