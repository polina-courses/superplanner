package ru.polina.blizko;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Срабатывает в запланированное время и показывает уведомление. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Notifier.show(context,
                intent.getIntExtra("id", 1),
                intent.getStringExtra("title"),
                intent.getStringExtra("text"),
                intent.getStringExtra("ch"));
    }
}
