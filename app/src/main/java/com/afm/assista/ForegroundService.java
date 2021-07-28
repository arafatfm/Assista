package com.afm.assista;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.afm.assista.App.ACTION_STOP;
import static com.afm.assista.App.CHANNEL_ID;
import static com.afm.assista.App.CLASS_NAME;
import static com.afm.assista.App.FILENAME_APP_STATE;
import static com.afm.assista.App.FILENAME_MAP;
import static com.afm.assista.App.STOP_COMMAND;
import static com.afm.assista.App.TIMER_DEFAULT;
import static com.afm.assista.App.TIME_UNIT;
import static com.afm.assista.App.getMap;
import static com.afm.assista.App.ioO;
import static com.afm.assista.App.isAppActive;
import static com.afm.assista.App.purpose;
import static com.afm.assista.App.setAppActive;
import static com.afm.assista.App.setForegroundServiceRunning;
import static com.afm.assista.App.setTimedOut;

public class ForegroundService extends Service {
    private final String TAG = "xxx" + getClass().getSimpleName();
    private BroadcastReceiver receiverScreen;
    private static ScheduledExecutorService executor, timer;
    private static boolean executorRunning, timerRunning;
    private static MyCalendar logEndTime;

    @Override
    public void onCreate() {
        super.onCreate();
        setForegroundServiceRunning(true);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        receiverScreen = new ScreenBR();
        registerReceiver(receiverScreen, filter);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.hasExtra(STOP_COMMAND)) {
            stopSelf();
            sendBroadcast(new Intent(ACTION_STOP));

            setAppActive(false);
            try {
                ioO.saveObjectToStorage(isAppActive(), FILENAME_APP_STATE);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        if(intent.hasExtra(CLASS_NAME)) {
            String className = intent.getStringExtra(CLASS_NAME);

            if(className.equals("LoggerActivity")) {
                long time = intent.getLongExtra("TimerExtra", TIMER_DEFAULT);
                timer = Executors.newSingleThreadScheduledExecutor();
                timer.schedule(this::timerTimeOut, time, TIME_UNIT);
                MyCalendar logStartTime = new MyCalendar(Calendar.getInstance());
                timerRunning = true;
                List<List<MyCalendar>> thisList = getMap().get(purpose);
                if(thisList != null) {
                    thisList.add(new ArrayList<>());
                    addToMap(logStartTime);
                }
            } else {
                startExecutor();
            }
        }

        Intent stopService = new Intent(this, ForegroundService.class);
        stopService.putExtra(STOP_COMMAND, "stopForegroundService");

        PendingIntent pIntent = PendingIntent.getService(this, 0, stopService, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Assista is running")
                .setSmallIcon(R.drawable.ic_notification)
                .addAction(0, "Stop running", pIntent)
                .build();

        startForeground(1, notification);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setForegroundServiceRunning(false);
        unregisterReceiver(receiverScreen);
        interruptTimer();
        stopExecutor();
        purpose = "";
        LoggerActivity.deleteLastPurpose();

    }

    private void startExecutor() {
        if(!executorRunning){
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleWithFixedDelay(() -> {
                Log.i("TAG", "Executor started");
                executorRunning = true;
                Intent intenT = new Intent(this, LoggerActivity.class);
                intenT.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intenT);
            }, 0, 3, TimeUnit.SECONDS);
        }
    }

    private void timerTimeOut() {
        timerRunning = false;
        setTimedOut(true);
        logEndTime = new MyCalendar(Calendar.getInstance());
        addToMap(logEndTime);
        purpose = "";
        startExecutor();
    }

    private static void addToMap(MyCalendar data) {
        List<List<MyCalendar>> thisList = getMap().get(purpose);
        int index;
        if(thisList != null) {
            index = thisList.size() - 1;
            thisList.get(index).add(data);
        }

        try {
            ioO.saveObjectToStorage(getMap(), FILENAME_MAP);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public static void interruptTimer() {
        if(timerRunning) {
            if(!timer.isShutdown()) {
                timer.shutdownNow();
                timerRunning = false;
                logEndTime = new MyCalendar(Calendar.getInstance());
                addToMap(logEndTime);
                purpose = "";
            }
        }
    }

    public static void stopExecutor() {
        if(executorRunning) {
            if(!executor.isShutdown()) {
                executor.shutdownNow();
                executorRunning = false;
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
