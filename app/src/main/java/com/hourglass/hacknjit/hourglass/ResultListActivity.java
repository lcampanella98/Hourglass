package com.hourglass.hacknjit.hourglass;

import android.app.ListActivity;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.util.DateTime;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ResultListActivity extends AppCompatActivity {

    private ArrayAdapter<String> adapter;
    private Toolbar toolbar;
    private ListView listView;
    private TextView emptyListText;
    private ArrayList<DateTime> freeDatesList;
    private List<String> stringDates;
    private View selectedView;
    private int prevTextColor, prevBGColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_list);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = (ListView) findViewById(R.id.list_view);

        emptyListText = (TextView) findViewById(R.id.text_view_empty);
        if (emptyListText != null) {
            emptyListText.setVisibility(View.INVISIBLE);
        }

        freeDatesList = (ArrayList<DateTime>) getIntent().getSerializableExtra(MainActivity.FREE_DATES);
        stringDates = new ArrayList<>();
        for (DateTime dt : freeDatesList) {
            stringDates.add(getDateText(dt));
        }

        adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.result_list_row, R.id.text_result_list_row_date, stringDates);
        listView.setAdapter(adapter);

        listView.setEmptyView(emptyListText);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (selectedView != null) {
                    TextView tv = (TextView) selectedView.findViewById(R.id.text_result_list_row_date);
                    tv.setTextColor(prevTextColor);
                    tv.setBackgroundColor(prevBGColor);
                }
                selectedView = view;
                TextView tv = (TextView) selectedView.findViewById(R.id.text_result_list_row_date);
                prevBGColor = tv.getSolidColor();
                prevTextColor = tv.getCurrentTextColor();
                tv.setTextColor(getResources().getColor(android.R.color.white));
                tv.setBackgroundColor(getResources().getColor(R.color.colorAccentLight));

                String selected = adapter.getItem(position);
                if (selected != null) {
                    Toast.makeText(getApplicationContext(), "Selected " + selected, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private static final String[] DAYS_OF_WEEK = new String[] {
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    private static final String[] MONTHS = new String[] {
            "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
    };

    private String getDateText(DateTime dt) {
        Date date = new Date(dt.getValue());
        return DAYS_OF_WEEK[date.getDay()] + ", " + MONTHS[date.getMonth()] + " " + date.getDate() +  " at " + new SimpleDateFormat("h:mm a", Locale.US).format(date);
    }
}
