package com.hourglass.hacknjit.hourglass;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.DatePicker;

/**
 * Created by lorenzo on 11/5/2016.
 */

public class MyDatePickerDialog extends DatePickerDialog {

    private CharSequence title;

    public MyDatePickerDialog(Context context, OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth) {
        super(context, callBack, year, monthOfYear, dayOfMonth);
    }

    public void setPermanentTitle(CharSequence title) {
        this.title = title;
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int month, int day) {
        super.onDateChanged(view, year, month, day);
        //setCustomTitle(title);
    }
}