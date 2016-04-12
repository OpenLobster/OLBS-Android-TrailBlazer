package org.openlobster.trailblazer;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.ParcelUuid;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.EditText;

import com.google.sample.eddystonevalidator.Beacon;
//import com.google.sample.eddystonevalidator.BeaconArrayAdapter;
import com.google.sample.eddystonevalidator.Constants;
import com.google.sample.eddystonevalidator.TlmValidator;
import com.google.sample.eddystonevalidator.UidValidator;
import com.google.sample.eddystonevalidator.UrlValidator;
import com.google.sample.eddystonevalidator.Utils;

import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;
import org.openlobster.olbs.OLBSBeacon;
import org.openlobster.olbs.OLBSBeaconScanningMethodType;
import org.openlobster.olbs.OLBSConfigType;
import org.openlobster.olbs.OLBSEventType;
import org.openlobster.olbs.OLBSLocation;
import org.openlobster.olbs.OLBSNotifier;
import org.openlobster.olbs.OpenLobster;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import javax.net.ssl.SSLSocketFactory;

import org.openlobster.trailblazer.MainActivity.ResponseReceiver;

/*
 * This {@code IntentService} does the app's actual work.
 * {@code SampleAlarmReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class SchedulingService extends IntentService implements OLBSNotifier {

    public static final String PARAM_IN_MSG = "imsg";
    public static final String PARAM_OUT_MSG = "omsg";

    public SchedulingService() {
        super("SchedulingService");
    }
    
    public static final String TAG = "Scheduling Demo";
    // An ID used to post the notification.
    public static final int NOTIFICATION_ID = 1;

    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;


    OpenLobster olsdk;
    private Object lock = new Object();

    String currentUuid = null;
    int currentMajor;
    int currentMinor;
    int no_of_beacon=0;
    int no_of_new_beacon = 0;
    int no_of_old_beacon = 0;
    String UserToken_String;
    int no_beacon_detect_count;

    // Google Eddystone - Start
    int no_of_eddystone = 0;
    int no_of_new_eddystone = 0;
    int no_of_old_eddystone = 0;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    // An aggressive scan for nearby devices that reports immediately.
    private static final ScanSettings SCAN_SETTINGS =
            new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER/*SCAN_MODE_LOW_LATENCY*/).setReportDelay(0)
                    .build();
    // The Eddystone Service UUID, 0xFEAA.
    private static final ParcelUuid EDDYSTONE_SERVICE_UUID =
            ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
    private BluetoothLeScanner scanner;
    //private BeaconArrayAdapter arrayAdapter;
    private List<ScanFilter> scanFilters;
    private ScanCallback scanCallback;
    private Map<String /* device address */, Beacon> deviceToBeaconMap = new HashMap<>();
    private EditText filter;
    // Google Eddystone - End

    protected Location mCurrentLocation;
    private int HttpConnectionTimeout = 5000;
    private int SocketConnectionTimeout = 5000;
    //scanlog variable
    private String  ScanLogUrl =   "https://www.openlobster.org/v1/scanLog";

    ScanDB scandb;

    Intent broadcastIntent;

    @Override
    protected void onHandleIntent(Intent intent) {

        scandb = new ScanDB(this);

        UserToken_String = intent.getStringExtra("UserToken");
        no_beacon_detect_count = 2;//3;

        Log.d(TAG, "Service Started!");

        olsdk = new OpenLobster(this);
        olsdk.setContext(this);

        //need to set OLBSAPIKey

        olsdk.SetBeaconScanningMethod(OLBSBeaconScanningMethodType.OLBS_FIND_ALL_BEACON,true);
        olsdk.SetBeaconScanningMethod(OLBSBeaconScanningMethodType.OLBS_FIND_NEAREST_BEACON,false);
        olsdk.SetBeaconScanningMethod(OLBSBeaconScanningMethodType.OLBS_ESTIMATE_LOCATION,true);
        olsdk.SetBeaconScanningMethod(OLBSBeaconScanningMethodType.OLBS_BEACON_MONITORING,false);

        olsdk.setOpenLobsterConfig(OLBSConfigType.OLBS_BEACON_FOREGROUND_BETWEEN_SCAN_PERIOD,100);
        olsdk.setOpenLobsterConfig(OLBSConfigType.OLBS_BEACON_FOREGROUND_SCAN_PERIOD,15000);

        olsdk.setOpenLobsterConfig(OLBSConfigType.OLBS_LOCATION_FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS,20000);
        olsdk.setOpenLobsterConfig(OLBSConfigType.OLBS_LOCATION_UPDATE_INTERVAL_IN_MILLISECONDS,40000);

        olsdk.setOpenLobsterConfig(OLBSConfigType.OLBS_HTTP_CONNECTION_TIMEOUT, 5000);
        olsdk.setOpenLobsterConfig(OLBSConfigType.OLBS_SOCKET_CONNECTION_TIMEOUT,5000);

        olsdk.setOpenLobsterConfig(OLBSConfigType.OLBS_USER_TOKEN,UserToken_String);

       //olsdk.addBeaconRegion("CUHK","6375686B-2E65-6475-2E68-6B2E30303031",null,null);
        olsdk.addBeaconRegion("ALL",null,null,null);
        olsdk.onCreate();

//        //send out scan status
//        Intent broadcastIntent = new Intent();
//        broadcastIntent.setAction(ResponseReceiver.ACTION_RESP);
//        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
//        broadcastIntent.putExtra(PARAM_OUT_MSG, "搜尋 iBeacon");
//        sendBroadcast(broadcastIntent);

        try {
            synchronized(lock) {
                Log.d(TAG, "Wait for iBeacon scan");
                lock.wait();
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //olsdk.deleteBeaconRegion("CUHK");
        olsdk.deleteBeaconRegion("ALL");

        //if (no_of_beacon > 0)
        //    sendNotification("UUID:" + currentUuid + " (" + no_of_beacon + ")");

        // END_INCLUDE(service_onhandle)

        olsdk.onDestroy();
     //   olsdk = null;

        Log.d(TAG, "Start Eddystone");
        //send out scan status
        broadcastIntent = new Intent();
        broadcastIntent.setAction(ResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(PARAM_OUT_MSG, "正搜尋 iBeacon 和 Eddystone");
        sendBroadcast(broadcastIntent);

        //Google Eddystone
        BluetoothManager manager = (BluetoothManager) getApplicationContext()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter btAdapter = manager.getAdapter();
        scanner = btAdapter.getBluetoothLeScanner();
        ArrayList<Beacon> arrayList = new ArrayList<>();
        //arrayAdapter = new BeaconArrayAdapter(getActivity(), R.layout.beacon_list_item, arrayList);
        scanFilters = new ArrayList<>();
        scanFilters.add(new ScanFilter.Builder().setServiceUuid(EDDYSTONE_SERVICE_UUID).build());
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord == null) {
                    return;
                }
                String deviceAddress = result.getDevice().getAddress();
                Beacon beacon;
                if (!deviceToBeaconMap.containsKey(deviceAddress)) {
                    beacon = new Beacon(deviceAddress, result.getRssi());
                    deviceToBeaconMap.put(deviceAddress, beacon);
                    //arrayAdapter.add(beacon);
                } else {
                    deviceToBeaconMap.get(deviceAddress).rssi = result.getRssi();
                }

                byte[] serviceData = scanRecord.getServiceData(EDDYSTONE_SERVICE_UUID);
                Log.d(TAG, deviceAddress + " " + Utils.toHexString(serviceData));
                //validateServiceData(deviceAddress, serviceData);
                //only handle UID Frame

                if (serviceData[0] == Constants.UID_FRAME_TYPE)
                {

                    int txPower = (int) serviceData[1];
                    byte[] uidBytes = Arrays.copyOfRange(serviceData, 2, 18);
                    String uidValue = Utils.toHexString(uidBytes);
                    no_of_eddystone++;
                    deviceToBeaconMap.get(deviceAddress).hasUidFrame = true;
                    deviceToBeaconMap.get(deviceAddress).uidStatus.uidValue = uidValue;
                    if (scandb.checkNewEddystone(uidValue)) {
                        no_of_new_eddystone++;
                    } else {
                        no_of_old_eddystone++;
                    }
                }
                if (serviceData[0] == Constants.URL_FRAME_TYPE)
                {
                    Log.d(TAG, deviceAddress + " URL FRAME ");
                }
                if (serviceData[0] == Constants.TLM_FRAME_TYPE)
                {
                    Log.d(TAG, deviceAddress + " TLM FRAME ");
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                switch (errorCode) {
                    case SCAN_FAILED_ALREADY_STARTED:
                        //logErrorAndShowToast("SCAN_FAILED_ALREADY_STARTED");
                        break;
                    case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                        //logErrorAndShowToast("SCAN_FAILED_APPLICATION_REGISTRATION_FAILED");
                        break;
                    case SCAN_FAILED_FEATURE_UNSUPPORTED:
                        //logErrorAndShowToast("SCAN_FAILED_FEATURE_UNSUPPORTED");
                        break;
                    case SCAN_FAILED_INTERNAL_ERROR:
                        //logErrorAndShowToast("SCAN_FAILED_INTERNAL_ERROR");
                        break;
                    default:
                        //logErrorAndShowToast("Scan failed, unknown error code");
                        break;
                }
            }

        };

        scanner.startScan(scanFilters, SCAN_SETTINGS, scanCallback);

        SystemClock.sleep(10000);
        Log.d(TAG, "Stop BtScan");
        scanner.stopScan(scanCallback);


        //Send Eddystone ScanLog to OpenLobster
        //Prepare Json
        JSONObject ScanLogParent;
        JSONArray ScanLogArray;

        ScanLogParent = new JSONObject();
        ScanLogArray = new JSONArray();



        try {

            int     no_of_valid_eddystone = 0;
            if (no_of_old_eddystone > 0) {
                for (Map.Entry<String, Beacon> current : deviceToBeaconMap.entrySet()) {
                    if (current.getValue().hasUidFrame) {
                        no_of_valid_eddystone++;

                        JSONObject jsonObj = new JSONObject();

                        jsonObj.put("UID", current.getValue().uidStatus.uidValue);
                        jsonObj.put("RSSI", current.getValue().rssi);
                        jsonObj.put("BName", "TrailBlazer");
                        jsonObj.put("BMac", current.getKey().replace(":", "").toUpperCase());
                        ScanLogArray.put(jsonObj);
                    }

                }
            }

            ScanLogParent.accumulate("OLBSAPIKey", "6375686B2E6564752E686B2E30303031");
            ScanLogParent.accumulate("SMode", 1);
            ScanLogParent.accumulate("UserToken", Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID));
            ScanLogParent.accumulate("DeviceID", Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID));
            ScanLogParent.accumulate("DeviceOS", 1);
            ScanLogParent.accumulate("Longitude", mCurrentLocation == null?-999:mCurrentLocation.getLongitude());
            ScanLogParent.accumulate("Latitude", mCurrentLocation == null?-999:mCurrentLocation.getLatitude());
            ScanLogParent.accumulate("Altitude", mCurrentLocation == null?-999:mCurrentLocation.getAltitude());
            ScanLogParent.accumulate("NumEddystone", no_of_valid_eddystone);
            ScanLogParent.accumulate("LocLabel", "");

            ScanLogParent.put("ScanDetail", ScanLogArray);

            if (no_of_valid_eddystone>0)
                EddystoneScanLog(ScanLogParent.toString());

        }
        catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }


        String notifmsg = "";

        if (no_of_beacon == 0 && no_of_eddystone == 0)
            notifmsg = "努力啊! 沒有 iBeacon 或 Eddystone 在附近.\n";
        if (no_of_new_beacon > 0)
            notifmsg = notifmsg + "找到" + no_of_new_beacon + "個新的 iBeacon.\n";
        if (no_of_new_eddystone > 0)
            notifmsg = notifmsg + "找到" + no_of_new_eddystone + "個新的 Eddystone.\n";
        if (no_of_old_beacon > 0)
            notifmsg = notifmsg + "再遇見已捕獲的 iBeacon.\n";
        if (no_of_old_eddystone > 0)
            notifmsg = notifmsg + "再遇見已捕獲的 Eddystone.\n";
        sendNotification(notifmsg);

        // Release the wake lock provided by the BroadcastReceiver.
        AlarmReceiver.completeWakefulIntent(intent);

        //send out scan status
        broadcastIntent = new Intent();
        broadcastIntent.setAction(ResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(PARAM_OUT_MSG, "完成搜尋\n" + notifmsg + "等待下次的定時搜尋.");
        sendBroadcast(broadcastIntent);

        Log.d(TAG, "Service Stopping!");
        this.stopSelf();

    }

    @Override
    public void OLBSonEventNotify(OLBSEventType event, OLBSLocation olbs_location) {

        switch (event) {

            case OLBS_GPS:

                mCurrentLocation = olbs_location.gps_loc;
                break;

            case OLBS_SENSOR:
                final float currentAzimuth = (int) (olbs_location.sensor_event.values[0]) % 360;


                break;


            case OLBS_NO_BEACON_IN_REGION:
                no_beacon_detect_count--;

                if (no_beacon_detect_count ==0)
                {
                    synchronized(lock) {

                        lock.notify();
                    }

                }
                break;

            case OLBS_ALL_BEACON_IN_REGION:


                final Object[] beaconArrayList = olbs_location.beacons.toArray();



                isExternalStorageWritable();

                String filename = "ol_scanlog.txt";

                String path = getExternalFilesDir(null).getAbsolutePath();

                String file_string = path + "/ol_scanlog.txt";



                PrintWriter fileWriter = null;

                try {

//                    fileWriter = new PrintWriter(new BufferedWriter(new FileWriter(
//                            file_string, true)));


                    //no_of_beacon = beaconArrayList.length;
                    no_of_beacon = 0;

                    for (int i = 0; i < beaconArrayList.length; i++) {
                        currentUuid = ((OLBSBeacon) beaconArrayList[i]).UUID;
                        currentMajor = Integer.parseInt(((OLBSBeacon) beaconArrayList[i]).major_id);
                        currentMinor = Integer.parseInt(((OLBSBeacon) beaconArrayList[i]).minor_id);

                        //String write_string = DateFormat.getDateInstance().format(new Date()) + " " +  DateFormat.getTimeInstance().format(new Date()) + ":" + currentUuid + ":" + currentMajor + ":" + currentMinor;

                        //fileWriter.println(write_string);

                        no_of_beacon++;
                        if (scandb.checkNewiBeacon (currentUuid, currentMajor, currentMinor)) {
                            no_of_new_beacon++;
                        } else {
                            no_of_old_beacon++;
                        }

                    }
//                    if (fileWriter != null)
//                    {
//                        fileWriter.close();
//                    }

                }
                catch(Exception e)
                  {
                      e.printStackTrace();
                    }








                synchronized(lock) {
                    Log.d(TAG, "ALL Beacon Scan Lock Release");
                    lock.notify();
                }
//
               break;

            case OLBS_ESTIMATE_LOCATION:
                synchronized(lock) {
                    Log.d(TAG, "Estimate Location Lock Release");
                    lock.notify();
                }
                break;
        }
    }

    // Post a notification indicating whether a doodle was found.
    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
               this.getSystemService(Context.NOTIFICATION_SERVICE);
    
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
            new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
        .setSmallIcon(R.mipmap.ic_ol_launcher)
        //.setContentTitle(getString(R.string.doodle_alert))
                        .setContentTitle("TrailBlazer - 開拓者")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }



//    //Google Eddystone
//    private void validateServiceData(String deviceAddress, byte[] serviceData) {
//        Beacon beacon = deviceToBeaconMap.get(deviceAddress);
//        if (serviceData == null) {
//            String err = "Null Eddystone service data";
//            //beacon.frameStatus.nullServiceData = err;
//            //logDeviceError(deviceAddress, err);
//            return;
//        }
//        switch (serviceData[0]) {
//            case Constants.UID_FRAME_TYPE:
//                UidValidator.validate(deviceAddress, serviceData, beacon);
//                break;
//            case Constants.TLM_FRAME_TYPE:
//                TlmValidator.validate(deviceAddress, serviceData, beacon);
//                break;
//            case Constants.URL_FRAME_TYPE:
//                UrlValidator.validate(deviceAddress, serviceData, beacon);
//                break;
//            default:
//                String err = String.format("Invalid frame type byte %02X", serviceData[0]);
//                beacon.frameStatus.invalidFrameType = err;
//                //logDeviceError(deviceAddress, err);
//                break;
//        }
//        //arrayAdapter.notifyDataSetChanged();
//    }
//    //Google Eddystone

    protected void EddystoneScanLog(String beacon_json_string)
    {
        try
        {
            // String file_url = "";
            new HTTPPostJason().execute(beacon_json_string);
        }
        catch (Exception e) {
            Log.d(TAG, "[EddystoneScanLog]" + e.getMessage());
        }

    }



    // Async Task Class
    class HTTPPostJason extends AsyncTask<String, String, String> {

        // Show Progress bar before
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }


        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {

                // 1. create HttpClient
                HttpClient httpclient = new DefaultHttpClient();


                HttpParams params = httpclient.getParams();
                HttpConnectionParams.setConnectionTimeout(params, HttpConnectionTimeout);
                HttpConnectionParams.setSoTimeout(params, SocketConnectionTimeout);

                // 2. make POST request to the given URL
                HttpPost httpPost = new HttpPost(ScanLogUrl);

                String json = "";

                // 4. convert JSONObject to JSON to String
                //json = ScanLogParent.toString();
                json = f_url[0].toString();
//                ScanLogParent = new JSONObject();

                // 5. set json to StringEntity
                StringEntity se = new StringEntity(json);

                // 6. set httpPost Entity


                // 7. Set some headers to inform server about the type of the content
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                httpPost.setEntity(se);

                // 8. Execute POST request to the given URL
                HttpResponse httpResponse = httpclient.execute(httpPost);



                // 9. receive response as inputStream
                BufferedReader br = new BufferedReader(
                        new InputStreamReader((httpResponse.getEntity().getContent())));

                String output;


                while ((output = br.readLine()) != null) {
                    JSONObject obj = new JSONObject(output);

//                    olbs_location.indoor_pos.last_floor_map_uuid =  olbs_location.indoor_pos.floor_map_uuid;
//
//                    olbs_location.indoor_pos.luuid = obj.getString("LUUID");
//                    olbs_location.indoor_pos.organization = obj.getString("Organization");
//                    olbs_location.indoor_pos.building = obj.getString("Building");
//                    olbs_location.indoor_pos.floor_seq = Integer.parseInt(obj.getString("FloorSeq"));
//                    olbs_location.indoor_pos.floor_label = obj.getString("FloorLabel");
//                    olbs_location.indoor_pos.room = obj.getString("Room");
//                    olbs_location.indoor_pos.floor_map_uuid= obj.getString("FMapUUID");
//                    olbs_location.indoor_pos.ux = Float.parseFloat(obj.getString("ux"));
//                    olbs_location.indoor_pos.uy = Float.parseFloat(obj.getString("uy"));
//                    ole.OLBSonEventNotify(OLBSEventType.OLBS_ESTIMATE_LOCATION, olbs_location);

                    Log.d("[ScanLog] " + "Eddystone: ", output);
                }



            } catch (Exception e) {
                Log.d(TAG, "[ScanLog]" + "[doInBackground]" + e.getMessage());
                //olbs_location.indoor_pos.luuid = null;
//                olbs_location.error = e;
//                ole.OLBSonEventNotify(OLBSEventType.OLBS_ESTIMATE_LOCATION, olbs_location);
            }
            return null;
        }

        // While Progressing
        protected void onProgressUpdate(String... progress) {
            // Set progress percentage

        }

        // Once findished
        @Override
        protected void onPostExecute(String file_url) {
           //

        }
    }


}
