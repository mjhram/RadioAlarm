package com.mjhram.radioalarm.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mjhram.radioalarm.alarm.Alarm;
import com.mjhram.radioalarm.db.DatabaseManager;

import java.util.List;

/**
 * This BroadcastReceiver automatically (re)starts the alarm when the device is
 * rebooted. This receiver is set to be disabled (android:enabled="false") in the
 * application's manifest file. When the user sets the alarm, the receiver is enabled.
 * When the user cancels the alarm, the receiver is disabled, so that rebooting the
 * device will not trigger this receiver.
 */
public class BootReceiver extends BroadcastReceiver {
    AlarmReceiver alarm = new AlarmReceiver();
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
        {
            // TODO: 04.12.2016
            DatabaseManager.init(context);
            final List<Alarm> alarms = DatabaseManager.getAll(false);
            for(Alarm a: alarms) {
                a.schedule(context);
            }
        }
    }
}
