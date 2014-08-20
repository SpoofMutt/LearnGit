package net.lasley.hgdo;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.Location;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import net.lasley.hgdo.GeofenceUtils.REMOVE_TYPE;
import net.lasley.hgdo.GeofenceUtils.REQUEST_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.text.format.DateUtils.FORMAT_SHOW_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_TIME;
import static android.text.format.DateUtils.formatDateTime;

public class HGDOActivity extends FragmentActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = Geofence.NEVER_EXPIRE;
    // Store a list of geofences to add
    List<Geofence> mCurrentGeofences;
    List<SimpleGeofence> mUIGeofence;
    List<String> mAreaVisits;
    // Store the current request
    private REQUEST_TYPE mRequestType;
    // Store the current type of removal
    private REMOVE_TYPE mRemoveType;
    // Add handlers
    private GeofenceRequester mGeofenceRequester;
    private GeofenceRemover mGeofenceRemover;
    private GeofenceSampleReceiver mBroadcastReceiver;
    private IntentFilter mIntentFilter;
    private LocationClient mLocationClient;
    private ArrayAdapter<String> adapter;
    private ToggleDoorCountDownTimer countDownTimer;
    private ProgressBar progressBar;

//    private GoogleMap map;
    private static final int GARAGE_PORT = 55555;
    private static final String SERVER_HOSTNAME = "lasley.mynetgear.com";

    private static final int TIME_FOR_DOOR_TO_OPEN = 30;  // Seconds
    private static final int TIME_TO_WAIT_FOR_DOOR_TO_OPEN = (int)(TIME_FOR_DOOR_TO_OPEN * 1.1 * 1000);  // Milliseconds
    private static final int LENGTH_V1_NDX         = 0;
    private static final int VERSION_V1_NDX        = 1;
    private static final int MSG_VERSION           = 0x01;
    private static final int ACTION_V1_NDX         = 2;
    private static final int COMMAND_V1_NDX        = 3;
    private static final int COMMAND_REPLY_V1_NDX  = 3;
    private static final int STR_START_V1_NDX      = 3;
    private static final int STATUS_DOOR_V1_NDX    = 3;
    private static final int STATUS_LIGHT_V1_NDX   = 4;

// Version 1 Lengths
    private static final int COMMAND_LENGTH_V1         = 8;
    private static final int COMMAND_REPLY_LENGTH_V1   = 8;
    private static final int STATUS_REQUEST_LENGTH_V1  = 7;
    private static final int STATUS_REPLY_LENGTH_V1    = 9;

    private static final byte VERSION = 1;

    private static final byte STRING       = 16;
    private static final byte COMMAND      = 10;
    private static final byte COMMANDREPLY = 45;
    private static final byte STATUSREQ    = 21;
    private static final byte STATUSREPLY  = 33;

    private static final byte DOOR_CLOSED  = 0;
    private static final byte DOOR_OPEN    = 1;
    private static final byte DOOR_OPENING = 2;
    private static final byte DOOR_CLOSING = 3;
    private static final byte DOOR_BUSY    = 4;

    private static final byte LIGHT_OFF = 0;
    private static final byte LIGHT_ON  = 1;

    private static final byte OPEN_DOOR   = 0;
    private static final byte CLOSE_DOOR  = 1;
    private static final byte TOGGLE_DOOR = 2;

    @Override
    public void onConnected(Bundle dataBundle) {
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        Location mCurrentLocation = mLocationClient.getLastLocation();
        // map.animateCamera(CameraUpdateFactory.newLatLngZoom( new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), 17));
        AddGeoFencing();
    }

    @Override
    public void onDisconnected() {
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(GeofenceUtils.APPTAG, "ErrorCode: " + Integer.toString(connectionResult.getErrorCode()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocationClient = new LocationClient(this, this, this);
        mLocationClient.connect();
        mBroadcastReceiver = new GeofenceSampleReceiver();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ENTER);
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_EXIT);
        // All Location Services sample apps use this category
        mIntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

        mUIGeofence = new ArrayList<SimpleGeofence>();
        mCurrentGeofences = new ArrayList<Geofence>();
        mAreaVisits = new ArrayList<String>();

        // Instantiate a Geofence requester/Removers
        mGeofenceRequester = new GeofenceRequester(this);
        mGeofenceRemover = new GeofenceRemover(this);

        countDownTimer = new ToggleDoorCountDownTimer(TIME_TO_WAIT_FOR_DOOR_TO_OPEN,
                (int)(TIME_TO_WAIT_FOR_DOOR_TO_OPEN/10.0));
        progressBar = (ProgressBar) findViewById(R.id.WaitForDoor);
        // Attach to the main UI
        setContentView(R.layout.activity_hgdo);
        ListView list = (ListView) findViewById(R.id.Activity);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mAreaVisits);
        list.setAdapter(adapter);

/*
        map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setMyLocationEnabled(true);
*/
        sendStatusRequest();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (GeofenceUtils.REQUEST_TYPE.ADD == mRequestType) {
                            mGeofenceRequester.setInProgressFlag(false);
                            mGeofenceRequester.addGeofences(mCurrentGeofences);
                        } else if (GeofenceUtils.REQUEST_TYPE.REMOVE == mRequestType) {
                            mGeofenceRemover.setInProgressFlag(false);
                            if (GeofenceUtils.REMOVE_TYPE.INTENT == mRemoveType) {
                                mGeofenceRemover.removeGeofencesByIntent(
                                        mGeofenceRequester.getRequestPendingIntent());
                            }
                        }
                        break;
                    default:
                        Log.d(GeofenceUtils.APPTAG, getString(net.lasley.hgdo.R.string.no_resolution));
                }
            default:
                Log.d(GeofenceUtils.APPTAG, getString(net.lasley.hgdo.R.string.unknown_activity_request_code, requestCode));
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the broadcast receiver to receive status updates
        Log.d(GeofenceUtils.APPTAG, "OnResume");
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(GeofenceUtils.APPTAG, "OnPause");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
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
                errorFragment.show(getSupportFragmentManager(), GeofenceUtils.APPTAG);
            }
            return false;
        }
    }

    public void RemoveGeoFencing() {
        mRemoveType = GeofenceUtils.REMOVE_TYPE.INTENT;
        if (!servicesConnected()) {
            return;
        }

        try {
            mGeofenceRemover.removeGeofencesByIntent(mGeofenceRequester.getRequestPendingIntent());
        } catch (UnsupportedOperationException e) {
            Toast.makeText(this, net.lasley.hgdo.R.string.remove_geofences_already_requested_error, Toast.LENGTH_LONG).show();
        }
    }

    public void AddGeoFencing() {
        mRequestType = GeofenceUtils.REQUEST_TYPE.ADD;
        if (!servicesConnected()) {
            return;
        }

        mUIGeofence.add(new SimpleGeofence("LOWER_APPROACH", 38.931323d, -104.726197d, 30.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT));
        // 1/3 Lower Entry
        mUIGeofence.add(new SimpleGeofence("LOWER_ENTRY", 38.930366d, -104.726935d, 20.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT));
        // 1/2 Lower Entry
        //mUIGeofence.add(new SimpleGeofence("LOWER_ENTRY",    38.930106d,  -104.72671d, 20.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT));
        mUIGeofence.add(new SimpleGeofence("UPPER_APPROACH", 38.932177d, -104.724792d, 20.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT)); //
        // 1/3 Upper Entry
        mUIGeofence.add(new SimpleGeofence("UPPER_ENTRY", 38.931571d, -104.724192d, 30.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT));
        // 1/2 Upper Entry
        //mUIGeofence.add(new SimpleGeofence("UPPER_ENTRY",    38.931185d,  -104.724138d,  30.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT));
        mUIGeofence.add(new SimpleGeofence("DRIVEWAY", 38.930474d, -104.725154d, 10.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_ENTER));


        mCurrentGeofences.clear();
        for (SimpleGeofence element : mUIGeofence) {
            mCurrentGeofences.add(element.toGeofence());
        }

        try {
            mGeofenceRequester.addGeofences(mCurrentGeofences);
        } catch (UnsupportedOperationException e) {
            Toast.makeText(this, net.lasley.hgdo.R.string.add_geofences_already_requested_error, Toast.LENGTH_LONG).show();
        }
    }

    public void getLocation() {
        if (servicesConnected()) {
            Location currentLocation = mLocationClient.getLastLocation();
            ((TextView) (findViewById(net.lasley.hgdo.R.id.lat_lng))).setText(GeofenceUtils.getLatLng(this, currentLocation));
        }
    }

    public class ToggleDoorCountDownTimer extends CountDownTimer {
        public ToggleDoorCountDownTimer(long startTime, long interval) {
            super(startTime, interval);
        }

        @Override
        public void onFinish() {
//            progressBar.setProgress(0);
            sendStatusRequest();
        }

        @Override
        public void onTick(long millisUntilFinished) {
//            progressBar.incrementProgressBy(10);
        }
    }

    protected void sendStatusRequest() {
        byte[] msg = new byte[7];
        msg[LENGTH_V1_NDX]              = STATUS_REQUEST_LENGTH_V1;
        msg[VERSION_V1_NDX]             = VERSION;
        msg[ACTION_V1_NDX]              = STATUSREQ;
        msg[STATUS_REQUEST_LENGTH_V1-4] = 1;
        msg[STATUS_REQUEST_LENGTH_V1-3] = 2;
        msg[STATUS_REQUEST_LENGTH_V1-2] = 3;
        msg[STATUS_REQUEST_LENGTH_V1-1] = 4;
        new AsyncGarage().execute(msg);
    }

    public void RefreshState(View view) {
        sendStatusRequest();
    }

    public void toggleDoor(View view) {
        byte[] msg = new byte[8];
        msg[LENGTH_V1_NDX]       = COMMAND_LENGTH_V1;
        msg[VERSION_V1_NDX]      = VERSION;
        msg[ACTION_V1_NDX]       = COMMAND;
        msg[COMMAND_V1_NDX]      = TOGGLE_DOOR;
        msg[COMMAND_LENGTH_V1-4] = 1;
        msg[COMMAND_LENGTH_V1-3] = 2;
        msg[COMMAND_LENGTH_V1-2] = 3;
        msg[COMMAND_LENGTH_V1-1] = 4;
        new AsyncGarage().execute(msg);
        countDownTimer.start();
    }

    protected void decodeReply(byte[] reply) {
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
            TextView t = (TextView)findViewById(R.id.DoorStatus);
            sb = new StringBuilder("Action: ");
            if(reply[COMMAND_REPLY_V1_NDX] == DOOR_OPENING) {
                sb.append("Door Opening");
                t.setText("Opening");
            } else if(reply[COMMAND_REPLY_V1_NDX] == DOOR_CLOSING) {
                sb.append("Door Closing");
                t.setText("Closing");
            } else if(reply[COMMAND_REPLY_V1_NDX] == DOOR_BUSY) {
                sb.append("Door Busy");
                t.setText("Busy");
            }
            Log.d("decodeReply", sb.toString());
        } else if (reply[ACTION_V1_NDX] == STATUSREPLY) {
            Calendar rightNow = Calendar.getInstance();
            String date = formatDateTime (getApplicationContext(),rightNow.getTimeInMillis(),FORMAT_SHOW_DATE|FORMAT_SHOW_TIME);
            TextView t = (TextView)findViewById(R.id.TimeChecked);
            t.setText("Refreshed: " + date);

            sb.append("StatusReply");
            Log.d("decodeReply", sb.toString());
            sb = new StringBuilder("Door: ");
            sb.append(Byte.toString(reply[STATUS_DOOR_V1_NDX]));
            Log.d("decodeReply", sb.toString());
            t = (TextView)findViewById(R.id.DoorStatus);
            if(reply[STATUS_DOOR_V1_NDX] == DOOR_OPEN) {
                t.setText("Open");
            } else if(reply[STATUS_DOOR_V1_NDX] == DOOR_CLOSED) {
                t.setText("Closed");
            } else if(reply[STATUS_DOOR_V1_NDX] == DOOR_BUSY) {
                t.setText("Busy");
            }
            sb = new StringBuilder("Light: ");
            sb.append(Byte.toString(reply[STATUS_LIGHT_V1_NDX]));
            Log.d("decodeReply", sb.toString());
            t = (TextView)findViewById(R.id.LightStatus);
            if(reply[STATUS_LIGHT_V1_NDX] == LIGHT_ON) {
                t.setText("On");
            } else if(reply[STATUS_LIGHT_V1_NDX] == LIGHT_OFF) {
                t.setText("Off");
            }
        }
    }

    private class AsyncGarage extends AsyncTask<byte[], Void, byte[]> {

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected byte[] doInBackground(byte[] ... outbuffers) {
            Socket nsocket = new Socket();   //Network Socket
            byte[] tempdata = new byte[20];

            boolean result = false;
            try {
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
            decodeReply(result);
        }

    }

    public static class ErrorDialogFragment extends DialogFragment {
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

    public class GeofenceSampleReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ERROR)) {
                handleGeofenceError(context, intent);
            } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_ADDED)
                    ||
                    TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_REMOVED)) {
                handleGeofenceStatus(context, intent);
            } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ENTER)) {
                handleGeofenceEnter(context, intent);
            } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_EXIT)) {
                handleGeofenceExit(context, intent);
            } else {
                Log.e(GeofenceUtils.APPTAG, getString(net.lasley.hgdo.R.string.invalid_action_detail, action));
                Toast.makeText(context, net.lasley.hgdo.R.string.invalid_action, Toast.LENGTH_LONG).show();
            }
        }

        private void handleGeofenceStatus(Context context, Intent intent) {
            String action = String.format("Status() %s", intent.getAction().toString());
            Log.d(GeofenceUtils.APPTAG, action);
        }

        private void handleGeofenceEnter(Context context, Intent intent) {
            String action = String.format("Enter() %s", intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS));
            Log.d(GeofenceUtils.APPTAG, action);
            getLocation();
        }

        private void handleGeofenceExit(Context context, Intent intent) {
            String action = String.format("Exit() %s", intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS));
            Time tmp = new Time();
            tmp.setToNow();
            String msg = tmp.format("%T ") + ": " + intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
            adapter.insert(msg, 0);
            Log.d(GeofenceUtils.APPTAG, action);
            getLocation();
        }

        private void handleGeofenceError(Context context, Intent intent) {
            String msg = intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
            Log.e(GeofenceUtils.APPTAG, msg);
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }
}
