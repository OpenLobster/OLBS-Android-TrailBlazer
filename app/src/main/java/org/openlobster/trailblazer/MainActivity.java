/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openlobster.trailblazer;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import android.content.SharedPreferences;
import android.content.Context;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;


/**
 * This sample demonstrates how to schedule an alarm that causes a service to
 * be started. This is useful when you want to schedule alarms that initiate
 * long-running operations, such as retrieving a daily forecast.
 * This particular sample retrieves content from the Google home page once a day and  
 * checks it for the search string "doodle". If it finds this string, that indicates 
 * that the page contains a custom doodle instead of the standard Google logo.
 */
public class MainActivity extends Activity {
    AlarmReceiver alarm = new AlarmReceiver();
    SharedPreferences sharedpreferences;

    public static final String MyPREFERENCES = "MyPrefs" ;
    private final static int REQUEST_ENABLE_BT = 1;
    private boolean mHasBLE = true;
    ScanDB scandb;

    private MenuItem startMenuItem;
    private MenuItem cancelMenuItem;

    private ResponseReceiver receiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {

            setContentView(R.layout.activity_no_ble);
            mHasBLE = false;
        }

        scandb = new ScanDB(this);
        scandb.createRedeem();
        scandb.createSchedule();

        BluetoothManager btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();
        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        IntentFilter filter = new IntentFilter(ResponseReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new ResponseReceiver();
        registerReceiver(receiver, filter);

    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        BluetoothManager btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = btManager.getAdapter();
        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        IntentFilter filter = new IntentFilter(ResponseReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new ResponseReceiver();
        registerReceiver(receiver, filter);

    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        startMenuItem = menu.getItem(1);
        cancelMenuItem = menu.getItem(2);


        //if (scandb.getSchedule()) {
        if (alarm.isAlarmSet(this)) {
            cancelMenuItem.setEnabled(true);
            startMenuItem.setEnabled(false);
        } else {
            cancelMenuItem.setEnabled(false);
            startMenuItem.setEnabled(true);
        }

        //disable Menu if device do not have BLE
        if (mHasBLE) {
            return true;
        } else {
            return false;
        }
    }

    // Menu options to set and cancel the alarm.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.about_action:
                //show about text
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            // When the user clicks START ALARM, set the alarm.
            case R.id.start_action:
                intent = new Intent();
                String UserToken = "1234";
                UserToken = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                intent.putExtra("UserToken", UserToken);

                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString("UserToken", UserToken);
                editor.commit();

                // setup regular background scan
                alarm.setAlarm(this, intent);

//                //setup the instant scan
                //Intent instantScanIntent = new Intent(this, SchedulingService.class);
                //startService(instantScanIntent);
                TextView scan_status = (TextView) findViewById(R.id.scan_status);
                scan_status.setText("準備搜尋");



                startMenuItem.setEnabled(false);
                scandb.setSchedule();
                cancelMenuItem.setEnabled(true);
                return true;
            // When the user clicks CANCEL ALARM, cancel the alarm. 
            case R.id.cancel_action:
                alarm.cancelAlarm(this);
                startMenuItem.setEnabled(true);
                scandb.resetSchedule();
                if(alarm.isAlarmSet(this))
                    cancelMenuItem.setEnabled(true);
                else
                    cancelMenuItem.setEnabled(false);
                return true;
            case R.id.found_action:
                //show about text
                intent = new Intent(this, FoundActivity.class);
                startActivity(intent);
                return true;
            case R.id.redeem_action:
                //show about text
                intent = new Intent(this, RedeemActivity.class);
                startActivity(intent);
                return true;
        }
        return false;
    }


    public class ResponseReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "org.openlobster.trailblazer.intent.action.SCAN_STATUS";

        @Override
        public void onReceive(Context context, Intent intent) {
            TextView scan_status = (TextView) findViewById(R.id.scan_status);
            String text = intent.getStringExtra(SchedulingService.PARAM_OUT_MSG);
            scan_status.setText(text);
        }
    }



}
