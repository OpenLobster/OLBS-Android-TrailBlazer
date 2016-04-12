package org.openlobster.trailblazer;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class FoundActivity extends ListActivity {

    ScanDB scandb;

    int ibeacon_num = 0;
    int eddystone_num = 0;
    int ibeacon_points = 0;
    int eddystone_points = 0;

    private ArrayList<String> results = new ArrayList<String>();
    //private String tableName = DBHelper.tableName;
    private SQLiteDatabase newDB;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        scandb = new ScanDB(this);
        openAndQueryDatabase();
        displayResultList();

    }
    private void displayResultList() {
        TextView tView = new TextView(this);
        int total_points = ibeacon_points + eddystone_points;
        tView.setText("總分:" + (total_points) + ", iBeacon: " + ibeacon_num + "個, Eddystone: " + eddystone_num + "個");
        getListView().addHeaderView(tView);

        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, results));
        getListView().setTextFilterEnabled(true);

    }
    private void openAndQueryDatabase() {
        try {
            //DBHelper dbHelper = new DBHelper(this.getApplicationContext());
            newDB = scandb.getWritableDatabase();
            Cursor c = newDB.rawQuery("SELECT UUID, MAJOR, MINOR, POINTS, LOGTIME FROM IBEACON order by LOGTIME desc ", null);

            if (c != null ) {
                if (c.moveToFirst()) {
                    do {
                        String UUID = c.getString(c.getColumnIndex("UUID"));
                        int Major = c.getInt(c.getColumnIndex("MAJOR"));
                        int Minor = c.getInt(c.getColumnIndex("MINOR"));
                        int POINTS = c.getInt(c.getColumnIndex("POINTS"));
                        String LOGTIME = c.getString(c.getColumnIndex("LOGTIME"));
                        results.add("iBeacon: " + UUID.substring(24, 32) + "-(" + Major + "-" + Minor + ") [" + POINTS + "]\n  " + LOGTIME);
                        ibeacon_num++;
                        ibeacon_points+=POINTS;
                    }while (c.moveToNext());
                }
            }
            c.close();
        } catch (SQLiteException se ) {
            Log.e(getClass().getSimpleName(), "Could not create or Open the database");
        }

        try {
            Cursor c = newDB.rawQuery("SELECT UID, POINTS, LOGTIME FROM EDDYSTONE order by LOGTIME desc ", null);

            if (c != null ) {
                if (c.moveToFirst()) {
                    do {
                        String UID = c.getString(c.getColumnIndex("UID"));
                        int POINTS = c.getInt(c.getColumnIndex("POINTS"));
                        String LOGTIME = c.getString(c.getColumnIndex("LOGTIME"));

                        String Inno2015 = "";
                        if (UID.equals("f7826da6bc5b71e0893e324c727a696f"))
                            Inno2015 = "Green Technology";
                        if (UID.equals("f7826da6bc5b71e0893e45543459696f"))
                            Inno2015 = "ICT";
                        if (UID.equals("f7826da6bc5b71e0893e62413334696f"))
                            Inno2015 = "Robotics";
                        if (UID.equals("f7826da6bc5b71e0893e6c445a75696f"))
                            Inno2015 = "Medical Science 1";
                        if (UID.equals("f7826da6bc5b71e0893e75547a41696f"))
                            Inno2015 = "Medical Science 2";
                        if (UID.equals("f7826da6bc5b71e0893e75726c4b696f"))
                            Inno2015 = "ITSC";
                        results.add("Eddystone: " + UID.substring(12, 32) + " [" + POINTS + "]\n  " + LOGTIME + " " + Inno2015);
                        eddystone_num++;
                        eddystone_points+=POINTS;
                    }while (c.moveToNext());
                }
            }
            c.close();
        } catch (SQLiteException se ) {
        Log.e(getClass().getSimpleName(), "Could not create or Open the database");
    }
//        finally {
//            if (newDB != null)
//                newDB.execSQL("DELETE FROM " + tableName);
//            newDB.close();
//        }

    }

}