package com.afm.assista;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;
import static com.afm.assista.App.purpose;

//this class is used to detect screen on off action

public class ScreenBR extends BroadcastReceiver {
    private final String TAG = "xxx" + getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {

            Intent serviceIntent = new Intent(context, ForegroundService.class);
            serviceIntent.putExtra("IntentExtra", getClass().getSimpleName());
            ContextCompat.startForegroundService(context, serviceIntent);

        } else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {

            ForegroundService.interruptTimer();
            ForegroundService.stopExecutor();
            purpose = "";
        }
    }
}
