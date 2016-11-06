package com.hourglass.hacknjit.hourglass;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.api.client.util.DateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResultListActivity extends AppCompatActivity {

    private MyListAdapter adapter;
    private Toolbar toolbar;
    private ListView listView;
    private TextView emptyListText;
    private ArrayList<DateTime> freeDatesList;
    private List<String> stringDates;
    private View selectedView;
    private int prevTextColor, prevBGColor;
    private long hoursMillis;

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
        hoursMillis = getIntent().getLongExtra(MainActivity.HOURS_MILLIS, 0L);
        stringDates = new ArrayList<>();
        for (DateTime dt : freeDatesList) {
            stringDates.add(getDateText(dt));
        }

        adapter = new MyListAdapter(getApplicationContext(), R.layout.result_list_row, R.id.text_result_list_row_date, stringDates);
        listView.setAdapter(adapter);

        listView.setEmptyView(emptyListText);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.setChosenPosition(position);
                if (selectedView != null) {
                    TextView tv = (TextView) (selectedView.findViewById(R.id.text_result_list_row_date));
                    tv.setBackgroundColor(getApplicationContext().getResources().getColor(android.R.color.background_light));
                    tv.setTextColor(getApplicationContext().getResources().getColor(android.R.color.secondary_text_light));
                }
                selectedView = view;
                TextView tv = (TextView) (selectedView.findViewById(R.id.text_result_list_row_date));
                tv.setBackgroundColor(getApplicationContext().getResources().getColor(R.color.colorAccentLight));
                tv.setTextColor(getApplicationContext().getResources().getColor(android.R.color.white));

            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer pos = adapter.getChosenPosition();
                if (pos != null) {
                    Intent i = new Intent(getApplicationContext(), SubmitActivity.class);
                    i.putExtra(MainActivity.HOURS_MILLIS, hoursMillis);
                    i.putExtra(MainActivity.START_DATE, freeDatesList.get(pos));
                    startActivity(i);
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
        System.out.println(date.toString());
        return DAYS_OF_WEEK[date.getDay()] + ", " + MONTHS[date.getMonth()] + " " + date.getDate() +  " at " + new SimpleDateFormat("h:mm a", Locale.US).format(date);
    }
}