package com.afm.assista;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "xxx" + getClass().getSimpleName();
    private Button button;

    ActivityResultLauncher<Intent> arl = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> checkOverlayPermission());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);
        checkOverlayPermission();

        Button buttonFix = findViewById(R.id.buttonFix);
        buttonFix.setOnClickListener(v -> new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                .setCancelable(false)
                .setTitle("For MiUi Only")
                .setMessage("-> Boost speed -> Lock apps" + "\n-> Find app name & lock")
                .setPositiveButton("proceed", (dialog, which) -> {
                    Intent intent = new Intent();
                    intent.setClassName("com.miui.securitycenter",
                            "com.miui.securityscan.ui.settings.SettingsActivity");
                    startActivity(intent);
                })
                .setNegativeButton("cancel", (dialog, which) -> dialog.dismiss()).create().show());

        button.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, ForegroundService.class);
            serviceIntent.putExtra("IntentExtra", getClass().getSimpleName());

            if(!ForegroundService.isRunning()) {
                ContextCompat.startForegroundService(this, serviceIntent);

                button.setText(R.string.stop);
                finishAndRemoveTask();
            } else {
                stopService(serviceIntent);
                button.setText(R.string.start);
            }
        });
    }

    private void checkOverlayPermission() {
        if(!Settings.canDrawOverlays(this)) {
            new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)         //    https://github.com/material-components/material-components-android/issues/539
                    .setCancelable(false)
                    .setTitle("Permission Required")
                    .setMessage("Assista needs your permission to\nDisplay Over Other Apps")
                    .setPositiveButton("ok", (dialog, which) -> arl.launch(new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))))
                    .setNegativeButton("exit", (dialog, which) -> finishAndRemoveTask()).create().show();

        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            if(!ForegroundService.isRunning()) {button.setText(R.string.start);}
            else {button.setText(R.string.stop);}
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if(ForegroundService.isRunning()) {
            finishAndRemoveTask();
        } else {
            super.onBackPressed();
        }
    }

}