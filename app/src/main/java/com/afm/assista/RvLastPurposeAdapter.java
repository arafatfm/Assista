package com.afm.assista;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

//https://www.youtube.com/watch?v=69C1ljfDvl0

//special recyclerView adapter for onClick Action on last purpose

public class RvLastPurposeAdapter extends RecyclerView.Adapter<RvLastPurposeAdapter.RvViewHolder> {
    private final List<List<String>> list;
    private final OnLpClickListener mOnLpClickListener;

    public RvLastPurposeAdapter(List<List<String>> stringList, OnLpClickListener onLpClickListener) {
        list = stringList;
        this.mOnLpClickListener = onLpClickListener;
    }

    @NonNull
    @Override
    public RvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.row_rv_purpose, parent, false);
        return new RvViewHolder(view, mOnLpClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RvViewHolder holder, int position) {
        if(list.size() > 0) {
            holder.textViewRV1.setText(list.get(0).get(0));
            holder.textViewRV2.setText(list.get(0).get(1));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class RvViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textViewRV1;
        TextView textViewRV2;
        OnLpClickListener onLpClickListener;

        public RvViewHolder(@NonNull View itemView, OnLpClickListener onLpClickListener) {
            super(itemView);
            textViewRV1 = itemView.findViewById(R.id.textViewRVP1);
            textViewRV2 = itemView.findViewById(R.id.textViewRVP2);
            this.onLpClickListener = onLpClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onLpClickListener.onLpClick(getAdapterPosition());
        }
    }
    public interface OnLpClickListener {
        void onLpClick(int position);
    }
}
