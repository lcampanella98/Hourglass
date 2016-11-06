package com.hourglass.hacknjit.hourglass;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.os.Bundle;

import java.util.Date;

/**
 * Created by lorenzo on 11/5/2016.
 */

public class DateDialogFragment extends DialogFragment {

    public interface DateDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    DateDialogListener dListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DatePickerDialog.Builder dpBuilder = new DatePickerDialog.Builder(getActivity());

        dpBuilder.setTitle(savedInstanceState.getString("dateDialogTitle"));
        dpBuilder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            dListener = (DateDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }
}
