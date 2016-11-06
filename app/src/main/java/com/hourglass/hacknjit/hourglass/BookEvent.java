package com.hourglass.hacknjit.hourglass;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by David on 11/6/16.
 */

public class BookEvent extends AsyncTask<Void, Void,String>{

    private Calendar service = null;

    private String title = null;
    private String description = null;

    private DateTime start_date = null;
    private DateTime end_date = null;

    private Context application = null;

    private static ArrayList<String> peopleToAdd = new ArrayList<String>();

    public BookEvent(Context application,
                     Calendar services,
                     String title,
                     String description,
                     DateTime eventStartTime,
                     DateTime eventEndTime
                     //ArrayList<String> emails
    ){
        this.application = application;
        this.service = services;
        this.title = title;
        this.description = description;
        this.start_date = eventStartTime;
        this.end_date = eventEndTime;
        //this.peopleToAdd = emails;
    }

    @Override
    protected String doInBackground(Void... params) {

        boolean flagBooking  = false;
        try {
            System.out.println("in background running test");
            flagBooking = bookEvent();
        }
        catch(Exception e){
            e.printStackTrace();
        }
        if(flagBooking)
            return "true";
        else
            return null;
    }

    @Override
    protected void onPostExecute(String token) {

        if(token != null && token.length() > 0 ){
            Toast.makeText(application, "Success! Your Calendar has been updated!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(application, "Error! Failure to insert event!", Toast.LENGTH_LONG).show();
        }
    }

    // should we be able set reoccuring events?
    public boolean bookEvent() throws IOException  {

        System.out.println("Creating event...");
        String calendarId = "primary";

        Event event = new Event();

        DateTime startValue = start_date;
        EventDateTime start = new EventDateTime()
                .setDateTime(startValue)
                .setTimeZone("America/New_York");
        event.setStart(start);

        DateTime endValue = end_date;
        EventDateTime end = new EventDateTime()
                .setDateTime(endValue)
                .setTimeZone("America/New_York");
        event.setEnd(end);

        // event.setAttendees(setAttendants(peopleToAdd));

        // set event title
        event.setSummary(title);

        // set description
        event.setDescription(description);
        calendarId = (service.calendarList().get(calendarId)).getCalendarId();
        Calendar.CalendarList x = service.calendarList();
        event = service.events().insert(calendarId, event).execute();
        System.out.printf("Event created: %s\n", event.getHtmlLink());
        if(event.getHtmlLink().length() > 0){
            return true;
        } else {
            return false;
        }
    }

    // currently not used
    private ArrayList<EventAttendee> setAttendants(ArrayList<String> attendees){
        ArrayList<EventAttendee> people = new ArrayList<>();
        for(String attendant : attendees){
            people.add(new EventAttendee().setEmail(attendant));
            System.out.println("adding email...");
        }
        System.out.println(people.toString());
        return people;
    }

}
