package com.afm.assista;

import static com.afm.assista.App.FILENAME_BLACKLIST;
import static com.afm.assista.App.getBlacklist;
import static com.afm.assista.App.ioO;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.List;

public class BlacklistActivity extends AppCompatActivity implements RvBlacklistAdapter.OnSwitchStateChangeListener {
    private final String TAG = "xxx" + getClass().getSimpleName();
    private final int MAX_VELOCITY_Y = 11000;
    private RecyclerView recyclerView;
    private RvBlacklistAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_black_list);
        recyclerView = findViewById(R.id.recyclerViewWhiteList);

        Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> pkgAppsList = pm.queryIntentActivities( mainIntent, 0);

        new Thread(() -> {
            pkgAppsList.sort(new ResolveInfo.DisplayNameComparator(pm));
            for(ResolveInfo app : pkgAppsList) {
                if(app.activityInfo.applicationInfo.packageName.equals(getPackageName())) {
                    pkgAppsList.remove(app);
                    break;
                }
            }
            runOnUiThread(() -> {
                adapter = new RvBlacklistAdapter(pkgAppsList, pm, this);
                recyclerView.setAdapter(adapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            });
        }).start();

        recyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                if(Math.abs(velocityY) > MAX_VELOCITY_Y) {
                    velocityY = MAX_VELOCITY_Y * (int) Math.signum((double)velocityY);
                    recyclerView.fling(velocityX, velocityY);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onChange(int position) {
        Log.i(TAG, "onChange: Hello");
//        adapter.notifyItemChanged(position);
        try {
            ioO.saveObjectToStorage(getBlacklist(), FILENAME_BLACKLIST);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}