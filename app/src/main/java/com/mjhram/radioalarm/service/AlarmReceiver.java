package com.mjhram.radioalarm.service;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.mjhram.radioalarm.PlayerService;
import com.mjhram.radioalarm.alarm.Alarm;
import com.mjhram.radioalarm.core.Station;
import com.mjhram.radioalarm.db.DatabaseManager;
import com.mjhram.radioalarm.helpers.LogHelper;
import com.mjhram.radioalarm.helpers.StationListHelper;
import com.mjhram.radioalarm.util.Parcelables;

import java.util.ArrayList;

import static com.mjhram.radioalarm.helpers.TransistorKeys.ACTION_PLAY;
import static com.mjhram.radioalarm.helpers.TransistorKeys.EXTRA_STATION;

/**
 * When the alarm fires, this WakefulBroadcastReceiver receives the broadcast Intent 
 * and then starts the IntentService {@code SchedulingService} to do some work.
 */
public class AlarmReceiver extends WakefulBroadcastReceiver {

    private final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent AIntent) {

        Log.d(TAG, "onReceive: " + AIntent.toString());

        /*final Bundle bundle = intent.getExtras();
        final Intent serviceIntent = new Intent(context, SchedulingService.class);

        serviceIntent.putExtra(Alarm.TAG, bundle.getByteArray(Alarm.TAG));
        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, serviceIntent);
        */
        final Bundle bundle = AIntent.getExtras();
        Alarm alarm = Parcelables.toParcelableAlarm(bundle.getByteArray(Alarm.TAG));
        DatabaseManager.init(context);
        Alarm a = DatabaseManager.getExistingAlarm(alarm);
        if(a == null) {
            Log.d(TAG, "onReceive: Alarm not exist in DB.");
            alarm.cancelAlarm(context);
            return;
        }

        Intent serviceIntent = new Intent(context, PlayerService.class);
        serviceIntent.setAction(ACTION_PLAY);

        ArrayList<Station> newStationList = StationListHelper.loadStationListFromStorage(context);
        if(newStationList.size() > 0) {
            //startPlayback(newStationList.get(0));
            serviceIntent.putExtra(EXTRA_STATION, newStationList.get(0));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            }else {
                context.startService(serviceIntent);
            }
        }

        //mPlayerServiceStation = station;
        LogHelper.v(TAG, "Starting player service.");
    }
}
