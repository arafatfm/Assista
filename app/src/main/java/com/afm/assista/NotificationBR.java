package com.afm.assista;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationBR extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.stopService(new Intent(context, ForegroundService.class));
    }
}
