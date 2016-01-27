package com.moetto.moso3;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by moetto on 27/01/16.
 */
public class CalendarPoller extends IntentService {

    private final String TAG = "CalendarPoller";
    private Handler handler;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public CalendarPoller(String name) {
        super(name);
        handler = new Handler();
        Log.d(TAG, "Created service");
    }

    public CalendarPoller() {
        this("CalendarPoller");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Handling intent");
        handler.removeCallbacks(null);

        Cursor eventCursor = getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                new String[]{CalendarContract.Calendars.NAME},
                null,
                new String[]{CalendarContract.Calendars.IS_PRIMARY},
                CalendarContract.Calendars.NAME
        );
        try {

            while (eventCursor.moveToNext()) {
                Log.d(TAG, eventCursor.getString(0));
                eventCursor.moveToNext();
            }

            eventCursor.close();
        } catch (NullPointerException nux) {
            Log.e(TAG, Log.getStackTraceString(nux));
            Toast.makeText(this, "Error getting calendar data", Toast.LENGTH_SHORT).show();
        }

        eventCursor = getContentResolver().query(
                CalendarContract.Events.CONTENT_URI,
                new String[]{CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND},
                null,
                new String[]{CalendarContract.Events.IS_PRIMARY},
                CalendarContract.Events.DTSTART
        );


        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startService(new Intent(getApplicationContext(), CalendarPoller.class));
            }
        }, 60 * 5 );
    }
}
