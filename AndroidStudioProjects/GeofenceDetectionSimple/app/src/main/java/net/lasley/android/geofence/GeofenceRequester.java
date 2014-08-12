package net.lasley.android.geofence;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeofenceRequester
                implements
                    OnAddGeofencesResultListener,
                    ConnectionCallbacks,
                    OnConnectionFailedListener {
    private final Activity mActivity;
    private PendingIntent mGeofencePendingIntent;
    private ArrayList<Geofence> mCurrentGeofences;
    private LocationClient mLocationClient;
    private boolean mInProgress;

    public GeofenceRequester(Activity activityContext) {
        mActivity = activityContext;
        mGeofencePendingIntent = null;
        mLocationClient = null;
        mInProgress = false;
    }

    public void setInProgressFlag(boolean flag) {
        mInProgress = flag;
    }

    public boolean getInProgressFlag() {
        return mInProgress;
    }

    public PendingIntent getRequestPendingIntent() {
        return createRequestPendingIntent();
    }

    public void addGeofences(List<Geofence> geofences) throws UnsupportedOperationException {
        mCurrentGeofences = (ArrayList<Geofence>) geofences;
        if (!mInProgress) {
            mInProgress = true;
            requestConnection();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void requestConnection() {
        getLocationClient().connect();
    }

    private GooglePlayServicesClient getLocationClient() {
        if (mLocationClient == null) {
            mLocationClient = new LocationClient(mActivity, this, this);
        }
        return mLocationClient;
    }

    private void continueAddGeofences() {
        mGeofencePendingIntent = createRequestPendingIntent();
        mLocationClient.addGeofences(mCurrentGeofences, mGeofencePendingIntent, this);
    }


    @Override
    public void onAddGeofencesResult(int statusCode, String[] geofenceRequestIds) {
        Intent broadcastIntent = new Intent();
        String msg;
        if (LocationStatusCodes.SUCCESS == statusCode) {
            msg = mActivity.getString(net.lasley.android.geofence.R.string.add_geofences_result_success,
                    Arrays.toString(geofenceRequestIds));
            Log.d(GeofenceUtils.APPTAG, msg);
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCES_ADDED)
                           .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                           .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, msg);
        } else {
            msg = mActivity.getString(
                    net.lasley.android.geofence.R.string.add_geofences_result_failure,
                    statusCode,
                    Arrays.toString(geofenceRequestIds)
            );
            Log.e(GeofenceUtils.APPTAG, msg);
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_ERROR)
                           .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                           .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, msg);
        }
        LocalBroadcastManager.getInstance(mActivity).sendBroadcast(broadcastIntent);
        requestDisconnection();
    }

    private void requestDisconnection() {
        mInProgress = false;
        getLocationClient().disconnect();
    }

    @Override
    public void onConnected(Bundle arg0) {
        Log.d(GeofenceUtils.APPTAG, mActivity.getString(net.lasley.android.geofence.R.string.connected));
        continueAddGeofences();
    }

    @Override
    public void onDisconnected() {
        mInProgress = false;
        Log.d(GeofenceUtils.APPTAG, mActivity.getString(net.lasley.android.geofence.R.string.disconnected));
        mLocationClient = null;
    }

    private PendingIntent createRequestPendingIntent() {
        if (null != mGeofencePendingIntent) {
            return mGeofencePendingIntent;
        } else {
            Intent intent = new Intent(mActivity, ReceiveTransitionsIntentService.class);
            return PendingIntent.getService(
                    mActivity,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mInProgress = false;
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(mActivity,
                    GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Intent errorBroadcastIntent = new Intent(GeofenceUtils.ACTION_CONNECTION_ERROR);
            errorBroadcastIntent.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                                .putExtra(GeofenceUtils.EXTRA_CONNECTION_ERROR_CODE,
                                        connectionResult.getErrorCode());
            LocalBroadcastManager.getInstance(mActivity).sendBroadcast(errorBroadcastIntent);
        }
    }
}
