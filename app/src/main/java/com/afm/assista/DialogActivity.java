package com.afm.assista;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DialogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getAttributes().gravity = Gravity.TOP;

        setContentView(R.layout.activity_dialog);
        TextView textView = findViewById(R.id.textViewDialogActivity);
        textView.setText(getIntent().getStringExtra("dialogExtra"));
    }
}