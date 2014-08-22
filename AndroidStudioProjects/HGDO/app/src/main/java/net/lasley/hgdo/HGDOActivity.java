package net.lasley.hgdo;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
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

public class HGDOActivity extends FragmentActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = Geofence.NEVER_EXPIRE;
    //    private GoogleMap map;
    private static final int GARAGE_PORT = 55555;
    private static final String SERVER_HOSTNAME = "lasley.mynetgear.com";
    private static final int TIME_FOR_DOOR_TO_OPEN = 16;  // Seconds
    private static final int TIME_TO_WAIT_FOR_DOOR_TO_OPEN = (int) (TIME_FOR_DOOR_TO_OPEN * 1.1 * 1000);  // Milliseconds
    private static final int LENGTH_V1_NDX = 0;
    private static final int VERSION_V1_NDX = 1;
    private static final int MSG_VERSION = 0x01;
    private static final int ACTION_V1_NDX = 2;
    private static final int COMMAND_V1_NDX = 3;
    private static final int COMMAND_REPLY_V1_NDX = 3;
    private static final int STR_START_V1_NDX = 3;
    private static final int STATUS_DOOR_V1_NDX = 3;
    private static final int STATUS_LIGHT_V1_NDX = 4;
    private static final int STATUS_RANGE_V1_NDX = 5;
    // Version 1 Lengths
    private static final int COMMAND_LENGTH_V1 = 8;
    private static final int COMMAND_REPLY_LENGTH_V1 = 8;
    private static final int STATUS_REQUEST_LENGTH_V1 = 7;
    private static final int STATUS_REPLY_LENGTH_V1 = 10;
    private static final byte VERSION = 1;
    private static final byte STRING = 16;
    private static final byte COMMAND = 10;
    private static final byte COMMANDREPLY = 45;
    private static final byte STATUSREQ = 21;
    private static final byte STATUSREPLY = 33;
    private static final byte DOOR_CLOSED = 0;
    private static final byte DOOR_OPEN = 1;
    private static final byte DOOR_OPENING = 2;
    private static final byte DOOR_CLOSING = 3;
    private static final byte DOOR_BUSY = 4;
    private static final byte LIGHT_OFF = 0;
    private static final byte LIGHT_ON = 1;
    private static final byte OPEN_DOOR = 0;
    private static final byte CLOSE_DOOR = 1;
    private static final byte TOGGLE_DOOR = 2;
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

    @Override
    public void onConnected(Bundle dataBundle) {
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        CheckBox cb = (CheckBox) findViewById(R.id.checkGPS);
        if (cb.isChecked()) {
            Log.d(GeofenceUtils.APPTAG, "onResume - AddGeoFencing.");
            AddGeoFencing();
        }
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

        mUIGeofence = new ArrayList<SimpleGeofence>();
        mCurrentGeofences = new ArrayList<Geofence>();
        mAreaVisits = new ArrayList<String>();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ENTER);
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_EXIT);
        // All Location Services sample apps use this category
        mIntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

        mLocationClient = new LocationClient(this, this, this);
        mLocationClient.connect();

        mBroadcastReceiver = new GeofenceSampleReceiver();
        mGeofenceRequester = new GeofenceRequester(this);
        mGeofenceRemover = new GeofenceRemover(this);

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, mIntentFilter);

        // Attach to the main UI
        setContentView(R.layout.activity_hgdo);

        countDownTimer = new ToggleDoorCountDownTimer(TIME_TO_WAIT_FOR_DOOR_TO_OPEN, (int) (TIME_TO_WAIT_FOR_DOOR_TO_OPEN / 11.0));

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
//        CheckBox cb = (CheckBox)findViewById(R.id.checkGPS);
//        if(cb.isChecked()) {
//            Log.d(GeofenceUtils.APPTAG, "onResume - AddGeoFencing.");
//            AddGeoFencing();
//        }
//        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(GeofenceUtils.APPTAG, "OnPause");
//        CheckBox cb = (CheckBox)findViewById(R.id.checkGPS);
//        if(cb.isChecked()) {
//            Log.d(GeofenceUtils.APPTAG, "onPause - RemoveGeoFencing.");
//            RemoveGeoFencing();
//        }
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
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

        mUIGeofence.clear();
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
            (new GetAddressTask(this)).execute(currentLocation);
            String feetstr = new DecimalFormat("0.0").format(currentLocation.getAccuracy() * 3.2808);
            ((TextView) (findViewById(net.lasley.hgdo.R.id.accuracy))).setText(feetstr + " ft.");
        }
    }

    protected void sendStatusRequest() {
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

    public void RefreshState(View view) {
        sendStatusRequest();
    }

    public void SetGPSState(View view) {
        CheckBox cb = (CheckBox) findViewById(R.id.checkGPS);
        if (cb.isChecked()) {
            Log.d(GeofenceUtils.APPTAG, "SetGPSState - Checked.");
            AddGeoFencing();
        } else {
            Log.d(GeofenceUtils.APPTAG, "SetGPSState - Unchecked.");
            RemoveGeoFencing();
        }
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

    public class ToggleDoorCountDownTimer extends CountDownTimer {
        private ProgressBar progressBar;

        public ToggleDoorCountDownTimer(long startTime, long interval) {
            super(startTime, interval);
            progressBar = (ProgressBar) findViewById(R.id.WaitForDoor);
        }

        @Override
        public void onFinish() {
            progressBar.setProgress(0);
            sendStatusRequest();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            progressBar.incrementProgressBy(10);
        }
    }

    private class AsyncGarage extends AsyncTask<byte[], Void, byte[]> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected byte[] doInBackground(byte[]... outbuffers) {
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
            String action = String.format("Status() %s", intent.getAction());
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

    private class GetAddressTask extends
            AsyncTask<Location, Void, String> {
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
            Geocoder geocoder =
                    new Geocoder(mContext, Locale.getDefault());
            // Get the current location from the input parameter list
            Location loc = params[0];
            // Create a list to contain the result address
            List<Address> addresses = null;
            try {
                /*
                 * Return 1 address.
                 */
                addresses = geocoder.getFromLocation(loc.getLatitude(),
                        loc.getLongitude(), 1);
            } catch (IOException e1) {
                Log.e("LocationSampleActivity",
                        "IO Exception in getFromLocation()");
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
                String addressText = String.format(
                        "%s, %s, %s",
                        // If there's a street address, add it
                        address.getMaxAddressLineIndex() > 0 ?
                                address.getAddressLine(0) : "",
                        // Locality is usually a city
                        address.getLocality(),
                        // The country of the address
                        address.getCountryName());
                // Return the text
                return addressText;
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
