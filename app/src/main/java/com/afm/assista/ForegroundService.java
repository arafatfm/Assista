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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ForegroundService extends Service {
    private final String TAG = "xxx" + getClass().getSimpleName();
    public static boolean isRunning;
    private BroadcastReceiver receiver;
    public static ScheduledExecutorService executor, timer;
    public static boolean executorRunning, timerRunning, timedOut;
    public static MyCalendar logStart, logEnd;
    private static File location;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);

        receiver = new ScreenBR();
        registerReceiver(receiver, filter);

        location = getExternalFilesDir(null);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String className = intent.getStringExtra("IntentExtra");

        if(className.equals("LoggerActivity")) {
            long time = intent.getLongExtra("TimerExtra", LoggerActivity.TIMER_DEFAULT);
            timer = Executors.newSingleThreadScheduledExecutor();
            timer.schedule(this::timerTimeOut, time, TimeUnit.MINUTES);
            logStart = new MyCalendar(Calendar.getInstance());
            timerRunning = true;
            List<List<MyCalendar>> thisList = LoggerActivity.map.get(LoggerActivity.purpose);
            if(thisList != null) {
                thisList.add(new ArrayList<>());
                addToMap(logStart);
            }
        } else {
            startExecutor();
        }

        Intent stopService = new Intent(this, NotificationBR.class);
        PendingIntent pIntent = PendingIntent.getBroadcast(this, 0, stopService, 0);

        Notification notification = new NotificationCompat.Builder(this,App.getChannelId())
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
        isRunning = false;
        unregisterReceiver(receiver);
        interruptTimer();
        stopExecutor();
        LoggerActivity.purpose = "";

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
        timedOut = true;
        logEnd = new MyCalendar(Calendar.getInstance());
        addToMap(logEnd);
        LoggerActivity.purpose = "";
        startExecutor();
    }

    private static void addToMap(MyCalendar data) {
        List<List<MyCalendar>> thisList = LoggerActivity.map.get(LoggerActivity.purpose);
        int index;
        if(thisList != null) {
            index = thisList.size() - 1;
            thisList.get(index).add(data);
        }

        File fileMap = new File(location, LoggerActivity.FILE_MAP);
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileMap))) {
            oos.writeObject(LoggerActivity.map);
        } catch (FileNotFoundException fnfException) {
            Log.d("xxxForegroundService", "writeMap: " + fnfException);
        } catch (IOException ioException) {
            Log.d("xxxForegroundService", "writeMap:  " + ioException);
        }
    }

    public static void interruptTimer() {
        if(timerRunning) {
            if(!timer.isShutdown()) {
                timer.shutdownNow();
                timerRunning = false;
                logEnd = new MyCalendar(Calendar.getInstance());
                addToMap(logEnd);
                LoggerActivity.purpose = "";
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


    public static boolean isRunning() {
        return isRunning;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
