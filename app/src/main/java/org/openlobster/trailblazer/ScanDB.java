package org.openlobster.trailblazer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Created by view on 14/10/15.
 */
public class ScanDB extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "TrailBlazer.db";
    public static final String IBEACON_TABLENAME = "IBEACON";
    public static final String IBEACON_UUID = "UUID";
    public static final String IBEACON_MAJOR = "MAJOR";
    public static final String IBEACON_MINOR = "MINOR";
    public static final String IBEACON_POINTS = "POINTS";
    public static final String IBEACON_LOGTIME = "LOGTIME";

    public static final String EDDYSTONE_TABLENAME = "EDDYSTONE";
    public static final String EDDYSTONE_UID = "UID";
    public static final String EDDYSTONE_POINTS = "POINTS";
    public static final String EDDYSTONE_LOGTIME = "LOGTIME";

    SimpleDateFormat mDateFormat;

    public ScanDB(Context context)
    {
        super(context, DATABASE_NAME, null, 1);
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        Log.d("scanDB", "onCreate");
        db.execSQL(
                "create table if not exists " + IBEACON_TABLENAME + "(" +
                        IBEACON_UUID + " text, " +
                        IBEACON_MAJOR + " integer, " +
                        IBEACON_MINOR + " integer, " +
                        IBEACON_POINTS + " integer, " +
                        IBEACON_LOGTIME + " datime)"
        );
        db.execSQL(
                "create table if not exists " + EDDYSTONE_TABLENAME + "(" +
                        EDDYSTONE_UID + " text, " +
                        EDDYSTONE_POINTS + " integer, " +
                        EDDYSTONE_LOGTIME + " datime)"
        );
        db.execSQL(
                "create table if not exists REDEEM (EVENT text, STATUS integer)"
        );
        db.execSQL(
                "create table if not exists SCHEDULE (ID text, STATUS integer)"
        );
        //db.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS " + IBEACON_TABLENAME);
        db.execSQL("DROP TABLE IF EXISTS " + EDDYSTONE_TABLENAME);
        onCreate(db);
    }

    public boolean checkNewiBeacon (String UUID, Integer MAJOR, Integer MINOR)
    {
        int points = 50;
        Log.d("scanDB", "checkNewiBeacon");
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL(
                "create table if not exists " + IBEACON_TABLENAME + "(" +
                        IBEACON_UUID + " text, " +
                        IBEACON_MAJOR + " integer, " +
                        IBEACON_MINOR + " integer, " +
                        IBEACON_POINTS + " integer, " +
                        IBEACON_LOGTIME + " datime)"
        );
        Cursor res =  db.rawQuery("select * from " + IBEACON_TABLENAME +
                " where " + IBEACON_UUID + "='" + UUID + "'" +
                " and " + IBEACON_MAJOR + "='" + MAJOR + "'" +
                " and " + IBEACON_MINOR + "='" + MINOR + "'"
                , null);
        if (res.getCount() <= 0) {
            db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(IBEACON_UUID, UUID);
            contentValues.put(IBEACON_MAJOR, MAJOR);
            contentValues.put(IBEACON_MINOR, MINOR);
            contentValues.put(IBEACON_POINTS, points);
            Date date = new Date();
            contentValues.put(IBEACON_LOGTIME, mDateFormat.format(date));
            db.insert(IBEACON_TABLENAME, null, contentValues);
            res.close();
            db.close();
            return true;
        } else {
            res.close();
            db.close();
            return false;
        }
    }

    public boolean checkNewEddystone (String UID)
    {
        int points = 50;
        Log.d("scanDB", "checkNewEddystone");
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL(
                "create table if not exists " + EDDYSTONE_TABLENAME + "(" +
                        EDDYSTONE_UID + " text, " +
                        EDDYSTONE_POINTS + " integer, " +
                        EDDYSTONE_LOGTIME + " datime)"
        );
        Cursor res =  db.rawQuery("select * from " + EDDYSTONE_TABLENAME +
                " where " + EDDYSTONE_UID + "='" + UID + "' "
                , null);
        if (res.getCount() <= 0) {
            db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(EDDYSTONE_UID, UID);
            if (UID.equals("f7826da6bc5b71e0893e324c727a696f") ||
                UID.equals("f7826da6bc5b71e0893e45543459696f") ||
                UID.equals("f7826da6bc5b71e0893e62413334696f") ||
                UID.equals("f7826da6bc5b71e0893e6c445a75696f") ||
                UID.equals("f7826da6bc5b71e0893e75547a41696f") ||
                UID.equals("f7826da6bc5b71e0893e75726c4b696f"))
                points = 500;
            contentValues.put(EDDYSTONE_POINTS, points);
            Date date = new Date();
            contentValues.put(EDDYSTONE_LOGTIME, mDateFormat.format(date));
            db.insert(EDDYSTONE_TABLENAME, null, contentValues);
            res.close();
            db.close();
            return true;
        } else {
            res.close();
            db.close();
            return false;
        }
    }

    public boolean createSchedule ()
    {
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL(
                "create table if not exists SCHEDULE (ID text, STATUS boolean)"
        );
        Cursor res =  db.rawQuery("select * from SCHEDULE where ID='15MINS'", null);
        if (res.getCount() <= 0) {
            db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("ID", "15MINS");
            contentValues.put("STATUS", 0);
            db.insert("SCHEDULE", null, contentValues);
            res.close();
            db.close();
            return true;
        } else {
            res.close();
            db.close();
            return false;
        }
    }

    public boolean getSchedule()
    {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from SCHEDULE where ID='15MINS'", null);
        if (res.getCount() <= 0) {
            db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("ID", "15MINS");
            contentValues.put("STATUS", 0);
            db.insert("SCHEDULE", null, contentValues);
            res.close();
            db.close();
            return false;
        } else {
            if (res != null ) {
                if (res.moveToFirst()) {
                    int STATUS = res.getInt(res.getColumnIndex("STATUS"));
                    res.close();
                    db.close();
                    if (STATUS == 1)
                        return true;
                    else
                        return false;
                }
            }
        }
        return false;
    }

    public boolean setSchedule()
    {
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL(
                "update SCHEDULE set STATUS = 1 where ID='15MINS'"
        );
        db.close();
        return true;

    }

    public boolean resetSchedule ()
    {
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL(
                "update SCHEDULE set STATUS = 0 where ID='15MINS'"
        );
        db.close();
        return false;

    }


    public boolean createRedeem ()
    {
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL(
                "create table if not exists REDEEM (EVENT text, STATUS boolean)"
        );
        Cursor res =  db.rawQuery("select * from REDEEM where EVENT='INNO2015'", null);
        if (res.getCount() <= 0) {
            db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("EVENT", "INNO2015");
            contentValues.put("STATUS", 0);
            db.insert("REDEEM", null, contentValues);
            res.close();
            db.close();
            return true;
        } else {
            res.close();
            db.close();
            return false;
        }
    }

    public boolean getRedeem ()
    {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from REDEEM where EVENT='INNO2015'", null);
        if (res.getCount() <= 0) {
            db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("EVENT", "INNO2015");
            contentValues.put("STATUS", 0);
            db.insert("REDEEM", null, contentValues);
            res.close();
            db.close();
            return false;
        } else {
            if (res != null ) {
                if (res.moveToFirst()) {
                    int STATUS = res.getInt(res.getColumnIndex("STATUS"));
                    res.close();
                    db.close();
                    if (STATUS == 1)
                        return true;
                    else
                        return false;
                }
            }
        }
        return false;
    }

    public boolean setRedeem ()
    {
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL(
                "create table if not exists REDEEM (EVENT text, STATUS boolean)"
        );
        db.execSQL(
                "update REDEEM set STATUS = 1 where EVENT='INNO2015'"
        );
        db.close();
        return true;

    }

    public boolean resetRedeem ()
    {
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL(
                "create table if not exists REDEEM (EVENT text, STATUS boolean)"
        );
        db.execSQL(
                "update REDEEM set STATUS = 0 where EVENT='INNO2015'"
        );
        db.close();
        return true;

    }

    public int getPoints () {

        int ibeacon_num = 0;
        int eddystone_num = 0;
        int ibeacon_points = 0;
        int eddystone_points = 0;

        SQLiteDatabase db = this.getReadableDatabase();

        try {
            Cursor c = db.rawQuery("SELECT UUID, MAJOR, MINOR, POINTS FROM IBEACON ", null);

            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        String UUID = c.getString(c.getColumnIndex("UUID"));
                        int Major = c.getInt(c.getColumnIndex("MAJOR"));
                        int Minor = c.getInt(c.getColumnIndex("MINOR"));
                        int POINTS = c.getInt(c.getColumnIndex("POINTS"));
                        ibeacon_num++;
                        ibeacon_points += POINTS;
                    } while (c.moveToNext());
                }
            }
            c.close();

        } catch (SQLiteException se) {
            Log.e(getClass().getSimpleName(), "Could not create or Open the database");
        }
        try {
            Cursor c = db.rawQuery("SELECT UID, POINTS FROM EDDYSTONE ", null);

            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        String UID = c.getString(c.getColumnIndex("UID"));
                        int POINTS = c.getInt(c.getColumnIndex("POINTS"));
                        eddystone_num++;
                        eddystone_points += POINTS;
                    } while (c.moveToNext());
                }
            }
            c.close();

        } catch (SQLiteException se) {
            Log.e(getClass().getSimpleName(), "Could not create or Open the database");
        }
        db.close();
        return (ibeacon_points + eddystone_points);
    }

    public boolean clearAll ()
    {
        SQLiteDatabase db = this.getReadableDatabase();

        db.execSQL(
                "delete from " + IBEACON_TABLENAME
        );
        db.execSQL(
                "delete from " + EDDYSTONE_TABLENAME
        );
        db.close();
        return true;

    }
}
