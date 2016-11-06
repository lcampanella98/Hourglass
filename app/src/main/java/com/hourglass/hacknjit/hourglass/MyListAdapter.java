package com.hourglass.hacknjit.hourglass;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by lorenzo on 11/6/2016.
 */

public class MyListAdapter extends ArrayAdapter<String> {

    private Integer chosenPosition;

    public MyListAdapter(Context context, int resource, int textViewResourceId, List<String> objects) {
        super(context, resource, textViewResourceId, objects);
    }


    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null)
        {
            LayoutInflater inflater = (LayoutInflater)  getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.result_list_row, null);
        }
        String s = getItem(position);
        TextView tv = (TextView) v.findViewById(R.id.text_result_list_row_date);
        tv.setText(s);

        if (chosenPosition != null && position == chosenPosition) {
            tv.setBackgroundColor(getContext().getResources().getColor(R.color.colorAccentLight));
            tv.setTextColor(getContext().getResources().getColor(android.R.color.white));
        } else {
            tv.setBackgroundColor(getContext().getResources().getColor(android.R.color.background_light));
            tv.setTextColor(getContext().getResources().getColor(android.R.color.secondary_text_light));
        }
        return v;
    }

    public void setChosenPosition(int position) {
        this.chosenPosition = position;
    }

    public Integer getChosenPosition() {
        return chosenPosition;
    }
}