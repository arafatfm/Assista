package com.afm.assista;

import static com.afm.assista.App.ACTION_STOP;
import static com.afm.assista.App.ACTION_STOP_EXECUTOR;
import static com.afm.assista.App.CLASS_NAME;
import static com.afm.assista.App.FILENAME_PURPOSE;
import static com.afm.assista.App.FILENAME_TIMER;
import static com.afm.assista.App.TIMER_DEFAULT;
import static com.afm.assista.App.getMap;
import static com.afm.assista.App.ioO;
import static com.afm.assista.App.isForegroundServiceRunning;
import static com.afm.assista.App.isTimedOut;
import static com.afm.assista.App.purpose;
import static com.afm.assista.App.setTimedOut;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoggerActivity extends AppCompatActivity {
    private static final int MINIMUM_PURPOSE_LENGTH = 2;
    private final String TAG = "xxx" + getClass().getSimpleName();

    private long timerDefault;
    private RecyclerView recyclerViewHistory;
    private TextView textViewPurpose, textViewTotalTimeSpent;
    private CardView cardViewHistory, cardViewPurpose;
    private EditText editTextTimer;
    private AutoCompleteTextView acTextView;
    private String lastPurpose;
    RvHistoryAdapter rvHistoryAdapter;
    private Button buttonBegin;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger);
        Log.i(TAG, "onCreate: called");


        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finishAndRemoveTask();
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_STOP);
        registerReceiver(receiver, filter);

        initialization();
        getTimerDefaultFromStorage();
        getLastPurposeFromStorage();
        timeUpAction();

        ArrayAdapter<String> adapterACTV = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>(getMap().keySet()));
        acTextView.setAdapter(adapterACTV);

        cardViewPurpose.setOnClickListener(v -> {
            if(!textViewPurpose.getText().equals(getResources().getString(R.string.textViewPurpose))) {
                acTextView.setText(textViewPurpose.getText());
            }
            inputAction();
            if(cardViewHistory.getVisibility() == View.VISIBLE) cardViewHistory.setVisibility(View.GONE);
            else cardViewHistory.setVisibility(View.VISIBLE);
        });

        buttonBegin.setOnLongClickListener(v -> {
            setTimerDefault();
            return true;
        });

        buttonBegin.setOnClickListener(v -> buttonBeginClickAction());

        acTextView.setOnKeyListener((v, keyCode, event) -> {
            if(keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                inputAction();
            }
            return false;
        });

        acTextView.setOnItemClickListener((parent, view, position, id) -> inputAction());

        editTextTimer.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().isEmpty()) {
                    editTextTimer.setHint(timerDefault + " minutes");
                } else {
                    editTextTimer.setHint("");
                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

    }

    private void buttonBeginClickAction() {
        hideKeyboard();
        String str = acTextView.getText().toString().trim();
        if(str.length() > MINIMUM_PURPOSE_LENGTH) {
            sendBroadcast(new Intent(ACTION_STOP_EXECUTOR));

            setPurpose(str);

            str = editTextTimer.getText().toString().trim();

            Intent serviceIntent = new Intent(this, ForegroundService.class);
            serviceIntent.putExtra(CLASS_NAME, getClass().getSimpleName());
            serviceIntent.putExtra("TimerExtra", getTimer(str));
            ContextCompat.startForegroundService(this, serviceIntent);

            setLastPurposeToStorage();

            finishAndRemoveTask();
        }
    }
    private void setPurpose(String str) {
        str = str.substring(0,1).toUpperCase() +
                str.substring(1).toLowerCase();
        lastPurpose = purpose = str;

        if(!getMap().containsKey(purpose)) {
            getMap().put(purpose, new ArrayList<>());
        }
    }
    private long getTimer(String str) {
        if(!str.isEmpty() && Long.parseLong(str) != 0) {
            return Long.parseLong(str);
        }
        return timerDefault;
    }

    private void setTimerDefault() {
        String str = editTextTimer.getText().toString().trim();
        if(!str.isEmpty()) {
            if(Long.parseLong(str) != 0) {
                timerDefault = Long.parseLong(str);
            }
            editTextTimer.setText("");
        } else {
            timerDefault = TIMER_DEFAULT;
        }
        editTextTimer.setHint(timerDefault + " minutes");

        try {
            ioO.saveObjectToStorage(timerDefault, FILENAME_TIMER);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void initialization() {
        buttonBegin = findViewById(R.id.buttonBegin);
        cardViewPurpose = findViewById(R.id.cardViewPurpose);
        acTextView = findViewById(R.id.autoCompleteTextView);
        editTextTimer = findViewById(R.id.editTextNumber);
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        cardViewHistory = findViewById(R.id.cardViewHistory);
        cardViewHistory.setVisibility(View.GONE);
        textViewPurpose = findViewById(R.id.textViewPurpose);
        textViewTotalTimeSpent = findViewById(R.id.textViewTotalTimeSpent);
    }

    private void getLastPurposeFromStorage() {
        try {
            lastPurpose = String.valueOf(ioO.loadObjectFromStorage(FILENAME_PURPOSE));
        } catch (IOException | ClassNotFoundException ioException) {
            lastPurpose = "";
        }
    }

    private void getTimerDefaultFromStorage() {

        try {
            timerDefault = (long) ioO.loadObjectFromStorage(FILENAME_TIMER);
        } catch (IOException | ClassNotFoundException exception) {
            exception.printStackTrace();
            timerDefault = TIMER_DEFAULT;
        }
        editTextTimer.setHint(timerDefault + " minutes");
    }

    private void setLastPurposeToStorage() {
        try {
            ioO.saveObjectToStorage(lastPurpose, FILENAME_PURPOSE);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public String getTimeLastSpent(String purpose) {
        long seconds = 0;
        if(getMap().containsKey(purpose)) {
            List<List<MyCalendar>> calendarList = getMap().get(purpose);

            if(calendarList != null) {
                if(calendarList.size() != 0){
                    int index = calendarList.size() - 1;
                    seconds = (calendarList.get(index).get(1).getTimeInMillis()
                            - calendarList.get(index).get(0).getTimeInMillis()) / 1000;

                }
            }
        }
        return timeFormatter(seconds);
    }

    private String getTimeTotalSpent(String purpose) {
        long seconds = 0;
        if(getMap().containsKey(purpose)) {
            List<List<MyCalendar>> calendarList = getMap().get(purpose);

            if(calendarList != null) {          //for compiler's sake
                for(List<MyCalendar> mCalender : calendarList) {
                    seconds += (mCalender.get(1).getTimeInMillis()
                            - mCalender.get(0).getTimeInMillis()) / 1000;
                }
            }
        }
        return timeFormatter(seconds);
    }

    private void inputAction() {
        String str = acTextView.getText().toString().trim();
        if(str.length() > MINIMUM_PURPOSE_LENGTH) {
            str = str.substring(0,1).toUpperCase() +
                    str.substring(1).toLowerCase();
            textViewPurpose.setText(str);
            textViewTotalTimeSpent.setText(getTimeTotalSpent(str));

            List<List<MyCalendar>> calendarList;
            if(getMap().containsKey(str)) {
                calendarList = getMap().get(str);

                acTextView.dismissDropDown();
                hideKeyboard();

            } else {
                calendarList = new ArrayList<>();
            }

            rvHistoryAdapter = new RvHistoryAdapter(calendarList);
            recyclerViewHistory.setAdapter(rvHistoryAdapter);
            recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));

        }
    }

    private String timeFormatter(long seconds) {
        long hours, minutes;
        StringBuilder sb = new StringBuilder();
        if(seconds >= 60) {
            minutes = seconds / 60;
            seconds = seconds % 60;
            if(minutes >= 60) {
                hours = minutes / 60;
                minutes = minutes % 60;
                sb.append(hours).append("h ");
            }
            sb.append(minutes).append("m ");
        }
        sb.append(seconds).append("s");
        return sb.toString();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop: called");

        if(!isForegroundServiceRunning()) {
            finishAndRemoveTask();
        }
    }

    @Override
    public void onBackPressed() {
        if(!isForegroundServiceRunning()) {
            finishAndRemoveTask();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("TAG", "onResume: called");
    }

    private void timeUpAction() {
        if(isTimedOut()) {
            setTimedOut(false);
            new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                    .setCancelable(false)
                    .setTitle("TIME UP !!!")
                    .setMessage("" + lastPurpose +
                            "\nused ->  " + getTimeLastSpent(lastPurpose) +
                            "\n\nDon't Waste Time  :)")
                    .setPositiveButton("ok", (dialog, which) ->
                            dialog.dismiss()).create().show();
            textViewPurpose.setText(lastPurpose);
            textViewTotalTimeSpent.setText(getTimeTotalSpent(lastPurpose));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}