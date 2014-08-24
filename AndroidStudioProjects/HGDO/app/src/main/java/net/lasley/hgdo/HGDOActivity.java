package net.lasley.hgdo;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import net.lasley.hgdo.GeofenceUtils.REMOVE_TYPE;
import net.lasley.hgdo.GeofenceUtils.REQUEST_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HGDOActivity
        extends FragmentActivity
        implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener,
                   LocationListener {

  private final static int    CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
  private static final long   GEOFENCE_EXPIRATION_IN_MILLISECONDS   = Geofence.NEVER_EXPIRE;
  //    private GoogleMap map;
  private static final int    GARAGE_PORT                           = 55555;
  private static final String SERVER_HOSTNAME                       =
          hgdoApp.getAppContext().getString(R.string.server_hostname);

  private static final int TIME_FOR_DOOR_TO_OPEN         = 16;  // Seconds
  private static final int TIME_TO_WAIT_FOR_DOOR_TO_OPEN = (int) (TIME_FOR_DOOR_TO_OPEN * 1000);
  // Milliseconds

  private static final int TIME_TO_WAIT_WIFI = 16 * 1000;  // Milliseconds

  private static final int  LENGTH_V1_NDX            = 0;
  private static final int  VERSION_V1_NDX           = 1;
  private static final int  MSG_VERSION              = 0x01;
  private static final int  ACTION_V1_NDX            = 2;
  private static final int  COMMAND_V1_NDX           = 3;
  private static final int  COMMAND_REPLY_V1_NDX     = 3;
  private static final int  STR_START_V1_NDX         = 3;
  private static final int  STATUS_DOOR_V1_NDX       = 3;
  private static final int  STATUS_LIGHT_V1_NDX      = 4;
  private static final int  STATUS_RANGE_V1_NDX      = 5;
  // Version 1 Lengths
  private static final int  COMMAND_LENGTH_V1        = 8;
  private static final int  COMMAND_REPLY_LENGTH_V1  = 8;
  private static final int  STATUS_REQUEST_LENGTH_V1 = 7;
  private static final int  STATUS_REPLY_LENGTH_V1   = 10;
  private static final byte VERSION                  = 1;
  private static final byte STRING                   = 16;
  private static final byte COMMAND                  = 10;
  private static final byte COMMANDREPLY             = 45;
  private static final byte STATUSREQ                = 21;
  private static final byte STATUSREPLY              = 33;
  private static final byte DOOR_CLOSED              = 0;
  private static final byte DOOR_OPEN                = 1;
  private static final byte DOOR_OPENING             = 2;
  private static final byte DOOR_CLOSING             = 3;
  private static final byte DOOR_BUSY                = 4;
  private static final byte LIGHT_OFF                = 0;
  private static final byte LIGHT_ON                 = 1;
  private static final byte OPEN_DOOR                = 0;
  private static final byte CLOSE_DOOR               = 1;
  private static final byte TOGGLE_DOOR              = 2;
  // Store a list of geofences to add
  private List<Geofence>           m_CurrentGeofences;
  private List<SimpleGeofence>     m_SimpleGeofence;
  private WifiManager              m_Wifi;
  // Define an object that holds accuracy and frequency parameters
  private LocationRequest          m_LocationRequest;
  // Store the current request
  private REQUEST_TYPE             m_RequestType;
  // Store the current type of removal
  private REMOVE_TYPE              m_RemoveType;
  // Add handlers
  private GeofenceRequester        m_GeofenceRequester;
  private GeofenceRemover          m_GeofenceRemover;
  private GeofenceSampleReceiver   m_GeofenceReceiver;
  private LocationClient           m_LocationClient;
  private WIFIReceiver             m_WIFIReceiver;
  private ArrayAdapter<String>     m_Adapter;
  private ToggleDoorCountDownTimer m_CountDownTimer;
  private WiFiCountDownTimer       m_wifiTimer;
  private Fences                   m_LastFence;
  private ProgressBar              m_DoorProgressBar;
  private ProgressBar              m_CommProgressBar;
  private int                      m_OrigWiFiSettingOn;
  private boolean                  m_ReadyToMonitorWifi;

  @Override
  protected void onDestroy() {
    RemoveGeoFencing();
    LocalBroadcastManager.getInstance(this).unregisterReceiver(m_GeofenceReceiver);
    unregisterReceiver(m_WIFIReceiver);
    if (m_OrigWiFiSettingOn == WifiManager.WIFI_STATE_ENABLED) {
      m_Wifi.setWifiEnabled(true);
    } else {
      m_Wifi.setWifiEnabled(false);
    }
    Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "onDestroy()");
    super.onDestroy();
  }

  void RemoveGeoFencing() {
    m_RemoveType = GeofenceUtils.REMOVE_TYPE.INTENT;
    if (!servicesConnected()) {
      return;
    }

    try {
      m_GeofenceRemover.removeGeofencesByIntent(m_GeofenceRequester.getRequestPendingIntent());
    } catch (UnsupportedOperationException e) {
      Toast.makeText(this, net.lasley.hgdo.R.string.remove_geofences_already_requested_error, Toast.LENGTH_LONG).show();
    }
  }

  private boolean servicesConnected() {
    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    if (ConnectionResult.SUCCESS == resultCode) {
      return true;
    } else {
      Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
      if (dialog != null) {
        ErrorDialogFragment errorFragment = new ErrorDialogFragment();
        errorFragment.setDialog(dialog);
        errorFragment.show(getSupportFragmentManager(), hgdoApp.getAppContext().getString(R.string.app_name));
      }
      return false;
    }
  }

  @Override
  public void onConnected(Bundle dataBundle) {
    Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    CheckBox cb = (CheckBox) findViewById(R.id.checkGPS);
    if (cb.isChecked()) {
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "onResume - AddGeoFencing.");
      m_LocationClient.requestLocationUpdates(m_LocationRequest, this);
      AddGeoFencing();
    }
  }

  void AddGeoFencing() {
    m_RequestType = GeofenceUtils.REQUEST_TYPE.ADD;
    if (!servicesConnected()) {
      return;
    }

    m_SimpleGeofence.clear();
    m_SimpleGeofence.add(
            new SimpleGeofence("LOWER_APPROACH", 38.931323d, -104.726197d, 30.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS,
                               Geofence.GEOFENCE_TRANSITION_EXIT));
    // 1/3 Lower Entry
    m_SimpleGeofence.add(
            new SimpleGeofence("LOWER_ENTRY", 38.930366d, -104.726935d, 20.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS,
                               Geofence.GEOFENCE_TRANSITION_EXIT));
    // 1/2 Lower Entry
    //m_SimpleGeofence.add(new SimpleGeofence("LOWER_ENTRY",    38.930106d,  -104.72671d, 20.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT));
    m_SimpleGeofence.add(
            new SimpleGeofence("UPPER_APPROACH", 38.932177d, -104.724792d, 20.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS,
                               Geofence.GEOFENCE_TRANSITION_EXIT)); //
    // 1/3 Upper Entry
    m_SimpleGeofence.add(
            new SimpleGeofence("UPPER_ENTRY", 38.931571d, -104.724192d, 30.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS,
                               Geofence.GEOFENCE_TRANSITION_EXIT));
    // 1/2 Upper Entry
    //m_SimpleGeofence.add(new SimpleGeofence("UPPER_ENTRY",    38.931185d,  -104.724138d,  30.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT));
    m_SimpleGeofence.add(
            new SimpleGeofence("DRIVEWAY", 38.930474d, -104.725154d, 10.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS,
                               Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_ENTER));


    m_CurrentGeofences.clear();
    for (SimpleGeofence element : m_SimpleGeofence) {
      m_CurrentGeofences.add(element.toGeofence());
    }

    try {
      m_GeofenceRequester.addGeofences(m_CurrentGeofences);
    } catch (UnsupportedOperationException e) {
      Toast.makeText(this, net.lasley.hgdo.R.string.add_geofences_already_requested_error, Toast.LENGTH_LONG).show();
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
  }

  @Override
  public void onDisconnected() {
    Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    if (connectionResult.hasResolution()) {
      try {
        connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
      } catch (IntentSender.SendIntentException e) {
        e.printStackTrace();
      }
    } else {
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name),
            "ErrorCode: " + Integer.toString(connectionResult.getErrorCode()));
    }
  }

  // Define the callback method that receives location updates
  @Override
  public void onLocationChanged(Location location) {
    if (servicesConnected()) {
      ((TextView) (findViewById(net.lasley.hgdo.R.id.lat_lng))).setText(GeofenceUtils.getLatLng(this, location));
      (new GetAddressTask(this)).execute(location);
      float meter = location.getAccuracy();
      if (meter > 13.0) {
        ((TextView) (findViewById(net.lasley.hgdo.R.id.accuracy))).setTextColor(Color.RED);
      } else if (meter > 10.0) {
        ((TextView) (findViewById(net.lasley.hgdo.R.id.accuracy))).setTextColor(Color.YELLOW);
      } else {
        ((TextView) (findViewById(net.lasley.hgdo.R.id.accuracy))).setTextColor(Color.GREEN);
        //                TypedArray themeArray = this.getTheme().obtainStyledAttributes(new int[] {android.R.attr.textColor});
        //                int index = 0;
        //                int defaultColorValue = 0;
        //                int TextColor = themeArray.getColor(index, defaultColorValue);
        //                ((TextView) (findViewById(net.lasley.hgdo.R.id.accuracy))).setTextColor(TextColor);
      }
      float feet = meter * 3.2808f;
      String feetstr = new DecimalFormat("0.0").format(feet);
      ((TextView) (findViewById(net.lasley.hgdo.R.id.accuracy))).setText(feetstr + " ft.");
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      //      startTime = (Calendar) bundle.getSerializable("starttime");
    }

    LocationRequest mLocationRequestSlow = LocationRequest.create();
    mLocationRequestSlow.setPriority(LocationRequest.PRIORITY_NO_POWER);
    mLocationRequestSlow.setInterval(60 * 1000); // Milliseconds
    mLocationRequestSlow.setFastestInterval(30 * 1000);

    m_LocationRequest = LocationRequest.create();
    m_LocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    m_LocationRequest.setInterval(5 * 1000); // Milliseconds
    m_LocationRequest.setFastestInterval(1 * 1000);

    m_LastFence = Fences.UNKNOWN;

    m_SimpleGeofence = new ArrayList<SimpleGeofence>();
    m_CurrentGeofences = new ArrayList<Geofence>();
    List<String> mAreaVisits = new ArrayList<String>();

    m_LocationClient = new LocationClient(this, this, this);
    m_LocationClient.connect();

    m_GeofenceReceiver = new GeofenceSampleReceiver();
    m_GeofenceRequester = new GeofenceRequester(this);
    m_GeofenceRemover = new GeofenceRemover(this);
    m_WIFIReceiver = new WIFIReceiver();
    m_Wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    m_OrigWiFiSettingOn = m_Wifi.getWifiState();
    m_ReadyToMonitorWifi = m_OrigWiFiSettingOn == WifiManager.WIFI_STATE_ENABLED;
    //        m_Wifi.setWifiEnabled(false);

    IntentFilter mIntentWIFIFilter = new IntentFilter();
    mIntentWIFIFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    //        mIntentWIFIFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
    //        mIntentWIFIFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
    //        mIntentWIFIFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
    //        mIntentWIFIFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    //        mIntentWIFIFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
    registerReceiver(m_WIFIReceiver, mIntentWIFIFilter);

    IntentFilter mIntentFilter = new IntentFilter();
    mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);
    mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);
    mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);
    mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ENTER);
    mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_EXIT);
    // All Location Services sample apps use this category
    mIntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);
    LocalBroadcastManager.getInstance(this).registerReceiver(m_GeofenceReceiver, mIntentFilter);

    m_CountDownTimer =
            new ToggleDoorCountDownTimer(TIME_TO_WAIT_FOR_DOOR_TO_OPEN, (int) (TIME_TO_WAIT_FOR_DOOR_TO_OPEN / 100.0));
    m_wifiTimer = new WiFiCountDownTimer(TIME_TO_WAIT_WIFI, (long) (TIME_TO_WAIT_WIFI / 100.0));

    // Attach to the main UI
    setContentView(R.layout.activity_hgdo);

    ListView list = (ListView) findViewById(R.id.Activity);
    m_Adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mAreaVisits);
    list.setAdapter(m_Adapter);

    m_DoorProgressBar = (ProgressBar) findViewById(R.id.WaitForDoor);
    m_CommProgressBar = (ProgressBar) findViewById(R.id.WaitForComm);

/*
        map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setMyLocationEnabled(true);
*/
    sendStatusRequest();
  }

  void sendStatusRequest() {
    byte[] msg = new byte[7];
    msg[LENGTH_V1_NDX] = STATUS_REQUEST_LENGTH_V1;
    msg[VERSION_V1_NDX] = VERSION;
    msg[ACTION_V1_NDX] = STATUSREQ;
    msg[STATUS_REQUEST_LENGTH_V1 - 4] = 1;
    msg[STATUS_REQUEST_LENGTH_V1 - 3] = 2;
    msg[STATUS_REQUEST_LENGTH_V1 - 2] = 3;
    msg[STATUS_REQUEST_LENGTH_V1 - 1] = 4;
    new AsyncGarage().execute(msg);
  }

  private boolean isConnectedViaWifi() {
    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    return mWifi.isConnected();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    switch (requestCode) {
      case GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST:
        switch (resultCode) {
          case Activity.RESULT_OK:
            if (GeofenceUtils.REQUEST_TYPE.ADD == m_RequestType) {
              m_GeofenceRequester.setInProgressFlag(false);
              m_GeofenceRequester.addGeofences(m_CurrentGeofences);
            } else if (GeofenceUtils.REQUEST_TYPE.REMOVE == m_RequestType) {
              m_GeofenceRemover.setInProgressFlag(false);
              if (GeofenceUtils.REMOVE_TYPE.INTENT == m_RemoveType) {
                m_GeofenceRemover.removeGeofencesByIntent(m_GeofenceRequester.getRequestPendingIntent());
              }
            }
            break;
          default:
            Log.d(hgdoApp.getAppContext().getString(R.string.app_name), getString(net.lasley.hgdo.R.string.no_resolution));
        }
      default:
        Log.d(hgdoApp.getAppContext().getString(R.string.app_name),
              getString(net.lasley.hgdo.R.string.unknown_activity_request_code, requestCode));
        break;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Register the broadcast receiver to receive status updates
    Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "OnResume");
    //        CheckBox cb = (CheckBox)findViewById(R.id.checkGPS);
    //        if(cb.isChecked()) {
    //            Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "onResume - AddGeoFencing.");
    //            AddGeoFencing();
    //        }
    //        LocalBroadcastManager.getInstance(this).registerReceiver(m_GeofenceReceiver, mIntentFilter);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "OnPause");
    //        CheckBox cb = (CheckBox)findViewById(R.id.checkGPS);
    //        if(cb.isChecked()) {
    //            Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "onPause - RemoveGeoFencing.");
    //            RemoveGeoFencing();
    //        }
    //        LocalBroadcastManager.getInstance(this).unregisterReceiver(m_GeofenceReceiver);
  }

  void decodeReply(byte[] reply) {
    StringBuilder sb = new StringBuilder();
    sb.append("Length: ");
    sb.append(Byte.toString(reply[LENGTH_V1_NDX]));
    Log.d("decodeReply", sb.toString());
    sb = new StringBuilder("Version: ");
    sb.append(Byte.toString(reply[VERSION_V1_NDX]));
    Log.d("decodeReply", sb.toString());
    sb = new StringBuilder("Action: ");
    if (reply[ACTION_V1_NDX] == COMMANDREPLY) {
      sb.append("CommandReply");
      Log.d("decodeReply", sb.toString());
      TextView t = (TextView) findViewById(R.id.DoorStatus);
      sb = new StringBuilder("Action: ");
      if (reply[COMMAND_REPLY_V1_NDX] == DOOR_OPENING) {
        sb.append("Door Opening");
        t.setText("Opening");
      } else if (reply[COMMAND_REPLY_V1_NDX] == DOOR_CLOSING) {
        sb.append("Door Closing");
        t.setText("Closing");
      } else if (reply[COMMAND_REPLY_V1_NDX] == DOOR_BUSY) {
        sb.append("Door Busy");
        t.setText("Busy");
      }
      Log.d("decodeReply", sb.toString());
    } else if (reply[ACTION_V1_NDX] == STATUSREPLY) {
      Calendar rightNow = Calendar.getInstance();
      //java.text.SimpleDateFormat
      SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yy hh:mm:ss", Locale.US);
      String date = sdf.format(rightNow.getTime());
      TextView t = (TextView) findViewById(R.id.TimeChecked);
      t.setText("Refreshed: " + date);

      sb.append("StatusReply");
      Log.d("decodeReply", sb.toString());
      sb = new StringBuilder("Door: ");
      sb.append(Byte.toString(reply[STATUS_DOOR_V1_NDX]));
      Log.d("decodeReply", sb.toString());
      t = (TextView) findViewById(R.id.DoorStatus);
      if (reply[STATUS_DOOR_V1_NDX] == DOOR_OPEN) {
        t.setText("Open");
      } else if (reply[STATUS_DOOR_V1_NDX] == DOOR_CLOSED) {
        t.setText("Closed");
      } else if (reply[STATUS_DOOR_V1_NDX] == DOOR_BUSY) {
        t.setText("Busy");
      }
      sb = new StringBuilder("Range: ");
      short s = (short) (reply[STATUS_RANGE_V1_NDX] & 0xFF);
      sb.append(s);
      Log.d("decodeReply", sb.toString());
      t = (TextView) findViewById(R.id.RangeStatus);
      t.setText(Byte.toString(reply[STATUS_RANGE_V1_NDX]));

      sb = new StringBuilder("Light: ");
      sb.append(Byte.toString(reply[STATUS_LIGHT_V1_NDX]));
      Log.d("decodeReply", sb.toString());
      t = (TextView) findViewById(R.id.LightStatus);
      if (reply[STATUS_LIGHT_V1_NDX] == LIGHT_ON) {
        t.setText("On");
      } else if (reply[STATUS_LIGHT_V1_NDX] == LIGHT_OFF) {
        t.setText("Off");
      }
    }
  }

  public void RefreshState(View view) {
    sendStatusRequest();
  }

  public void toggleDoor(View view) {
    byte[] msg = new byte[8];
    msg[LENGTH_V1_NDX] = COMMAND_LENGTH_V1;
    msg[VERSION_V1_NDX] = VERSION;
    msg[ACTION_V1_NDX] = COMMAND;
    msg[COMMAND_V1_NDX] = TOGGLE_DOOR;
    msg[COMMAND_LENGTH_V1 - 4] = 1;
    msg[COMMAND_LENGTH_V1 - 3] = 2;
    msg[COMMAND_LENGTH_V1 - 2] = 3;
    msg[COMMAND_LENGTH_V1 - 1] = 4;
    new AsyncGarage().execute(msg);
    m_CountDownTimer.start();
  }

  public void SetWIFIState(View view) {
    CheckBox cb = (CheckBox) findViewById(R.id.checkWIFI);
    if (cb.isChecked()) {
      if (!m_ReadyToMonitorWifi) {
        m_Wifi.setWifiEnabled(true);
        m_wifiTimer.start();
      }
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "SetWIFIState - Checked.");
    } else {
      m_ReadyToMonitorWifi = false;
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "SetWIFIState - Unchecked.");
      //            m_Wifi.setWifiEnabled(false);
    }
  }

  public void SetGPSState(View view) {
    CheckBox cb = (CheckBox) findViewById(R.id.checkGPS);
    if (cb.isChecked()) {
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "SetGPSState - Checked.");
      m_LocationClient.requestLocationUpdates(m_LocationRequest, this);
      getLocation();
      AddGeoFencing();
    } else {
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "SetGPSState - Unchecked.");
      m_LocationClient.removeLocationUpdates(this);
      //            m_LocationClient.requestLocationUpdates(mLocationRequestSlow, this);
      ((TextView) findViewById(R.id.lat_lng)).setText("Indeterminate");
      ((TextView) findViewById(R.id.fencearea)).setText("Indeterminate");

      TypedArray themeArray = this.getTheme().obtainStyledAttributes(new int[] {android.R.attr.textColorPrimary});
      int index = 0;
      int defaultColorValue = 0;
      int TextColor = themeArray.getColor(index, defaultColorValue);
      ((TextView) (findViewById(net.lasley.hgdo.R.id.accuracy))).setTextColor(TextColor);
      ((TextView) findViewById(R.id.accuracy)).setText("Indeterminate");
      m_Adapter.clear();
      RemoveGeoFencing();
    }
  }

  void getLocation() {
    if (servicesConnected()) {
      Location currentLocation = m_LocationClient.getLastLocation();
      ((TextView) (findViewById(net.lasley.hgdo.R.id.lat_lng))).setText(GeofenceUtils.getLatLng(this, currentLocation));
      (new GetAddressTask(this)).execute(currentLocation);
      String feetstr = new DecimalFormat("0.0").format(currentLocation.getAccuracy() * 3.2808);
      ((TextView) (findViewById(net.lasley.hgdo.R.id.accuracy))).setText(feetstr + " ft.");
    }
  }

  private enum Fences {APPROACH, ENTRY, DRIVEWAY, UNKNOWN}

  public static class ErrorDialogFragment
          extends DialogFragment {
    private Dialog mDialog;

    public ErrorDialogFragment() {
      super();
      mDialog = null;
    }

    public void setDialog(Dialog dialog) {
      mDialog = dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      return mDialog;
    }
  }

  public class ToggleDoorCountDownTimer
          extends CountDownTimer {
    long st;

    public ToggleDoorCountDownTimer(long startTime, long interval) {
      super(startTime, interval);
      st = startTime;
    }

    @Override
    public void onFinish() {
      m_DoorProgressBar.setProgress(0);
      sendStatusRequest();
    }

    @Override
    public void onTick(long millisUntilFinished) {
      m_DoorProgressBar.setProgress((int) ((float) (st - millisUntilFinished) * 100. / (float) st));
    }
  }

  public class WiFiCountDownTimer
          extends CountDownTimer {
    long st;

    public WiFiCountDownTimer(long startTime, long interval) {
      super(startTime, interval);
      st = startTime;
    }

    @Override
    public void onTick(long millisUntilFinished) {
      m_DoorProgressBar.setSecondaryProgress((int) ((float) (st - millisUntilFinished) * 100. / (float) st));
    }

    @Override
    public void onFinish() {
      m_DoorProgressBar.setSecondaryProgress(0);
      m_ReadyToMonitorWifi = true;
    }
  }

  private class AsyncGarage
          extends AsyncTask<byte[], Void, byte[]> {

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      m_CommProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected byte[] doInBackground(byte[]... outbuffers) {
      Socket nsocket = new Socket();   //Network Socket
      byte[] tempdata = new byte[20];

      try {
        ConnectivityManager cm =
                (ConnectivityManager) hgdoApp.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        while (!ni.isConnected()) {
          ni = cm.getActiveNetworkInfo();
        }
        Log.i("SendDataToNetwork", "Creating socket");
        SocketAddress sockaddr = new InetSocketAddress(SERVER_HOSTNAME, GARAGE_PORT);
        nsocket = new Socket();
        nsocket.connect(sockaddr, 5000); //10 second connection timeout
        if (nsocket.isConnected()) {
          InputStream nis = nsocket.getInputStream(); //Network Input Stream
          OutputStream nos = nsocket.getOutputStream(); //Network Output Stream

          nos.write(outbuffers[0]);

          byte[] buffer = new byte[4096];
          int read = nis.read(buffer, 0, 4096); //This is blocking
          tempdata = new byte[read];
          System.arraycopy(buffer, 0, tempdata, 0, read);
        }
      } catch (IOException e) {
        e.printStackTrace();
        Log.i("SendDataToNetwork", "IOException");
      } catch (Exception e) {
        e.printStackTrace();
        Log.i("SendDataToNetwork", "Exception");
      } finally {
        try {
          nsocket.close();
        } catch (IOException e) {
          e.printStackTrace();
        } catch (Exception e) {
          e.printStackTrace();
        }
        Log.i("SendDataToNetwork", "Finished");
      }
      return tempdata;
    }

    @Override
    protected void onPostExecute(byte[] result) {
      super.onPostExecute(result);
      m_CommProgressBar.setVisibility(View.INVISIBLE);
      decodeReply(result);
    }

  }

  public class GeofenceSampleReceiver
          extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ERROR)) {
        handleGeofenceError(context, intent);
      } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_ADDED) ||
                 TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_REMOVED)) {
        handleGeofenceStatus(intent);
      } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ENTER)) {
        handleGeofenceEnter(intent);
      } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_EXIT)) {
        handleGeofenceExit(intent);
      } else {
        Log.e(hgdoApp.getAppContext().getString(R.string.app_name),
              getString(net.lasley.hgdo.R.string.invalid_action_detail, action));
        Toast.makeText(context, net.lasley.hgdo.R.string.invalid_action, Toast.LENGTH_LONG).show();
      }
    }

    private void handleGeofenceStatus(Intent intent) {
      String action = String.format("Status() %s", intent.getAction());
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name), action);
    }

    private void handleGeofenceEnter(Intent intent) {
      String action = String.format("Enter() %s", intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS));
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name), action);
      getLocation();
    }

    private void handleGeofenceExit(Intent intent) {
      String action = String.format("Exit() %s", intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS));
      Time tmp = new Time();
      tmp.setToNow();
      String msg = tmp.format("%T ") + ": " + intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
      m_Adapter.insert(msg, 0);
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name), action);
      getLocation();
      if (action.contains("APPROACH")) {
        sendStatusRequest();
        m_LastFence = Fences.APPROACH;
      } else if (action.contains("ENTRY")) {
        if (m_LastFence == Fences.APPROACH) {
          TextView t = (TextView) findViewById(R.id.DoorStatus);
          String state = t.getText().toString();
          if (state.contains("Closed")) {
            tmp.setToNow();
            msg = tmp.format("%T ") + ": Opening Garage Door Now";
            toggleDoor(null);
            m_Adapter.insert(msg, 0);
            Log.d(hgdoApp.getAppContext().getString(R.string.app_name), msg);
          }
        }
        m_LastFence = Fences.ENTRY;
      } else if (action.contains("DRIVEWAY")) {
        m_LastFence = Fences.DRIVEWAY;
      } else {
        m_LastFence = Fences.UNKNOWN;
      }
    }

    private void handleGeofenceError(Context context, Intent intent) {
      String msg = intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
      Log.e(hgdoApp.getAppContext().getString(R.string.app_name), msg);
      Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }
  }

  private class WIFIReceiver
          extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name), action);
      if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        NetworkInfo ni = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (ni.isConnected()) {
          if (m_ReadyToMonitorWifi) {
            //do stuff
            WifiInfo wi = m_Wifi.getConnectionInfo();
            Log.d(hgdoApp.getAppContext().getString(R.string.app_name), wi.getSSID());
            String ssid = wi.getSSID().trim().replace("\"", "");
            if (ssid.equals("WHITESPRUCE") || ssid.equals("WHITESPRUCE2")) {
              CheckBox cb = (CheckBox) findViewById(R.id.checkWIFI);
              if (cb.isChecked()) {
                toggleDoor(null);
              }
            }
          }
        } else {
          // wifi connection was lost
        }
      }
    }
  }

  private class GetAddressTask
          extends AsyncTask<Location, Void, String> {
    Context mContext;

    public GetAddressTask(Context context) {
      super();
      mContext = context;
    }

    /**
     * Get a Geocoder instance, get the latitude and longitude
     * look up the address, and return it
     *
     * @return A string containing the address of the current
     * location, or an empty string if no address can be found,
     * or an error message
     * @params params One or more Location objects
     */
    @Override
    protected String doInBackground(Location... params) {
      Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
      // Get the current location from the input parameter list
      Location loc = params[0];
      // Create a list to contain the result address
      List<Address> addresses;
      try {
                /*
                 * Return 1 address.
                 */
        addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
      } catch (IOException e1) {
        Log.e("LocationSampleActivity", "IO Exception in getFromLocation()");
        e1.printStackTrace();
        return ("IO Exception trying to get address");
      } catch (IllegalArgumentException e2) {
        // Error message to post in the log
        String errorString = "Illegal arguments " +
                             Double.toString(loc.getLatitude()) +
                             " , " +
                             Double.toString(loc.getLongitude()) +
                             " passed to address service";
        Log.e("LocationSampleActivity", errorString);
        e2.printStackTrace();
        return errorString;
      }
      // If the reverse geocode returned an address
      if (addresses != null && addresses.size() > 0) {
        // Get the first address
        Address address = addresses.get(0);
                /*
                 * Format the first line of address (if available),
                 * city, and country name.
                 */
        // Return the text
        return String.format("%s, %s, %s",
                             // If there's a street address, add it
                             address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "",
                             // Locality is usually a city
                             address.getLocality(),
                             // The country of the address
                             address.getCountryName());
      } else {
        return "No address found";
      }
    }

    /**
     * A method that's called once doInBackground() completes. Turn
     * off the indeterminate activity indicator and set
     * the text of the UI element that shows the address. If the
     * lookup failed, display the error message.
     */
    @Override
    protected void onPostExecute(String address) {
      // Display the results of the lookup.
      ((TextView) (findViewById(R.id.fencearea))).setText(address);
    }
  }
}
