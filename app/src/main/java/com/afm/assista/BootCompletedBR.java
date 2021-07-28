package com.afm.assista;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import java.io.IOException;

import static com.afm.assista.App.CLASS_NAME;
import static com.afm.assista.App.FILENAME_APP_STATE;

public class BootCompletedBR extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Intent serviceIntent = new Intent(context, ForegroundService.class);
        serviceIntent.putExtra(CLASS_NAME, getClass().getSimpleName());

        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            IOOperation ioO = new IOOperation();
            try {
                if((boolean) ioO.loadObjectFromStorage(FILENAME_APP_STATE)) {
                    ContextCompat.startForegroundService(context, serviceIntent);
                }
            } catch (IOException | ClassNotFoundException exception) {
                exception.printStackTrace();
            }
        }
    }
}
