package com.afm.assista;

import static com.afm.assista.App.getBlacklist;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RvBlacklistAdapter extends RecyclerView.Adapter<RvBlacklistAdapter.RvViewHolder> {
    private final String TAG = "xxx" + getClass().getSimpleName();
    private final List<ResolveInfo> appsList;
    private final PackageManager pm;
    private final OnSwitchStateChangeListener onSwitchStateChangeListener;

    public RvBlacklistAdapter(List<ResolveInfo> pkgAppsList, PackageManager pm, OnSwitchStateChangeListener listener) {
        this.appsList = pkgAppsList;
        this.pm = pm;
        this.onSwitchStateChangeListener = listener;
    }

    @NonNull
    @Override
    public RvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.row_rv_blacklist, parent, false);
        return new RvViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RvViewHolder holder, int position) {
        holder.imageView.setImageDrawable(appsList.get(position).loadIcon(pm));
        holder.textView.setText(appsList.get(position).loadLabel(pm));

        holder.switchCompat.setOnClickListener(v -> {
            if(holder.switchCompat.isChecked()) {
                getBlacklist().put(appsList.get(position).activityInfo.applicationInfo.packageName, 0L);
            } else {
                getBlacklist().remove(appsList.get(position).activityInfo.applicationInfo.packageName);
            }
            onSwitchStateChangeListener.onChange(position);
        });

        holder.switchCompat.setChecked(getBlacklist().containsKey(
                appsList.get(position).activityInfo.applicationInfo.packageName));

    }

    @Override
    public int getItemCount() {
        return appsList.size();
    }

    public static class RvViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;
        SwitchCompat switchCompat;

        public RvViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewRVW);
            textView = itemView.findViewById(R.id.textViewRVW);
            switchCompat = itemView.findViewById(R.id.switchRVW);

        }
    }
    public interface OnSwitchStateChangeListener {
        void onChange(int position);
    }
}
