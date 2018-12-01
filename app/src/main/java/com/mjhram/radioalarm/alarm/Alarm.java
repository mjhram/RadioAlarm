package com.mjhram.radioalarm.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import com.mjhram.radioalarm.R;
import com.mjhram.radioalarm.service.AlarmReceiver;
import com.mjhram.radioalarm.service.BootReceiver;
import com.mjhram.radioalarm.util.Parcelables;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created by mariu on 04.12.2016.
 */

public class Alarm implements Parcelable {

    public static final String TAG = "Alarm";
    private int id;
    private boolean active;
    private boolean shouldVibrate;
    private String name;
    private String tonePath;
    private DateTime dateTime;
    private DateTimeFormatter dateTimeFormatter;
    private static DateTimeFormatter parcelableDateTimeFormatter = DateTimeFormat.forPattern("yyyy:MM:dd/kk:mm");
    private Context context;
    public boolean isCanceled;

    public Alarm() {
        isCanceled = false;
        active = true;
        shouldVibrate = true;
        name = "Alarm";
        dateTime = DateTime.now();
        id = -1;
        dateTimeFormatter = DateTimeFormat.forPattern("kk:mm");

        try {
            tonePath = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
        } catch (Exception e) {
            tonePath = "";
            e.printStackTrace();
        }
    }

    int getReqCode() {
        String tmp="";
        tmp = String.format("%02d%02d",dateTime.getHourOfDay(), dateTime.getMinuteOfHour());
        int reqCode = Integer.parseInt(tmp.trim());
        isCanceled = false;
        return reqCode;
    }
    public void schedule(Context context) {
        this.context = context;
        active = true;

        reset(); //check if alarm is older than now and reset it.
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        //alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(dateTime.getMillis(), pendingIntent), pendingIntent);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, dateTime.getMillis(),1000 * 3600 * 24
                , getAlarmPedingIntent(context, false));

        // Enable {@code BootReceiver} to automatically restart the alarm when the
        // device is rebooted.
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        Toast.makeText(context, getTimeUntilNextAlarmMessage(), Toast.LENGTH_SHORT).show();
    }

    public boolean checkAndDeActivate(Context cntx){
        if(isCanceled) {
            PendingIntent pi = getAlarmPedingIntent(cntx, true);
            if(pi == null) {
                //no need for entry
                return  false;
            } else {
                cancelAlarm(cntx);
            }
        }else{
            schedule(cntx);
        }
        return true;
    }

    private PendingIntent getAlarmPedingIntent(Context context, boolean check) {
        //check=true to check if pending intent exist or not
        Intent myIntent = new Intent(context, AlarmReceiver.class);
        final Bundle bundle = new Bundle();
        bundle.putByteArray(Alarm.TAG, Parcelables.toByteArray(this));
        myIntent.putExtras(bundle);
        PendingIntent pendingIntent = null;
        if(check) {
            pendingIntent = PendingIntent.getBroadcast(context, getReqCode(), myIntent, PendingIntent.FLAG_NO_CREATE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context, getReqCode(), myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return pendingIntent;
    }

    public void cancelAlarm(Context context) {
        active = false;//??
        isCanceled = true;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarmManager.cancel(getAlarmPedingIntent(context, false));
    }

    public String getAlarmTimeString() {
        return dateTimeFormatter.print(dateTime);
    }

    public String getAlarmTimeStringParcelable() {
        return parcelableDateTimeFormatter.print(dateTime);
    }

    public void setAlarmTimeStringParcelable(String parcelableAlarmTimeString) {
        try {
            dateTime = parcelableDateTimeFormatter.parseDateTime(parcelableAlarmTimeString);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean shouldVibrate() {
        return shouldVibrate;
    }

    public void setShouldVibrate(boolean shouldVibrate) {
        this.shouldVibrate = shouldVibrate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTonePath() {
        return tonePath;
    }

    public void setTonePath(String tonePath) {
        this.tonePath = tonePath;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    public String getTimeUntilNextAlarmMessage() {
        Duration duration = new Duration(DateTime.now(), dateTime);

        final long days = duration.getStandardDays();
        duration = duration.minus(days * 24 * 60 * 60 * 1000);

        final long hours = duration.getStandardHours();
        duration = duration.minus(hours * 60 * 60 * 1000);

        final long minutes = duration.getStandardMinutes();

        String message = "Alarm will sound in ";
        if (context != null) {
            message = context.getResources().getString(R.string.alarm_will_sound_in);
        }

        if (hours > 0) {
            if (context != null) {
                message += String.format(context.getResources().getString(R.string.alarm_hours_and_minutes), hours, minutes);
            } else {
                message += String.format("%d hours and %d minutes", hours, minutes);
            }
        } else {
            if (minutes > 0) {
                if (context != null) {
                    message += String.format(context.getResources().getString(R.string.alarm_minutes), minutes);
                } else {
                    message += String.format("%d minutes", minutes);
                }
            } else {
                if (context != null) {
                    message += context.getResources().getString(R.string.alarm_less_minute);
                } else {
                    message += "less than a minute";
                }
            }
        }


        return message;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(tonePath);
        dest.writeString(getAlarmTimeStringParcelable());
        dest.writeString(String.valueOf(shouldVibrate));
        dest.writeString(String.valueOf(active));
        dest.writeString(String.valueOf(isCanceled));
    }

    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<Alarm>() {

        public Alarm createFromParcel(Parcel source) {
            final Alarm alarm = new Alarm();

            alarm.setId(source.readInt());
            alarm.setName(source.readString());
            alarm.setTonePath(source.readString());
            alarm.setDateTime(DateTime.parse(source.readString(), parcelableDateTimeFormatter));
            alarm.setShouldVibrate(Boolean.parseBoolean(source.readString()));
            alarm.setActive(Boolean.parseBoolean(source.readString()));
            alarm.isCanceled = Boolean.parseBoolean(source.readString());
            return alarm;
        }

        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    public void reset() {
        final DateTime now = DateTime.now();
        final DateTime old = dateTime;

        // old date is in past
        if (dateTime.isBeforeNow()) {

            // same day?
            dateTime = dateTime
                    .withYear(now.getYear())
                    .withDayOfMonth(now.getDayOfMonth())
                    .withHourOfDay(old.getHourOfDay())
                    .withMinuteOfHour(old.getMinuteOfHour())
                    .withSecondOfMinute(0);

            // next day?
            if (dateTime.isBeforeNow()) {
                dateTime = dateTime.plusDays(1);
            }
        }
    }
}
