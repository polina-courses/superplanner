package ru.polina.blizko;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** После перезагрузки, обновления приложения или смены времени заново ставит напоминания. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Scheduler.restore(context);
    }
}
