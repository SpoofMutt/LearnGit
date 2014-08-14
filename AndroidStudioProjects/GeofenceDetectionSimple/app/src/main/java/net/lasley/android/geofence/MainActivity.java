package net.lasley.android.geofence;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.format.Time;

import net.lasley.android.geofence.GeofenceUtils.REMOVE_TYPE;
import net.lasley.android.geofence.GeofenceUtils.REQUEST_TYPE;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private final static int  CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS   =  Geofence.NEVER_EXPIRE;
    // Store the current request
    private REQUEST_TYPE mRequestType;

    // Store the current type of removal
    private REMOVE_TYPE mRemoveType;

    // Store a list of geofences to add
    List<Geofence> mCurrentGeofences;
    List<SimpleGeofence> mUIGeofence;
    List<String>   mAreaVisits;

    // Add handlers
    private GeofenceRequester mGeofenceRequester;
    private GeofenceRemover mGeofenceRemover;

    private GeofenceSampleReceiver mBroadcastReceiver;

    private IntentFilter mIntentFilter;

    private LocationClient mLocationClient;

    private ArrayAdapter<String> adapter;

    private Socket Garagesocket;
    private static final int GARAGE_PORT = 55555;
    private static final String SERVER_HOSTNAME = "lasley.mynetgear.com";

//    private GoogleMap map;

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

//        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, mIntentFilter);

        // Attach to the main UI
        setContentView(net.lasley.android.geofence.R.layout.activity_main);
        ListView list = (ListView) findViewById(net.lasley.android.geofence.R.id.Activity);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mAreaVisits);
        list.setAdapter(adapter);

/*
        map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
        map.setMyLocationEnabled(true);
*/
        new Thread(new ClientThread()).start();

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
                        Log.d(GeofenceUtils.APPTAG, getString(net.lasley.android.geofence.R.string.no_resolution));
                }
            default:
                Log.d(GeofenceUtils.APPTAG, getString(net.lasley.android.geofence.R.string.unknown_activity_request_code, requestCode));
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
            Toast.makeText(this, net.lasley.android.geofence.R.string.remove_geofences_already_requested_error,Toast.LENGTH_LONG).show();
        }
    }

    public void AddGeoFencing() {
        mRequestType = GeofenceUtils.REQUEST_TYPE.ADD;
        if (!servicesConnected()) {
            return;
        }

        mUIGeofence.add(new SimpleGeofence("LOWER_APPROACH", 38.931323d, -104.726197d, 30.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT));
        // 1/3 Lower Entry
        mUIGeofence.add(new SimpleGeofence("LOWER_ENTRY",    38.930366d, -104.726935d, 20.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT));
        // 1/2 Lower Entry
        //mUIGeofence.add(new SimpleGeofence("LOWER_ENTRY",    38.930106d,  -104.72671d, 20.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT));
        mUIGeofence.add(new SimpleGeofence("UPPER_APPROACH", 38.932177d, -104.724792d, 20.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT)); //
        // 1/3 Upper Entry
        mUIGeofence.add(new SimpleGeofence("UPPER_ENTRY",    38.931571d,  -104.724192d,  30.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT));
        // 1/2 Upper Entry
        //mUIGeofence.add(new SimpleGeofence("UPPER_ENTRY",    38.931185d,  -104.724138d,  30.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT));
        mUIGeofence.add(new SimpleGeofence("DRIVEWAY",       38.930474d,  -104.725154d, 10.0f, GEOFENCE_EXPIRATION_IN_MILLISECONDS, Geofence.GEOFENCE_TRANSITION_EXIT|Geofence.GEOFENCE_TRANSITION_ENTER));


        mCurrentGeofences.clear();
        for (SimpleGeofence element : mUIGeofence) {
            mCurrentGeofences.add(element.toGeofence());
        }

        try {
            mGeofenceRequester.addGeofences(mCurrentGeofences);
        } catch (UnsupportedOperationException e) {
            Toast.makeText(this, net.lasley.android.geofence.R.string.add_geofences_already_requested_error, Toast.LENGTH_LONG).show();
        }
    }

    public class GeofenceSampleReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ERROR)) {
                handleGeofenceError(context, intent);
            } else if ( TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_ADDED)
                         ||
                        TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_REMOVED)) {
                handleGeofenceStatus(context, intent);
            } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ENTER)) {
                handleGeofenceEnter(context, intent);
            } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_EXIT)) {
                handleGeofenceExit(context, intent);
            } else {
                Log.e(GeofenceUtils.APPTAG, getString(net.lasley.android.geofence.R.string.invalid_action_detail, action));
                Toast.makeText(context, net.lasley.android.geofence.R.string.invalid_action, Toast.LENGTH_LONG).show();
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
            adapter.insert(msg,0);
            Log.d(GeofenceUtils.APPTAG, action);
            getLocation();
        }

        private void handleGeofenceError(Context context, Intent intent) {
            String msg = intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
            Log.e(GeofenceUtils.APPTAG, msg);
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
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

    public void getLocation() {
        if (servicesConnected()) {
            Location currentLocation = mLocationClient.getLastLocation();
            ((TextView) (findViewById(net.lasley.android.geofence.R.id.lat_lng))).setText(GeofenceUtils.getLatLng(this, currentLocation));
        }
    }

    class ClientThread implements Runnable {
        @Override
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_HOSTNAME);
                Garagesocket = new Socket(serverAddr, GARAGE_PORT);
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

}
