package com.afm.assista;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class App extends Application {
    public static final TimeUnit TIME_UNIT = TimeUnit.SECONDS; //for testing purpose
    public static final String CHANNEL_ID ="myServiceChannel";
    public static final String FILENAME_MAP = "database.dat";
    public static final String FILENAME_TIMER = "timer.dat";
    public static final String FILENAME_PURPOSE = "purpose.dat";
    public static final String FILENAME_APP_STATE = "state.dat";
    public static final String CLASS_NAME = "intentExtraClassName";
    public static final String STOP_COMMAND = "intentExtraStopThis";
    public static final String ACTION_STOP = "intentActionStopThis";
    public static final IOOperation ioO = new IOOperation();
    public static final long TIMER_DEFAULT = 2; //minutes
    public static String purpose = "";
    public static File FILE_DIRECTORY;

    private static Map<String, List<List<MyCalendar>>> map;
    private static boolean foregroundServiceRunning = false;
    private static boolean appActive = false;
    private static boolean timedOut = false;

    @Override
    public void onCreate() {
        super.onCreate();
        FILE_DIRECTORY = getExternalFilesDir(null);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "My Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public static Map<String, List<List<MyCalendar>>> getMap() {
        return map;
    }
    public static void setMap(Map<String, List<List<MyCalendar>>> map) {
        App.map = map;
    }
    public static boolean isAppActive() {
        return appActive;
    }
    public static void setAppActive(boolean appActive) {
        App.appActive = appActive;
    }
    public static boolean isForegroundServiceRunning() {
        return foregroundServiceRunning;
    }
    public static void setForegroundServiceRunning(boolean foregroundServiceRunning) {
        App.foregroundServiceRunning = foregroundServiceRunning;
    }
    public static boolean isTimedOut() {
        return timedOut;
    }
    public static void setTimedOut(boolean timedOut) {
        App.timedOut = timedOut;
    }
}
