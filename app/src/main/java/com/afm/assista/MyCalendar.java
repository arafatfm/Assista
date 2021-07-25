package com.afm.assista;

import android.annotation.SuppressLint;

import java.io.Serializable;
import java.util.Calendar;

public class MyCalendar implements Serializable {
    private final long serialVersionUID = 1L;
    private final Calendar calendar;

    public MyCalendar(Calendar calendar) {
        this.calendar = calendar;
    }

    @SuppressLint("DefaultLocale")
    public String getTime() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("%02d", calendar.get(Calendar.HOUR))).append(":")
                .append(String.format("%02d", calendar.get(Calendar.MINUTE))).append(":")
                .append(String.format("%02d", calendar.get(Calendar.SECOND))).append(" ");

        if (calendar.get(Calendar.AM_PM) == Calendar.AM) stringBuilder.append("AM");
        else stringBuilder.append("PM");

        return stringBuilder.toString();
    }

    public int get(int field) {
        return calendar.get(field);
    }

    public long getTimeInMillis() {
        return calendar.getTimeInMillis();
    }

//    public Date getFullTime() {
//        return calendar.getTime();
//    }

}
