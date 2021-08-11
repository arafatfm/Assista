package com.afm.assista;

import static com.afm.assista.App.ACTION_EVENT_RECEIVED;
import static com.afm.assista.App.STOP_COMMAND;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityService extends AccessibilityService {
    private final String TAG = "xxx" + getClass().getSimpleName();

    private static boolean active = false;
    private static final Intent intent = new Intent(ACTION_EVENT_RECEIVED);
    private String eventPackageName = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            if(event.getPackageName().equals("com.android.systemui") ||
                    event.getPackageName().equals("com.google.android.inputmethod.latin"))
                return;
            if(event.getPackageName().equals(eventPackageName)) return;
            else eventPackageName = event.getPackageName().toString();
            Log.i(TAG,"TYPE_WINDOW_STATE_CHANGED");
            Log.i(TAG,event.getPackageName().toString());
            intent.putExtra("packageName", event.getPackageName());
            intent.putExtra("className", event.getClassName());
            sendBroadcast(intent);
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        active = true;
        Log.i(TAG, "Accessibility service started");
    }

    public static boolean isActive() {
        return active;
    }
}
