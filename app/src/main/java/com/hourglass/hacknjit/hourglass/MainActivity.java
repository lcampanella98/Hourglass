package com.hourglass.hacknjit.hourglass;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TimeZone;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity
        implements EasyPermissions.PermissionCallbacks, DatePickerDialog.OnDateSetListener, onTaskCompleted {

    public static final String FREE_DATES = "com.hourglass.hacknjit.hourglass.FREE_DATES";

    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    private FloatingActionButton fab;
    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Call Google Calendar API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {CalendarScopes.CALENDAR_READONLY};

    private String currentDialog;
    private int day, month, year;
    private static Date startDate, endDate;
    private int hoursChunk;
    private java.util.Calendar calendar;

    private static ArrayList<DateTime> starts;
    private static ArrayList<DateTime> ends;

    /**
     * Create the main activity.
     *
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        fab = (FloatingActionButton) findViewById(R.id.fab);
        calendar = new GregorianCalendar();
        dialogsQueue = new LinkedList<>();

        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // call the dialogs in sequence

                // first dialog: number of hours

                // second dialog: start date

                day = calendar.get(GregorianCalendar.DAY_OF_MONTH);
                month = calendar.get(GregorianCalendar.MONTH);
                year = calendar.get(GregorianCalendar.YEAR);
                System.out.println("year " + year);
                dialogsQueue.clear();
                dialogsQueue.addAll(Arrays.asList("time", "startDate", "endDate"));
                currentDialog = dialogsQueue.peek();
                System.out.println("Adding first dialog...");
                addNextDialog();

                return true;
            }
        });

        mOutputText = (TextView) findViewById(R.id.text_calendar_output);
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Calendar API ...");


        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());


    }

    Date cDate;
    Queue<String> dialogsQueue;

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        System.out.println("Date Set!");
        this.year = year;

        this.month = monthOfYear;
        this.day = dayOfMonth;
        System.out.println("Year about to be set to " + year);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");


        try {
            cDate = sdf.parse(dayOfMonth + "/" + (monthOfYear+1) + "/" + year);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println(cDate.toString());
        if (currentDialog.equals("startDate")) {
            System.out.println("startDate Set!");
            startDate = cDate;

        } else if (currentDialog.equals("endDate")) {
            endDate = cDate;
            System.out.println("endDate Set!");
        }
        if (!dialogsQueue.isEmpty()) {
            System.out.println("Setting next dialog");
            addNextDialog();
        } else {
            // start new activity showing results
            // use startDate and endDate
            starts = new ArrayList<>();
            ends = new ArrayList<>();

            Toast.makeText(getApplicationContext(), startDate.toString() + "\n" + endDate.toString() + "\n" + hoursChunk, Toast.LENGTH_LONG).show();
            getResultsFromApi();

        }
    }

    @Override
    public void onTaskCompleted() {
        ArrayList<Event> x = findFreeTime(hoursChunk);
        List<DateTime> freeDates = new ArrayList<>();
        for (Event e: x) {
            freeDates.add(e.getStart().getDateTime());
        }

        //Toast.makeText(getApplicationContext(), x.toString(), Toast.LENGTH_LONG).show();
        Intent i = new Intent(getApplicationContext(), ResultListActivity.class);
        i.putExtra(MainActivity.FREE_DATES, (Serializable) freeDates);
        startActivity(i);

    }

    public ArrayList<Event> findFreeTime(long millis) {

        ArrayList<Event> rtn = new ArrayList<Event>();
        long difference = Integer.MAX_VALUE;
        for (int i = 0; i <= starts.size() - 2; i++) {
            difference = starts.get(i + 1).getValue() - ends.get(i).getValue();
            if(difference > millis + 1200000) {
                long temp = millis;
                int counter = 0;
                while(difference > temp + 1200000) {
                    Event event = new Event();
                    DateTime newDate = new DateTime(ends.get(i).getValue() + 300000 + 1800000*counter);

                    Date d = new Date(newDate.getValue());
                    if(d.getHours() <= 5 && d.getHours() >=2 ){
                        difference -= 1800000;
                        counter+=1;
                        continue;

                    }

                    EventDateTime start = new EventDateTime().setDateTime(newDate);
                    newDate = new DateTime(ends.get(i).getValue() + millis + 300000 + 1800000*counter);
                    EventDateTime end = new EventDateTime().setDateTime(newDate);
                    event.setStart(start);
                    event.setEnd(end);
                    rtn.add(event);
                    difference -= 1800000;

                    counter+=1;
                }
            }
        }

        return rtn;
    }

    private void addNextDialog() {
        currentDialog = dialogsQueue.peek();
        dialogsQueue.remove();
        System.out.println("showing dialog...");
        removeDialog(1);
        showDialog(1);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 1:
                String title;
                Dialog dialog = null;
                if (currentDialog == null) return null;
                if (currentDialog.equals("startDate")) {
                    title = "Start Date";
                    System.out.println("creating dialogue with year " + year);
                    MyDatePickerDialog dpDialog = new MyDatePickerDialog(MainActivity.this, this, year, month, day);
                    LayoutInflater inflater = getLayoutInflater();
                    View view = inflater.inflate(R.layout.calendar_title, null);
                    ((TextView) view.findViewById(R.id.text_calendar_title)).setText(title);
                    dpDialog.setCustomTitle(view);
                    dialog = dpDialog;
                } else if (currentDialog.equals("endDate")) {
                    title = "End Date";
                    System.out.println("creating dialogue with year " + year);
                    MyDatePickerDialog dpDialog = new MyDatePickerDialog(MainActivity.this, this, year, month, day);
                    LayoutInflater inflater = getLayoutInflater();
                    View view = inflater.inflate(R.layout.calendar_title, null);
                    ((TextView) view.findViewById(R.id.text_calendar_title)).setText(title);
                    dpDialog.setCustomTitle(view);
                    dialog = dpDialog;
                } else if (currentDialog.equals("time")) {
                    title = "Select Hours";
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(title);
                    final NumberPicker picker = new NumberPicker(this);
                    int len = 48;
                    picker.setMaxValue(len - 1);
                    picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                    picker.setMinValue(0);
                    String values[] = new String[len];
                    for (int i = 0; i <= len - 1; i++) {
                        values[i] = Double.toString((i + 1) * 0.5);
                    }
                    picker.setDisplayedValues(values);
                    picker.computeScroll();
                    final LinearLayout parent = new LinearLayout(this);
                    parent.addView(picker, new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            Gravity.CENTER));
                    builder.setView(parent);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            hoursChunk = ((picker.getValue())+1)/2 * 60 * 60000;
                            addNextDialog();
                        }
                    });
                    dialog = builder.create();
                }
                return dialog;
            default:
                return null;
        }
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
        } else {
            new MakeRequestTask(mCredential, this).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }


    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */


    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;
        private onTaskCompleted listener;

        public MakeRequestTask(GoogleAccountCredential credential, onTaskCompleted listener) {
            this.listener = listener;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }

        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         *
         * @return List of Strings describing returned events.
         * @throws IOException
         */

        private List<String> getDataFromApi() throws IOException {

            // List the next 10 events from the primary calendar.
            DateTime now = new DateTime(System.currentTimeMillis());
            List<String> eventStrings = new ArrayList<String>();
            DateTime newStart = new DateTime(MainActivity.startDate, TimeZone.getDefault());
            DateTime newEnd = new DateTime(MainActivity.endDate, TimeZone.getDefault());
            Events events = mService.events().list("primary")
                    .setSingleEvents(true)
                    .setMaxResults(1000)
                    .setTimeMin(newStart)
                    .setTimeMax(newEnd)
                    .setOrderBy("startTime")

                    .execute();
            List<Event> items = events.getItems();

            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                DateTime end = event.getEnd().getDateTime();
                if (start == null) {
                    // All-day events don't have start times, so just use
                    // the start date.
                    start = event.getStart().getDate();
                }
                if (end == null) {
                    end = event.getEnd().getDate();
                }
                eventStrings.add(
                        String.format("%s (%s)", event.getSummary(), start));
                starts.add(start);
                ends.add(end);
            }
            //Toast.makeText(getApplicationContext(), eventStrings.toString(), Toast.LENGTH_LONG).show();
            return eventStrings;
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Google Calendar API:");
                //mOutputText.setText(TextUtils.join("\n", output));

            }
            listener.onTaskCompleted();
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                    Toast.makeText(getApplicationContext(), "The following error occurred:\n"
                            + mLastError.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }


}

