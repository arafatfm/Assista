package com.afm.assista;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;

//this class is used to hide vertical scrolling effects in last purpose recycler view

public class UnscrollableLinearLayoutManager extends LinearLayoutManager {

    @Override
    public boolean canScrollVertically() {
        return false;
    }

    public UnscrollableLinearLayoutManager(Context context) {
        super(context);
    }

    public UnscrollableLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public UnscrollableLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
