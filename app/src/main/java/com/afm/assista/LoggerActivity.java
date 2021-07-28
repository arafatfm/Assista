package com.afm.assista;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.afm.assista.App.ACTION_STOP;
import static com.afm.assista.App.CLASS_NAME;
import static com.afm.assista.App.FILENAME_MAP;
import static com.afm.assista.App.FILENAME_PURPOSE;
import static com.afm.assista.App.FILENAME_TIMER;
import static com.afm.assista.App.FILE_DIRECTORY;
import static com.afm.assista.App.TIMER_DEFAULT;
import static com.afm.assista.App.getMap;
import static com.afm.assista.App.ioO;
import static com.afm.assista.App.isForegroundServiceRunning;
import static com.afm.assista.App.isTimedOut;
import static com.afm.assista.App.purpose;
import static com.afm.assista.App.setMap;
import static com.afm.assista.App.setTimedOut;

public class LoggerActivity extends AppCompatActivity
        implements RvLastPurposeAdapter.OnLpClickListener {
    private final String TAG = "xxx" + getClass().getSimpleName();

    private static long timerDefault;
    private RecyclerView recyclerViewHistory, recyclerViewLPurpose;
    private TextView textViewPurpose, textViewTotalTimeSpent;
    private CardView cardViewHistory, cardViewPurpose;
    private EditText editTextTimer;
    private AutoCompleteTextView acTextView;
    private static String lastPurpose;
    RvHistoryAdapter rvHistoryAdapter;
    RvLastPurposeAdapter rvLastPurposeAdapter;
    private String timeLastSpent;
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
        getMapFromStorage();
        historyCleaner();
        getTimerDefaultFromStorage();
        getLastPurposeFromStorage();
        lastPurposeRVAction();
        timeUpAction();

        ArrayAdapter<String> adapterACTV = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>(getMap().keySet()));
        acTextView.setAdapter(adapterACTV);

        cardViewPurpose.setOnClickListener(v -> {
            inputAction();
            if(cardViewHistory.getVisibility() == View.VISIBLE) cardViewHistory.setVisibility(View.GONE);
            else cardViewHistory.setVisibility(View.VISIBLE);
        });

        buttonBegin.setOnLongClickListener(v -> buttonBeginLongClickAction());

        buttonBegin.setOnClickListener(v -> buttonBeginClickAction());

        acTextView.setOnKeyListener((v, keyCode, event) -> {
            if(keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                inputAction();
            }
            return false;
        });

        acTextView.setOnItemClickListener((parent, view, position, id) -> inputAction());

    }

    private void buttonBeginClickAction() {
        String tempString = acTextView.getText().toString().trim();
        if(tempString.length() > 1) {
            ForegroundService.stopExecutor();

            tempString = tempString.substring(0,1).toUpperCase() +
                    tempString.substring(1).toLowerCase();
            lastPurpose = purpose = tempString;
            acTextView.setText("");

            if(!getMap().containsKey(purpose)) {
                getMap().put(purpose, new ArrayList<>());
            }

            tempString = editTextTimer.getText().toString().trim();
            long timer;
            if(!tempString.equals("")) {
                if(Long.parseLong(tempString) != 0) {
                    timer = Long.parseLong(tempString);
                } else {
                    timer = timerDefault;
                }
                editTextTimer.setText("");
            } else timer = timerDefault;
            Intent serviceIntent = new Intent(this, ForegroundService.class);
            serviceIntent.putExtra(CLASS_NAME, getClass().getSimpleName());
            serviceIntent.putExtra("TimerExtra", timer);
            ContextCompat.startForegroundService(this, serviceIntent);

            setLastPurposeToStorage();

            finishAndRemoveTask();
        }
    }

    private boolean buttonBeginLongClickAction() {
        String tempString = editTextTimer.getText().toString();
        if(!tempString.equals("")) {
            if(Long.parseLong(tempString) != 0) {
                timerDefault = Long.parseLong(tempString);
            }
            editTextTimer.setText("");
        } else {
            timerDefault = TIMER_DEFAULT;
        }
        editTextTimer.setHint(String.valueOf(timerDefault));

        try {
            ioO.saveObjectToStorage(timerDefault, FILENAME_TIMER);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return true;
    }

    private void initialization() {
        buttonBegin = findViewById(R.id.buttonBegin);
        cardViewPurpose = findViewById(R.id.cardViewPurpose);
        acTextView = findViewById(R.id.autoCompleteTextView);
        editTextTimer = findViewById(R.id.editTextNumber);
        editTextTimer.setHint(String.valueOf(timerDefault));
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerViewLPurpose = findViewById(R.id.recyclerViewLastPurpose);
        cardViewHistory = findViewById(R.id.cardViewHistory);
        cardViewHistory.setVisibility(View.GONE);
        textViewPurpose = findViewById(R.id.textViewPurpose);
        textViewTotalTimeSpent = findViewById(R.id.textViewTotalTimeSpent);
    }

    private void lastPurposeRVAction() {
        List<List<String>> tempList = new ArrayList<>();

        ItemTouchHelper.SimpleCallback itemTouchCallback = new ItemTouchHelper
                .SimpleCallback(0, ItemTouchHelper.RIGHT) {

            @Override
            public void onSwiped(@NotNull RecyclerView.ViewHolder viewHolder, int direction) {
                Log.i(TAG, "onSwiped: worked");
                int position = viewHolder.getAdapterPosition();
                tempList.remove(position);
                rvLastPurposeAdapter.notifyItemRemoved(position);
                Log.i(TAG, "filePurposeDeleted?: " + deleteLastPurpose());

            }
            @Override
            public boolean onMove(@NotNull RecyclerView recyclerView,
                                  @NotNull RecyclerView.ViewHolder viewHolder,
                                  @NotNull RecyclerView.ViewHolder target) {
                return false;
            }
        };

        if(!lastPurpose.equals("")) {
            List<List<MyCalendar>> thisList = getMap().get(lastPurpose);

            if(thisList != null) {
                if(thisList.size() != 0){
                    int index = thisList.size() - 1;
                    long seconds = (thisList.get(index).get(1).getTimeInMillis()
                            - thisList.get(index).get(0).getTimeInMillis()) / 1000;
                    timeLastSpent = timeFormatter(seconds);

                    tempList.add(new ArrayList<>());
                    tempList.get(0).add(lastPurpose);
                    tempList.get(0).add(timeLastSpent);

                    rvLastPurposeAdapter = new RvLastPurposeAdapter(
                            tempList, this);
                    new ItemTouchHelper(itemTouchCallback)
                            .attachToRecyclerView(recyclerViewLPurpose);
                    recyclerViewLPurpose.setAdapter(rvLastPurposeAdapter);
                    recyclerViewLPurpose.setLayoutManager(
                            new UnscrollableLinearLayoutManager(this));
                }
            }
        } else {
            recyclerViewLPurpose.setVisibility(View.GONE);
        }
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
        editTextTimer.setHint(String.valueOf(timerDefault));
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

    private void setLastPurposeToStorage() {
        try {
            ioO.saveObjectToStorage(lastPurpose, FILENAME_PURPOSE);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void inputAction() {
        String tempString = acTextView.getText().toString().trim();
        if(tempString.length() > 1) {
            tempString = tempString.substring(0,1).toUpperCase() +
                    tempString.substring(1).toLowerCase();
            textViewPurpose.setText(tempString);

            List<List<MyCalendar>> thisList;
            if(getMap().containsKey(tempString)) {
                thisList = getMap().get(tempString);

                if(thisList != null) {          //for compiler's sake
                    long seconds = 0;
                    for(List<MyCalendar> mCalender : thisList) {
                        seconds += (mCalender.get(1).getTimeInMillis()
                                - mCalender.get(0).getTimeInMillis()) / 1000;
                    }
                    String timeTotalSpent = timeFormatter(seconds);
                    textViewTotalTimeSpent.setText(timeTotalSpent);
                    acTextView.dismissDropDown();
                    hideKeyboard();
                }
            } else {
                thisList = new ArrayList<>();
            }

            rvHistoryAdapter = new RvHistoryAdapter(thisList);
            recyclerViewHistory.setAdapter(rvHistoryAdapter);
            recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));

        }
    }

    private void historyCleaner() {
        Calendar today = Calendar.getInstance();
        for(List<List<MyCalendar>> thisList : getMap().values()) {
            for(int index=0 ; index<thisList.size() ; index++) {
                List<MyCalendar> thatList = thisList.get(index);
                if(thatList.size() < 2) {
                    thatList.add(thatList.get(0));      //bugfix
                }
                if(today.get(Calendar.DAY_OF_YEAR) != thatList.get(1).get(Calendar.DAY_OF_YEAR)) {
                    thisList.remove(thatList);
                    --index;
                }
            }
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
    protected void onPause() {
        super.onPause();
        Log.i("TAG", "onPause: called");

        if(isForegroundServiceRunning()) {
            if(purpose.equals("")) {
                ActivityManager activityManager = (ActivityManager) getApplicationContext()
                        .getSystemService(Context.ACTIVITY_SERVICE);

                activityManager.moveTaskToFront(getTaskId(), 0);
            }
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

    @Override
    public void onLpClick(int position) {
        acTextView.setText(lastPurpose);
        recyclerViewLPurpose.setVisibility(View.GONE);
        deleteLastPurpose();
        inputAction();
    }

    public static boolean deleteLastPurpose() {
        lastPurpose = "";
        File filePurpose = new File(FILE_DIRECTORY, FILENAME_PURPOSE);
        return filePurpose.delete();
    }

    private void timeUpAction() {
        if(isTimedOut()) {
            setTimedOut(false);
            new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                    .setCancelable(false)
                    .setTitle("TIME UP !!!")
                    .setMessage("" + lastPurpose +
                            "\nused ->  " + timeLastSpent +
                            "\n\nDon't Waste Time  :)")
                    .setPositiveButton("ok", (dialog, which) ->
                            dialog.dismiss()).create().show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}