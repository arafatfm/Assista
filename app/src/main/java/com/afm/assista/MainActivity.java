package com.afm.assista;

import static com.afm.assista.App.CLASS_NAME;
import static com.afm.assista.App.FILENAME_APP_STATE;
import static com.afm.assista.App.getMap;
import static com.afm.assista.App.ioO;
import static com.afm.assista.App.isAppActive;
import static com.afm.assista.App.isForegroundServiceRunning;
import static com.afm.assista.App.setAppActive;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "xxx" + getClass().getSimpleName();
    private final int PHONE_STATE_PERMISSION_CODE = 1;
    private Button button;

    private final ActivityResultLauncher<Intent> arl = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(button != null)
                button.setText(R.string.start);
        }
    };

    private AlertDialog accessibilityDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter("ForegroundStopped");
        registerReceiver(receiver, filter);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)) {
                showPhoneStatePermissionRationale();
            } else {
                ActivityCompat.requestPermissions(this, new String[]
                        {Manifest.permission.READ_PHONE_STATE}, PHONE_STATE_PERMISSION_CODE);
            }
            return;
        }

        if(!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
            return;
        }

        initializeAccessibilityDialog();

        if (!isAccessibilityServiceEnabled(getApplicationContext())) {
            accessibilityDialog.show();
            return;
        }

        if(!MyAccessibilityService.isActive()) {
            accessibilityDialog.setTitle("Oops !!");
            accessibilityDialog.setMessage("Accessibility Service is not working." +
                    " You need to stop it and start again");
            accessibilityDialog.show();
            return;
        }


        eraseHistory();

        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);

        if(isForegroundServiceRunning()) button.setText(R.string.stop);
        else button.setText(R.string.start);

        button.setOnClickListener(v -> buttonClickAction());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(isForegroundServiceRunning()) finish();
    }

    private void initializeAccessibilityDialog() {
        accessibilityDialog = new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                .setCancelable(false)
                .setTitle("Permission Required")
                .setMessage("Assista needs Accessibility permission to blacklist apps")
                .setPositiveButton("ok", (dialog, which) -> {
                    arl.launch(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));

                    Intent intent = new Intent(this, DialogActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra("dialogExtra", "-> Downloaded apps\n-> Assista                   ");
                    (new Handler()).postDelayed(() -> startActivity(intent), 500);
                })
                .setNegativeButton("exit", (dialog, which) -> finish()).create();
    }

    private void buttonClickAction() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        serviceIntent.putExtra(CLASS_NAME, getClass().getSimpleName());

        if(!isForegroundServiceRunning()) {
            ContextCompat.startForegroundService(this, serviceIntent);
            setAppActive(true);
            button.setText(R.string.stop);

        } else {
            stopService(serviceIntent);
            setAppActive(false);
            button.setText(R.string.start);
        }

        try {
            ioO.saveObjectToStorage(isAppActive(), FILENAME_APP_STATE);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private void itemFixClickAction() {
        new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                .setCancelable(false)
                .setTitle("For MiUi Only")
                .setMessage("-> Boost speed -> Lock apps" + "\n-> Find app name & lock")
                .setPositiveButton("proceed", (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setClassName("com.miui.securitycenter",
                            "com.miui.securityscan.ui.settings.SettingsActivity");
                    startActivity(intent);
                })
                .setNegativeButton("cancel", (dialog, which) ->
                        dialog.dismiss()).create().show();
    }

    private void showPhoneStatePermissionRationale() {
        new MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setTitle("Permission Required")
                .setMessage("Assista needs Phone permission to detect incoming call. " +
                        "Otherwise Assista can't function properly.")
                .setPositiveButton("ok", ((dialog, which) ->
                        ActivityCompat.requestPermissions(this, new String[]
                                        {Manifest.permission.READ_PHONE_STATE},
                                PHONE_STATE_PERMISSION_CODE)))
                .setNegativeButton("exit", ((dialog, which) -> finish()))
                .create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PHONE_STATE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_PHONE_STATE)) {
                    showPhoneStatePermissionRationale();
                } else {
                    new MaterialAlertDialogBuilder(this)
                            .setCancelable(false)
                            .setTitle("Phone Permission Missing!!")
                            .setMessage("Assista needs to detect incoming call. " +
                                    "You denied this permission multiple times. " +
                                    "Now you can accept it from settings page")
                            .setPositiveButton("ok", ((dialog, which) -> {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                arl.launch(intent);
                            }))
                            .setNegativeButton("exit", ((dialog, which) -> finish()))
                            .create().show();

                }
            }
        }
    }

    private void requestOverlayPermission() {
        new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)         //    https://github.com/material-components/material-components-android/issues/539
                .setCancelable(false)
                .setTitle("Permission Required")
                .setMessage("Assista needs your permission to\nDisplay Over Other Apps")
                .setPositiveButton("ok", (dialog, which) -> arl.launch(new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()))))
                .setNegativeButton("exit", (dialog, which) ->
                        finish()).create().show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.itemFix) {
            itemFixClickAction();
        } else if(item.getItemId() == R.id.itemBlacklist) {
            Intent intent = new Intent(this, BlacklistActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean isAccessibilityServiceEnabled(Context context)
    {
        String prefString = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return prefString!= null && prefString.contains(context.getPackageName() + "/" +
                MyAccessibilityService.class.getCanonicalName());
    }

    private void eraseHistory() {       //daily history
        Calendar today = Calendar.getInstance();
        for(List<List<MyCalendar>> calendarList : getMap().values()) {
            for(List<MyCalendar> calendar : calendarList) {
                if(calendar.size() < 2) calendar.add(calendar.get(0));      //crashFix due to force close
            }
            calendarList.removeIf(calendar ->
                    calendar.get(1).get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR));
        }
    }
}