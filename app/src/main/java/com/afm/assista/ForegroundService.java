package com.afm.assista;

import static com.afm.assista.App.ACTION_EVENT_RECEIVED;
import static com.afm.assista.App.ACTION_STOP;
import static com.afm.assista.App.ACTION_STOP_EXECUTOR;
import static com.afm.assista.App.CHANNEL_ID;
import static com.afm.assista.App.CLASS_NAME;
import static com.afm.assista.App.EXECUTOR_DELAY;
import static com.afm.assista.App.FILENAME_APP_STATE;
import static com.afm.assista.App.FILENAME_MAP;
import static com.afm.assista.App.STOP_COMMAND;
import static com.afm.assista.App.TIMER_DEFAULT;
import static com.afm.assista.App.TIME_UNIT;
import static com.afm.assista.App.getBlacklist;
import static com.afm.assista.App.getMap;
import static com.afm.assista.App.ioO;
import static com.afm.assista.App.isAppActive;
import static com.afm.assista.App.purpose;
import static com.afm.assista.App.setAppActive;
import static com.afm.assista.App.setForegroundServiceRunning;
import static com.afm.assista.App.setTimedOut;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.TelephonyManager;
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

public class ForegroundService extends Service {
    private final String TAG = "xxx" + getClass().getSimpleName();
    private ScheduledExecutorService executor, timer;
    private boolean executorRunning, timerRunning;
    private MyCalendar logEndTime;
    private boolean isPhoneIdle = true;
    private String eventPackageName;

    private final BroadcastReceiver eventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            eventPackageName = intent.getStringExtra("packageName");
//            eventClassName = intent.getStringExtra("className");
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    if(isPhoneIdle)
                        startExecutor();
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    interruptTimer();
                    stopExecutor();
                    purpose = "";
                    break;
                case ACTION_STOP_EXECUTOR:
                    stopExecutor();
                    break;
                case TelephonyManager.ACTION_PHONE_STATE_CHANGED:
                    String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

                    if(state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        isPhoneIdle = true;
                        if(!timerRunning) {
                            startExecutor();
                        }
                    } else {
                        isPhoneIdle = false;
                        stopExecutor();
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        setForegroundServiceRunning(true);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(ACTION_STOP_EXECUTOR);
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(receiver, filter);
        registerReceiver(eventReceiver, new IntentFilter(ACTION_EVENT_RECEIVED));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.hasExtra(STOP_COMMAND)) {
            stopExecutor();
            sendBroadcast(new Intent(ACTION_STOP));
            stopSelf();

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
                MyCalendar logStartTime = new MyCalendar(Calendar.getInstance());
                long time = intent.getLongExtra("TimerExtra", TIMER_DEFAULT);

                timer = Executors.newSingleThreadScheduledExecutor();
                timer.schedule(this::timerTimeOut, time, TIME_UNIT);
                timerRunning = true;

                List<List<MyCalendar>> calendarList = getMap().get(purpose);
                if(calendarList != null) {
                    calendarList.add(new ArrayList<>());
                    addToMap(logStartTime);
                }
            } else {
                startExecutor();
            }
        }

        Intent stopService = new Intent(this, ForegroundService.class);
        stopService.putExtra(STOP_COMMAND, "stopForegroundService");

        PendingIntent pIntent = PendingIntent.getService(this, 0, stopService, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Assista is running")
                .setSmallIcon(R.drawable.ic_notification)
                .addAction(0, "Stop running", pIntent)
                .setOnlyAlertOnce(true)
                .build();

        startForeground(100, notification);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setForegroundServiceRunning(false);
        unregisterReceiver(receiver);
        unregisterReceiver(eventReceiver);
        interruptTimer();
        stopExecutor();
        purpose = "";
        sendBroadcast(new Intent("ForegroundStopped"));
    }

    private void startExecutor() {
        if(!executorRunning){
            executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleWithFixedDelay(() -> {
                Log.i("TAG", "Executor started");
                executorRunning = true;

                if(getBlacklist().containsKey(eventPackageName)) {
                    Log.i(TAG, "startExecutor: blacklist found");
                    Intent intenT = new Intent(this, LoggerActivity.class);
                    intenT.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intenT);
                }
            }, EXECUTOR_DELAY, EXECUTOR_DELAY, TimeUnit.SECONDS);
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

    private void addToMap(MyCalendar data) {
        List<List<MyCalendar>> calendarList = getMap().get(purpose);
        int index;
        if(calendarList != null) {
            index = calendarList.size() - 1;
            calendarList.get(index).add(data);
        }

        try {
            ioO.saveObjectToStorage(getMap(), FILENAME_MAP);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public void interruptTimer() {
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

    public void stopExecutor() {
        if(executorRunning) {
            if(!executor.isShutdown()) {
                executor.shutdownNow();
                executorRunning = false;
            }
        }
        this.sendBroadcast(new Intent(ACTION_STOP));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
