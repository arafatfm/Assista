package com.afm.assista;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

//regular adapter for recyclerView used in history

public class RvHistoryAdapter extends RecyclerView.Adapter<RvHistoryAdapter.RvViewHolder> {
    private final List<List<MyCalendar>> lists;

    public RvHistoryAdapter(List<List<MyCalendar>> listList) {
        this.lists = listList;
    }

    @NonNull
    @Override
    public RvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.row_rv_history, parent, false);
        return new RvViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RvViewHolder holder, int position) {
        int size = lists.size() - 1;
        holder.textViewRV1.setText(lists.get(size-position).get(0).getTime());
        holder.textViewRV2.setText(lists.get(size-position).get(1).getTime());
    }

    @Override
    public int getItemCount() {
        return lists.size();
    }

    public static class RvViewHolder extends RecyclerView.ViewHolder {
        TextView textViewRV1;
        TextView textViewRV2;

        public RvViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewRV1 = itemView.findViewById(R.id.textViewRVH1);
            textViewRV2 = itemView.findViewById(R.id.textViewRVH2);
        }
    }
}
