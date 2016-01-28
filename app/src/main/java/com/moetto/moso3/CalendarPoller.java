package com.moetto.moso3;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Handler;
import android.preference.RingtonePreference;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by moetto on 27/01/16.
 */
public class CalendarPoller extends IntentService {

    int oldMode = -1;
    int ringVolume;
    AudioManager audioManager;

    Runnable muteSound = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Set silent");
            handler.removeCallbacks(muteSound);
            if (oldMode == -1) {
                oldMode = audioManager.getRingerMode();
                ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
            }
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        }
    };

    Runnable poller = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Handling intent");
            handler.removeCallbacks(poller);
            Cursor eventCursor;
            int primary_id;
            try {
                eventCursor = getContentResolver().query(
                        CalendarContract.Calendars.CONTENT_URI,
                        new String[]{CalendarContract.Calendars._ID, CalendarContract.Calendars.NAME, CalendarContract.Calendars.IS_PRIMARY},
                        CalendarContract.Calendars.NAME + " = ?",
                        new String[]{"antti.moettoenen@gmail.com"},
                        CalendarContract.Calendars.NAME
                );

                if (eventCursor.moveToFirst()) {
                    Log.d(TAG, eventCursor.getString(1));
                    primary_id = eventCursor.getInt(0);
                } else {
                    Toast.makeText(getApplicationContext(), "Error getting calendar", Toast.LENGTH_SHORT).show();
                    return;
                }

                eventCursor.close();

                eventCursor = getContentResolver().query(
                        CalendarContract.Events.CONTENT_URI,
                        new String[]{CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND},
                        CalendarContract.Events.CALENDAR_ID + " = ? AND " + CalendarContract.Events.DTEND + " > ?",
                        new String[]{"" + primary_id, "" + System.currentTimeMillis()},
                        CalendarContract.Events.DTSTART
                );

                if (eventCursor.moveToFirst()) {
                    long startTime = eventCursor.getLong(1);
                    long endTime = eventCursor.getLong(2);
                    long currentTime = System.currentTimeMillis();

                    if ((startTime - currentTime) < 5 * 60 * 1000) {

                        Log.d(TAG, "Send mute handler triggering after " + Math.max(0, startTime - currentTime) / 1000 + " seconds");
                        handler.removeCallbacks(muteSound);
                        handler.postDelayed(muteSound, (long) Math.max(0.0, startTime - currentTime));
                        Log.d(TAG, "Send next polling after " + ((endTime - currentTime) / 60 / 1000) + " mins");
                        handler.postDelayed(poller, endTime - currentTime);
                        eventCursor.close();
                        return;
                    }
                }
                eventCursor.close();

            } catch (NullPointerException nux) {
                Log.d(TAG, Log.getStackTraceString(nux));
                Toast.makeText(getApplicationContext(), "Error getting calendar data", Toast.LENGTH_SHORT).show();
            }

            //TODO set 1000 instead of 100
            handler.postDelayed(poller, 60 * 5 * 100);
            if (oldMode != -1) {
                audioManager.setRingerMode(oldMode);
                oldMode = -1;
            }

        }
    };

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
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        handler.post(poller);
    }
}
