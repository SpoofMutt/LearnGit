package net.lasley.hgdo;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import android.widget.RelativeLayout;
import android.widget.Space;
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

  public static final  int  TIME_TO_WAIT_WIFI                     = 16 * 1000;  // Milliseconds
  private final static int  CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
  //    private GoogleMap map;
  private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS   = Geofence.NEVER_EXPIRE;
  private static final int  TIME_FOR_DOOR_TO_OPEN                 = 18;  // Seconds
  // Milliseconds
  private static final int  TIME_TO_WAIT_FOR_DOOR_TO_OPEN         = (int) (TIME_FOR_DOOR_TO_OPEN * 1000);
  public boolean isDebuggable;
  // Add handlers
  IntentFilter             m_IntentFilter;
  IntentFilter             m_IntentFilter2;
  SharedPreferences        mPrefs;
  SharedPreferences.Editor ed;
  // Store a list of geofences to add
  private List<Geofence>           m_CurrentGeofences;
  private List<SimpleGeofence>     m_SimpleGeofence;
  // Define an object that holds accuracy and frequency parameters
  private LocationRequest          m_LocationRequest;
  // Store the current request
  private REQUEST_TYPE             m_RequestType;
  // Store the current type of removal
  private REMOVE_TYPE              m_RemoveType;
  private GeofenceRequester        m_GeofenceRequester;
  private GeofenceRemover          m_GeofenceRemover;
  private GeofenceSampleReceiver   m_GeofenceReceiver;
  private ServiceReceiver          m_ServiceReceiver;
  private LocationClient           m_LocationClient;
  private ArrayAdapter<String>     m_Adapter;
  private ToggleDoorCountDownTimer m_CountDownTimer;
  private WiFiCountDownTimer       m_wifiTimer;
  private Fences                   m_LastFence;
  private ProgressBar              m_DoorProgressBar;
  private ProgressBar              m_CommProgressBar;
  private CheckBox                 cb_checkWIFI;
  private CheckBox                 cb_debugWIFI;
  private CheckBox                 cb_checkGPS;
  private RelativeLayout           rl_GPSLayout;
  private TextView                 tv_label_lat_lng;
  private TextView                 tv_lat_lng;
  private Space                    s_space_lat_lng;
  private TextView                 tv_label_fencearea;
  private TextView                 tv_fencearea;
  private Space                    s_space_area;
  private TextView                 tv_AccuracyLabel;
  private TextView                 tv_accuracy;

  @Override
  public void onConnected(Bundle dataBundle) {
    Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    if (cb_checkGPS.isChecked()) {
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "onResume - AddGeoFencing.");
      m_LocationClient.requestLocationUpdates(m_LocationRequest, this);
      AddGeoFencing();
    }
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
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    isDebuggable = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));

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
    ArrayList<String> mAreaVisits = new ArrayList<String>();

    m_GeofenceReceiver = new GeofenceSampleReceiver();
    m_GeofenceRequester = new GeofenceRequester(this);
    m_GeofenceRemover = new GeofenceRemover(this);
    m_ServiceReceiver = new ServiceReceiver();

    m_IntentFilter = new IntentFilter();
    m_IntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);
    m_IntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);
    m_IntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);
    m_IntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ENTER);
    m_IntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_EXIT);
    // All Location Services sample apps use this category
    m_IntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

    m_IntentFilter2 = new IntentFilter();
    m_IntentFilter2.addAction(HGDOService.SERVICE_COMM_ACTIVITY);
    m_IntentFilter2.addAction(HGDOService.SERVICE_COMM_DATA);
    m_IntentFilter2.addAction(HGDOService.SERVICE_COMM_INFO);
    m_IntentFilter2.addAction(HGDOService.SERVICE_COMM_STATE);
    m_IntentFilter2.addAction(HGDOService.SERVICE_START_DOOR_TIMER);
    m_IntentFilter2.addAction(HGDOService.SERVICE_WIFI_SELECTION);

    m_CountDownTimer =
            new ToggleDoorCountDownTimer(TIME_TO_WAIT_FOR_DOOR_TO_OPEN, (int) (TIME_TO_WAIT_FOR_DOOR_TO_OPEN / 100.0));
    m_wifiTimer = new WiFiCountDownTimer(TIME_TO_WAIT_WIFI, (long) (TIME_TO_WAIT_WIFI / 100.0));

    // Attach to the main UI
    setContentView(R.layout.activity_hgdo);

    cb_checkWIFI = (CheckBox) findViewById(R.id.checkWIFI);
    cb_debugWIFI = (CheckBox) findViewById(R.id.DebugWiFi);
    cb_checkGPS = (CheckBox) findViewById(R.id.checkGPS);
    rl_GPSLayout = (RelativeLayout) findViewById(R.id.GPSLayout);
    tv_label_lat_lng = (TextView) findViewById(R.id.label_lat_lng);
    tv_lat_lng = (TextView) findViewById(R.id.lat_lng);
    s_space_lat_lng = (Space) findViewById(R.id.space_lat_lng);
    tv_label_fencearea = (TextView) findViewById(R.id.label_fencearea);
    tv_fencearea = (TextView) findViewById(R.id.fencearea);
    s_space_area = (Space) findViewById(R.id.space_area);
    tv_AccuracyLabel = (TextView) findViewById(R.id.AccuracyLabel);
    tv_accuracy = (TextView) findViewById(R.id.accuracy);

    mPrefs = hgdoApp.getAppContext().getSharedPreferences(getString(R.string.PREFERENCES), MODE_PRIVATE);

    getSharedPrefs();
    SetWIFIState(null);
    startMyServices();

    if (isDebuggable) {
      cb_debugWIFI.setVisibility(View.VISIBLE);
      WifiManager Wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
      if (Wifi.isWifiEnabled()) {
        cb_debugWIFI.setChecked(true);
      } else {
        cb_debugWIFI.setChecked(false);
      }
    } else {
      cb_debugWIFI.setVisibility(View.GONE);
      cb_checkGPS.setVisibility(View.GONE);
      rl_GPSLayout.setVisibility(View.GONE);
      tv_label_lat_lng.setVisibility(View.GONE);
      tv_lat_lng.setVisibility(View.GONE);
      s_space_lat_lng.setVisibility(View.GONE);
      tv_label_fencearea.setVisibility(View.GONE);
      tv_fencearea.setVisibility(View.GONE);
      s_space_area.setVisibility(View.GONE);
      tv_AccuracyLabel.setVisibility(View.GONE);
      tv_accuracy.setVisibility(View.GONE);

    }

    ListView list = (ListView) findViewById(R.id.Activity);
    m_Adapter = new MyListAdapter(this, mAreaVisits);
    list.setAdapter(m_Adapter);

    m_DoorProgressBar = (ProgressBar) findViewById(R.id.WaitForDoor);
    m_CommProgressBar = (ProgressBar) findViewById(R.id.WaitForComm);

/*
        map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setMyLocationEnabled(true);
*/
    sendStatusRequest();
  }

  @Override
  protected void onStart() {
    super.onStart();
    startMyServices();
  }

  @Override
  protected void onDestroy() {
    RemoveGeoFencing();
    stopMyServices();
    Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "onDestroy()");
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
    getSharedPrefs();
  }

  @Override
  protected void onPause() {
    super.onPause();
    Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "OnPause");
    saveSharedPrefs();
  }

  private void startMyServices() {
    startService(new Intent(this, HGDOService.class));
    if (cb_checkGPS.isChecked()) {
      startLocationClient();
      LocalBroadcastManager.getInstance(this).registerReceiver(m_GeofenceReceiver, m_IntentFilter);
    }
    LocalBroadcastManager.getInstance(this).registerReceiver(m_ServiceReceiver, m_IntentFilter2);
  }

  private void stopMyServices() {
    if (cb_checkGPS.isChecked()) {
      stopLocationClient();
      LocalBroadcastManager.getInstance(this).unregisterReceiver(m_GeofenceReceiver);
    }
    LocalBroadcastManager.getInstance(this).unregisterReceiver(m_ServiceReceiver);
    stopService(new Intent(this, HGDOService.class));
  }

  public void startLocationClient() {
    if (m_LocationClient == null) {
      m_LocationClient = new LocationClient(this, this, this);
      m_LocationClient.connect();
    }
  }

  public void stopLocationClient() {
    if (m_LocationClient != null) {
      m_LocationClient.removeLocationUpdates(this);
      m_LocationClient.disconnect();
      m_LocationClient = null;
    }
  }

  public void getSharedPrefs() {
    cb_checkWIFI.setChecked(mPrefs.getBoolean("wifiState", false));
    cb_debugWIFI.setChecked(mPrefs.getBoolean("debugWifi", false));
    cb_checkGPS.setChecked(mPrefs.getBoolean("checkGPS", false));
  }

  public void saveSharedPrefs() {
    ed = mPrefs.edit();
    ed.putBoolean("wifiState", cb_checkWIFI.isChecked());
    ed.putBoolean("debugWifi", cb_debugWIFI.isChecked());
    ed.putBoolean("checkGPS", cb_checkGPS.isChecked());
    ed.commit();
  }

  public void sendStatusRequest() {
    Intent broadcastIntent = new Intent(this, HGDOService.class);
    broadcastIntent.setAction(HGDOService.SERVICE_COMMAND).putExtra(HGDOService.EXTRA_PARAM1, HGDOService.STATUSREQ);
    startService(broadcastIntent);
  }

  public void SetWIFIState(View view) {
    Intent broadcastIntent = new Intent(this, HGDOService.class);
    broadcastIntent.setAction(HGDOService.SERVICE_WIFI_SELECTION);
    if (cb_checkWIFI.isChecked()) {
      cb_debugWIFI.setChecked(true);
      broadcastIntent.putExtra(HGDOService.EXTRA_PARAM1, 1);
      if (!m_wifiTimer.busy) {
        m_wifiTimer.startTimer();
      }
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "SetWIFIState - Checked.");
    } else {
      broadcastIntent.putExtra(HGDOService.EXTRA_PARAM1, 0);
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "SetWIFIState - Unchecked.");
    }
    saveSharedPrefs();
    startService(broadcastIntent);
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

  public void toggleWiFi(View view) {
    WifiManager Wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    if (cb_debugWIFI.isChecked()) {
      Wifi.setWifiEnabled(true);
    } else {
      Wifi.setWifiEnabled(false);
    }
  }

  public void SetGPSState(View view) {
    if (cb_checkGPS.isChecked()) {
      LocalBroadcastManager.getInstance(this).registerReceiver(m_GeofenceReceiver, m_IntentFilter);
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name), "SetGPSState - Checked.");
      startLocationClient();
      while (m_LocationClient.isConnecting()) {
        try {
          Thread.sleep(500);
        } catch (Exception e) {
          // Do nothing
        }
      }
      if (m_LocationClient.isConnected()) {
        m_LocationClient.requestLocationUpdates(m_LocationRequest, this);
        getLocation();
        AddGeoFencing();
      }
    } else {
      LocalBroadcastManager.getInstance(this).unregisterReceiver(m_GeofenceReceiver);
      Log.d(this.getString(R.string.app_name), "SetGPSState - Unchecked.");
      stopLocationClient();
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

  void decodeReply(byte[] reply) {
    StringBuilder sb = new StringBuilder();
    sb.append("Length: ");
    sb.append(Byte.toString(reply[HGDOService.LENGTH_V1_NDX]));
    Log.d("decodeReply", sb.toString());
    sb = new StringBuilder("Version: ");
    sb.append(Byte.toString(reply[HGDOService.VERSION_V1_NDX]));
    Log.d("decodeReply", sb.toString());
    sb = new StringBuilder("Action: ");
    if (reply[HGDOService.ACTION_V1_NDX] == HGDOService.COMMANDREPLY) {
      sb.append("CommandReply");
      Log.d("decodeReply", sb.toString());
      TextView t = (TextView) findViewById(R.id.DoorStatus);
      sb = new StringBuilder("Action: ");
      if (reply[HGDOService.COMMAND_REPLY_V1_NDX] == HGDOService.DOOR_OPENING) {
        sb.append("Door Opening");
        t.setText("Opening");
      } else if (reply[HGDOService.COMMAND_REPLY_V1_NDX] == HGDOService.DOOR_CLOSING) {
        sb.append("Door Closing");
        t.setText("Closing");
      } else if (reply[HGDOService.COMMAND_REPLY_V1_NDX] == HGDOService.DOOR_BUSY) {
        sb.append("Door Busy");
        t.setText("Busy");
      }
      Log.d("decodeReply", sb.toString());
    } else if (reply[HGDOService.ACTION_V1_NDX] == HGDOService.STATUSREPLY) {
      Calendar rightNow = Calendar.getInstance();
      //java.text.SimpleDateFormat
      SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yy hh:mm:ss", Locale.US);
      String date = sdf.format(rightNow.getTime());
      TextView t = (TextView) findViewById(R.id.TimeChecked);
      t.setText("Refreshed: " + date);

      sb.append("StatusReply");
      Log.d("decodeReply", sb.toString());
      sb = new StringBuilder("Door: ");
      sb.append(Byte.toString(reply[HGDOService.STATUS_DOOR_V1_NDX]));
      Log.d("decodeReply", sb.toString());
      t = (TextView) findViewById(R.id.DoorStatus);
      if (reply[HGDOService.STATUS_DOOR_V1_NDX] == HGDOService.DOOR_OPEN) {
        t.setText("Open");
      } else if (reply[HGDOService.STATUS_DOOR_V1_NDX] == HGDOService.DOOR_CLOSED) {
        t.setText("Closed");
      } else if (reply[HGDOService.STATUS_DOOR_V1_NDX] == HGDOService.DOOR_BUSY) {
        t.setText("Busy");
      }
      sb = new StringBuilder("Range: ");
      int i = byteToUnsigned(reply[HGDOService.STATUS_RANGE_V1_NDX]);
      sb.append(i);
      Log.d("decodeReply", sb.toString());
      t = (TextView) findViewById(R.id.RangeStatus);
      t.setText(Integer.toString(i));

      sb = new StringBuilder("Light: ");
      sb.append(Byte.toString(reply[HGDOService.STATUS_LIGHT_V1_NDX]));
      Log.d("decodeReply", sb.toString());
      t = (TextView) findViewById(R.id.LightStatus);
      if (reply[HGDOService.STATUS_LIGHT_V1_NDX] == HGDOService.LIGHT_ON) {
        t.setText("On");
      } else if (reply[HGDOService.STATUS_LIGHT_V1_NDX] == HGDOService.LIGHT_OFF) {
        t.setText("Off");
      }
    }
  }

  public static int byteToUnsigned(byte b) {
    return b & 0xFF;
  }

  public void RefreshState(View view) {
    sendStatusRequest();
  }

  public void toggleDoor(View view) {
    Intent broadcastIntent = new Intent(this, HGDOService.class);
    broadcastIntent.setAction(HGDOService.SERVICE_COMMAND).putExtra(HGDOService.EXTRA_PARAM1, HGDOService.TOGGLE_DOOR);
    startService(broadcastIntent);
    m_CountDownTimer.start();
  }

  public void openDoor(View view) {
    Intent broadcastIntent = new Intent(this, HGDOService.class);
    broadcastIntent.setAction(HGDOService.SERVICE_COMMAND).putExtra(HGDOService.EXTRA_PARAM1, HGDOService.OPEN_DOOR);
    startService(broadcastIntent);
    m_CountDownTimer.start();
  }

  public void closeDoor(View view) {
    Intent broadcastIntent = new Intent(this, HGDOService.class);
    broadcastIntent.setAction(HGDOService.SERVICE_COMMAND).putExtra(HGDOService.EXTRA_PARAM1, HGDOService.CLOSE_DOOR);
    startService(broadcastIntent);
    m_CountDownTimer.start();
  }

  // Define the callback method that receives location updates
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
    public void onTick(long millisUntilFinished) {
      m_DoorProgressBar.setProgress((int) ((float) (st - millisUntilFinished) * 100. / (float) st));
    }

    @Override
    public void onFinish() {
      m_DoorProgressBar.setProgress(0);
      sendStatusRequest();
    }


  }

  public class WiFiCountDownTimer
          extends CountDownTimer {
    public boolean busy;
    long st;

    public WiFiCountDownTimer(long startTime, long interval) {
      super(startTime, interval);
      st = startTime;
      busy = false;
    }

    public void startTimer() {
      busy = true;
    }

    @Override
    public void onTick(long millisUntilFinished) {
      m_DoorProgressBar.setSecondaryProgress((int) ((float) (st - millisUntilFinished) * 100. / (float) st));
    }

    @Override
    public void onFinish() {
      busy = false;
      m_DoorProgressBar.setSecondaryProgress(0);
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

  private class ServiceReceiver
          extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      Log.d(hgdoApp.getAppContext().getString(R.string.app_name), action);
      if (action.equals(HGDOService.SERVICE_COMM_STATE)) {
        int state = intent.getIntExtra(HGDOService.EXTRA_PARAM1, -1);
        if (state == View.INVISIBLE) {
          m_CommProgressBar.setVisibility(View.INVISIBLE);
        } else if (state == View.VISIBLE) {
          m_CommProgressBar.setVisibility(View.VISIBLE);
        }
      } else if (action.equals(HGDOService.SERVICE_COMM_INFO)) {
        boolean wifi = intent.getBooleanExtra(HGDOService.SERVICE_WIFI_STATE, false);
        cb_checkWIFI.setChecked(wifi);
      } else if (action.equals(HGDOService.SERVICE_COMM_DATA)) {
        byte[] data = intent.getByteArrayExtra(HGDOService.EXTRA_PARAM1);
        decodeReply(data);
      } else if (action.equals(HGDOService.SERVICE_COMM_ACTIVITY)) {
        String msg = intent.getStringExtra(HGDOService.EXTRA_PARAM1);
        Time tmp = new Time();
        tmp.setToNow();
        msg = tmp.format("%T ") + msg;
        m_Adapter.insert(msg, 0);
        Log.d(hgdoApp.getAppContext().getString(R.string.app_name), msg);
      } else if (action.equals(HGDOService.SERVICE_START_DOOR_TIMER)) {
        m_CountDownTimer.start();
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
