package com.mjhram.radioalarm.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mjhram.radioalarm.alarm.Alarm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mariu on 04.12.2016.
 */

public class DatabaseManager extends SQLiteOpenHelper {

    private static SQLiteDatabase database = null;
    private static DatabaseManager instance = null;

    private static final String DATABASE_NAME = "DB";
    private static final int DATABASE_VERSION = 4;

    private static final String ALARM_TABLE = "alarm";
    private static final String COLUMN_ALARM_ID = "_id";
    private static final String COLUMN_ALARM_ACTIVE = "alarm_active";
    private static final String COLUMN_ALARM_TIME = "alarm_time";
    private static final String COLUMN_ALARM_TONE = "alarm_tone";
    private static final String COLUMN_ALARM_VIBRATE = "alarm_vibrate";
    private static final String COLUMN_ALARM_NAME = "alarm_name";
    private static final String COLUMN_ALARM_CANCELED = "alarm_canceled";

    private DatabaseManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context);
        }
    }

    public static long create(Alarm alarm) {
        return getDatabase().insert(ALARM_TABLE, null, buildAlarmQuery(alarm));
    }

    public static SQLiteDatabase getDatabase() {
        if (null == database) {
            database = instance.getWritableDatabase();
        }
        return database;
    }

    private static ContentValues buildAlarmQuery(Alarm alarm) {
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_ALARM_ACTIVE, alarm.isActive());
        cv.put(COLUMN_ALARM_TIME, alarm.getAlarmTimeStringParcelable());
        cv.put(COLUMN_ALARM_TONE, alarm.getTonePath());
        cv.put(COLUMN_ALARM_VIBRATE, alarm.shouldVibrate());
        cv.put(COLUMN_ALARM_NAME, alarm.getName());
        cv.put(COLUMN_ALARM_CANCELED, alarm.isCanceled);

        return cv;
    }

    public static Alarm getExistingAlarm(Alarm a) {
        String[] columns = new String[]{
                COLUMN_ALARM_ID,
                COLUMN_ALARM_ACTIVE,
                COLUMN_ALARM_TIME,
                COLUMN_ALARM_TONE,
                COLUMN_ALARM_VIBRATE,
                COLUMN_ALARM_NAME,
                COLUMN_ALARM_CANCELED
        };

        final Cursor c = getDatabase().query(ALARM_TABLE, columns, COLUMN_ALARM_TIME + "='" + a.getAlarmTimeStringParcelable() +"'",
                null, null, null, null);
        Alarm alarm = null;

        if (c.moveToFirst()) {
            alarm = new Alarm();
            alarm.setId(c.getInt(c.getColumnIndex(COLUMN_ALARM_ID)));
            alarm.setActive(c.getInt(c.getColumnIndex(COLUMN_ALARM_ACTIVE)) == 1);
            alarm.setAlarmTimeStringParcelable(c.getString(c.getColumnIndex(COLUMN_ALARM_TIME)));
            alarm.setTonePath(c.getString(c.getColumnIndex(COLUMN_ALARM_TONE)));
            alarm.setShouldVibrate(c.getInt(c.getColumnIndex(COLUMN_ALARM_VIBRATE)) == 1);
            alarm.setName(c.getString(c.getColumnIndex(COLUMN_ALARM_NAME)));
            alarm.isCanceled = c.getInt(c.getColumnIndex(COLUMN_ALARM_CANCELED)) == 1;
        }
        c.close();
        return alarm;
    }

    public static Alarm getAlarm(int id) {
        String[] columns = new String[]{
                COLUMN_ALARM_ID,
                COLUMN_ALARM_ACTIVE,
                COLUMN_ALARM_TIME,
                COLUMN_ALARM_TONE,
                COLUMN_ALARM_VIBRATE,
                COLUMN_ALARM_NAME,
                COLUMN_ALARM_CANCELED
        };

        final Cursor c = getDatabase().query(ALARM_TABLE, columns, COLUMN_ALARM_ID + "=" + id,
                null, null, null, null);
        Alarm alarm = null;

        if (c.moveToFirst()) {
            alarm = new Alarm();
            alarm.setId(c.getInt(c.getColumnIndex(COLUMN_ALARM_ID)));
            alarm.setActive(c.getInt(c.getColumnIndex(COLUMN_ALARM_ACTIVE)) == 1);
            alarm.setAlarmTimeStringParcelable(c.getString(c.getColumnIndex(COLUMN_ALARM_TIME)));
            alarm.setTonePath(c.getString(c.getColumnIndex(COLUMN_ALARM_TONE)));
            alarm.setShouldVibrate(c.getInt(c.getColumnIndex(COLUMN_ALARM_VIBRATE)) == 1);
            alarm.setName(c.getString(c.getColumnIndex(COLUMN_ALARM_NAME)));
            alarm.isCanceled = c.getInt(c.getColumnIndex(COLUMN_ALARM_CANCELED)) == 1;

        }

        c.close();
        return alarm;
    }

    public static Cursor getCursor() {
        String[] columns = new String[]{
                COLUMN_ALARM_ID,
                COLUMN_ALARM_ACTIVE,
                COLUMN_ALARM_TIME,
                COLUMN_ALARM_TONE,
                COLUMN_ALARM_VIBRATE,
                COLUMN_ALARM_NAME,
                COLUMN_ALARM_CANCELED
        };

        return getDatabase().query(ALARM_TABLE, columns, null, null, null, null, null);
    }

    public static List<Alarm> getAll(boolean isCanceledIncluded) {
        List<Alarm> alarms = new ArrayList<Alarm>();
        Cursor cursor = DatabaseManager.getCursor();

        if (cursor.moveToFirst()) {

            do {
                final Alarm alarm = new Alarm();
                alarm.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_ID)));
                alarm.setActive(cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_ACTIVE)) == 1);
                alarm.setAlarmTimeStringParcelable(cursor.getString(cursor.getColumnIndex(COLUMN_ALARM_TIME)));
                alarm.setTonePath(cursor.getString(cursor.getColumnIndex(COLUMN_ALARM_TONE)));
                alarm.setShouldVibrate(cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_VIBRATE)) == 1);
                alarm.setName(cursor.getString(cursor.getColumnIndex(COLUMN_ALARM_NAME)));
                alarm.isCanceled = cursor.getInt(cursor.getColumnIndex(COLUMN_ALARM_CANCELED)) == 1;
                if(alarm.isCanceled == false) {
                    alarms.add(alarm);
                } else if(isCanceledIncluded == true) {
                    alarms.add(alarm);
                }

            } while (cursor.moveToNext());
        }
        cursor.close();
        return alarms;
    }

    public static int update(Alarm alarm) {
        return getDatabase().update(ALARM_TABLE, buildAlarmQuery(alarm), "_id=" + alarm.getId(), null);
    }

    public static int deleteEntry(Alarm alarm) {
        return deleteEntry(alarm.getId());
    }

    public static int deleteEntry(int id) {
        Alarm a=getAlarm(id);
        a.isCanceled=true;
        update(a);
        return 0;
        //return getDatabase().delete(ALARM_TABLE, COLUMN_ALARM_ID + "=" + id, null);
    }

    /*public static int deleteAll() {
        return getDatabase().delete(ALARM_TABLE, "1", null);
    }*/

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + ALARM_TABLE + " ( "
                + COLUMN_ALARM_ID + " INTEGER primary key autoincrement, "
                + COLUMN_ALARM_ACTIVE + " INTEGER NOT NULL, "
                + COLUMN_ALARM_TIME + " TEXT NOT NULL, "
                + COLUMN_ALARM_TONE + " TEXT NOT NULL, "
                + COLUMN_ALARM_VIBRATE + " INTEGER NOT NULL, "
                + COLUMN_ALARM_CANCELED + " INTEGER NOT NULL, "
                + COLUMN_ALARM_NAME + " TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ALARM_TABLE);
        onCreate(db);
    }
}
