package com.enchantedhunter.vmusic.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AudioServiceNotificationReceiver extends BroadcastReceiver {

    final String LOG_TAG = "AudioServiceNotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null) {
            String action = intent.getAction();
            Log.e(LOG_TAG, "action: " + action);
            if (action.equals(AudioService.NOTIFICATION_ACTION_PREV)) {
                context.startService(new Intent(context, AudioService.class)
                        .putExtra(AudioService.SERVICE_ACTION, AudioService.ACTION.PREV.name())
                );
            }
            if (action.equals(AudioService.NOTIFICATION_ACTION_PLAY_PAUSE)) {
                context.startService(new Intent(context, AudioService.class)
                        .putExtra(AudioService.SERVICE_ACTION, AudioService.ACTION.PLAY_OR_PAUSE.name())
                );
            }
            if (action.equals(AudioService.NOTIFICATION_ACTION_NEXT)) {
                context.startService(new Intent(context, AudioService.class)
                        .putExtra(AudioService.SERVICE_ACTION, AudioService.ACTION.NEXT.name())
                );
            }
            if (action.equals(AudioService.NOTIFICATION_ACTION_STOP)) {
                context.startService(new Intent(context, AudioService.class)
                        .putExtra(AudioService.SERVICE_ACTION, AudioService.ACTION.STOP.name())
                );
            }
        }
    }
}
