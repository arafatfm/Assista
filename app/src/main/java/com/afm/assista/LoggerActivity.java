package com.afm.assista;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoggerActivity extends AppCompatActivity implements RvLastPurposeAdapter.OnLpClickListener {
    private final String TAG = "xxx" + getClass().getSimpleName();

    public static final String FILE_MAP = "database.dat";
    public static final String FILE_TIMER = "timer.dat";
    public static final String FILE_PURPOSE = "purpose.txt";

    public static Map<String, List<List<MyCalendar>>> map;
    public static final long TIMER_DEFAULT = 2;
    private static long timerDefault;
    public static String purpose = "";
    private RecyclerView recyclerViewHistory, recyclerViewLPurpose;
    private TextView textViewPurpose, textViewTotalTimeSpent;
    private CardView cardViewHistory;
    private EditText editTextTimer;
    private AutoCompleteTextView acTextView;
    private long timer;
    private static String lastPurpose;
    RvHistoryAdapter rvHistoryAdapter;
    RvLastPurposeAdapter rvLastPurposeAdapter;
    private String timeLastSpent;

    private void timeUp() {
        if(ForegroundService.timedOut) {
            ForegroundService.timedOut = false;
            new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                    .setCancelable(false)
                    .setTitle("TIME UP !!!")
                    .setMessage("" + lastPurpose +
                            "\nused ->  " + timeLastSpent +
                            "\n\nDon't Waste Time  :)")
                    .setPositiveButton("ok", (dialog, which) -> dialog.dismiss()).create().show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger);
        Log.i(TAG, "onCreate: called");

        Button buttonBegin = findViewById(R.id.buttonBegin);
        CardView cardViewPurpose = findViewById(R.id.cardViewPurpose);
        acTextView = findViewById(R.id.autoCompleteTextView);
        editTextTimer = findViewById(R.id.editTextNumber);
        editTextTimer.setHint(String.valueOf(timerDefault));
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        recyclerViewLPurpose = findViewById(R.id.recyclerViewLastPurpose);
        cardViewHistory = findViewById(R.id.cardViewHistory);
        cardViewHistory.setVisibility(View.GONE);
        textViewPurpose = findViewById(R.id.textViewPurpose);
        textViewTotalTimeSpent = findViewById(R.id.textViewTotalTimeSpent);

        getMapFromStorage();
        historyCleaner();
        getTimerDefaultFromStorage();
        getLastPurposeFromStorage();
        lastPurposeRecyclerView();
        timeUp();

        ArrayAdapter<String> adapterACTV = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                new ArrayList<>(map.keySet()));
        acTextView.setAdapter(adapterACTV);

        cardViewPurpose.setOnClickListener(v -> {
            inputAction();
            if(cardViewHistory.getVisibility() == View.VISIBLE) cardViewHistory.setVisibility(View.GONE);
            else cardViewHistory.setVisibility(View.VISIBLE);
        });

        buttonBegin.setOnLongClickListener(v -> {
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

            File fileTimer = new File(getExternalFilesDir(null), FILE_TIMER);
            try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileTimer))) {
                oos.writeObject(timerDefault);
            } catch (FileNotFoundException fnfException) {
                Log.d(TAG, "writeFileTimer: " + fnfException);
            } catch (IOException ioException) {
                Log.d(TAG, "writeFileTimer:  " + ioException);
            }
            return true;
        });

        buttonBegin.setOnClickListener(v -> {
            String tempString = acTextView.getText().toString().trim();
            if(tempString.length() > 1) {
                ForegroundService.stopExecutor();

                tempString = tempString.substring(0,1).toUpperCase() + tempString.substring(1).toLowerCase();
                lastPurpose = purpose = tempString;
                acTextView.setText("");

                if(!map.containsKey(purpose)) {
                    map.put(purpose, new ArrayList<>());
                }

                tempString = editTextTimer.getText().toString().trim();
                if(!tempString.equals("")) {
                    timer = Long.parseLong(tempString);
                    if(Long.parseLong(tempString) != 0) {
                        timer = Long.parseLong(tempString);
                    } else {
                        timer = timerDefault;
                    }
                    editTextTimer.setText("");
                } else timer = timerDefault;
                Intent serviceIntent = new Intent(this, ForegroundService.class);
                serviceIntent.putExtra("IntentExtra", getClass().getSimpleName());
                serviceIntent.putExtra("TimerExtra", timer);
                ContextCompat.startForegroundService(this, serviceIntent);

                setLastPurposeToStorage();

                finishAndRemoveTask();
            }
        });

        acTextView.setOnKeyListener((v, keyCode, event) -> {
            if(keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                inputAction();
            }
            return false;
        });

        acTextView.setOnItemClickListener((parent, view, position, id) -> inputAction());

    }

    private void lastPurposeRecyclerView() {
        List<List<String>> tempList = new ArrayList<>();

        ItemTouchHelper.SimpleCallback itemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull @NotNull RecyclerView recyclerView, @NonNull @NotNull RecyclerView.ViewHolder viewHolder, @NonNull @NotNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull @NotNull RecyclerView.ViewHolder viewHolder, int direction) {
                Log.i(TAG, "onSwiped: worked");
                int position = viewHolder.getAdapterPosition();
                tempList.remove(position);
                rvLastPurposeAdapter.notifyItemRemoved(position);
                Log.i(TAG, "filePurposeDeleted?: " + deleteLastPurpose());

            }
        };

        if(!lastPurpose.equals("")) {
            List<List<MyCalendar>> thisList = map.get(lastPurpose);

            if(thisList != null) {
                if(thisList.size() != 0){
                    int index = thisList.size() - 1;
                    long seconds = (thisList.get(index).get(1).getTimeInMillis()
                            - thisList.get(index).get(0).getTimeInMillis()) / 1000;
                    timeLastSpent = timeFormatter(seconds);

                    tempList.add(new ArrayList<>());
                    tempList.get(0).add(lastPurpose);
                    tempList.get(0).add(timeLastSpent);

                    rvLastPurposeAdapter = new RvLastPurposeAdapter(tempList, this);
                    new ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recyclerViewLPurpose);
                    recyclerViewLPurpose.setAdapter(rvLastPurposeAdapter);
                    recyclerViewLPurpose.setLayoutManager(new UnscrollableLinearLayoutManager(this));
                }
            }
        } else {
            recyclerViewLPurpose.setVisibility(View.GONE);
        }
    }

    private void getLastPurposeFromStorage() {
        File filePurpose = new File(getExternalFilesDir(null), FILE_PURPOSE);
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePurpose)))) {
            lastPurpose = reader.readLine();
            Log.i(TAG, "getLastPurposeFromStorage: " + lastPurpose);
        } catch (FileNotFoundException fnfException) {
            lastPurpose = "";
            Log.d(TAG, "readPurpose:   " + fnfException);
        } catch (IOException ioException) {
            Log.d(TAG, "readPurpose:  " + ioException);
        }
    }

    private void getTimerDefaultFromStorage() {
        File fileTimer = new File(getExternalFilesDir(null), FILE_TIMER);
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileTimer))) {
            timerDefault = (long) ois.readObject();
        } catch (FileNotFoundException fnfException) {
            timerDefault = TIMER_DEFAULT;
            Log.d(TAG, "readTimer: " + fnfException);
        } catch (IOException ioException) {
            Log.d(TAG, "readTimer: " + ioException);
        } catch (ClassNotFoundException cnfException) {
            Log.d(TAG, "readTimer:  " + cnfException);
        }
        editTextTimer.setHint(String.valueOf(timerDefault));
    }

    @SuppressWarnings("unchecked")
    private void getMapFromStorage() {
        File fileMap = new File(getExternalFilesDir(null), FILE_MAP);
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileMap))) {
            map = (Map<String, List<List<MyCalendar>>>) ois.readObject();
        } catch (FileNotFoundException fnfException) {
            map = new HashMap<>();
            Log.d(TAG, "readMap: " + fnfException);
        } catch (IOException ioException) {
            Log.d(TAG, "readMap: " + ioException);
        } catch (ClassNotFoundException cnfException) {
            Log.d(TAG, "readMap:  " + cnfException);
        }
    }

    private void setLastPurposeToStorage() {
        File filePurpose = new File(getExternalFilesDir(null), FILE_PURPOSE);
        try(FileOutputStream fos = new FileOutputStream(filePurpose)) {
            fos.write(lastPurpose.getBytes());
        } catch (FileNotFoundException fnfException) {
            Log.d(TAG, "writePurpose: " + fnfException);
        } catch (IOException ioException) {
            Log.d(TAG, "writePurpose:  " + ioException);
        }
    }

    private void inputAction() {
        String tempString = acTextView.getText().toString().trim();
        if(tempString.length() > 1) {
            tempString = tempString.substring(0,1).toUpperCase() + tempString.substring(1).toLowerCase();
            textViewPurpose.setText(tempString);

            List<List<MyCalendar>> thisList;
            if(map.containsKey(tempString)) {
                thisList = map.get(tempString);

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
        for(List<List<MyCalendar>> thisList : map.values()) {
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

        if(ForegroundService.isRunning()) {
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

        if(!ForegroundService.isRunning()) {
            finishAndRemoveTask();
        }
    }

    @Override
    public void onBackPressed() {
        if(!ForegroundService.isRunning()) {
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

    private boolean deleteLastPurpose() {
        lastPurpose = "";
        File filePurpose = new File(getExternalFilesDir(null), FILE_PURPOSE);
        return filePurpose.delete();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            if(!ForegroundService.isRunning()) finishAndRemoveTask();
        }
    }
}