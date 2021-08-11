package com.afm.assista;

import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class App extends Application {
//    public static final TimeUnit TIME_UNIT = TimeUnit.SECONDS; //for testing purpose
    public static final TimeUnit TIME_UNIT = TimeUnit.MINUTES;
    public static final long EXECUTOR_DELAY = 10; //seconds
    public static final String CHANNEL_ID ="notificationChannel";
    public static final String FILENAME_MAP = "database.dat";
    public static final String FILENAME_TIMER = "timer.dat";
    public static final String FILENAME_PURPOSE = "purpose.dat";
    public static final String FILENAME_APP_STATE = "state.dat";
    public static final String FILENAME_BLACKLIST = "blacklist.dat";
    public static final String CLASS_NAME = "intentExtraClassName";
    public static final String STOP_COMMAND = "intentExtraStopThis";
    public static final String ACTION_STOP = "intentActionStopThis";
    public static final String ACTION_STOP_EXECUTOR = "intentActionStopExecutor";
    public static final String ACTION_EVENT_RECEIVED = "intentActionAccessibilityEvent";
    public static final IOOperation ioO = new IOOperation();
    public static final long TIMER_DEFAULT = 2; //minutes
    public static String purpose = "";
    public static File FILE_DIRECTORY;
    public static ActivityManager activityManager;

    private static Map<String, List<List<MyCalendar>>> map;
    private static Map<String, Long> blacklist;
    private static boolean foregroundServiceRunning = false;
    private static boolean appActive = false;
    private static boolean timedOut = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        FILE_DIRECTORY = getExternalFilesDir(null);
        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        getMapFromStorage();
        getBlacklistFromStorage();
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Default",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    public static Map<String, Long> getBlacklist() { return blacklist; }
    public static void setBlacklist(Map<String, Long> blacklist) { App.blacklist = blacklist; }
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


    @SuppressWarnings("unchecked")
    private void getMapFromStorage() {
        try {
            setMap((Map<String, List<List<MyCalendar>>>) ioO.loadObjectFromStorage(FILENAME_MAP));
        } catch (IOException | ClassNotFoundException exception) {
            exception.printStackTrace();
            setMap(new HashMap<>());
        }
    }
    @SuppressWarnings("unchecked")
    private void getBlacklistFromStorage() {
        try {
            setBlacklist((Map<String, Long>) ioO.loadObjectFromStorage(FILENAME_BLACKLIST));
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            setBlacklist(new HashMap<>());
        }
    }
}
