package net.lasley.hgdo;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.location.LocationClient.OnRemoveGeofencesResultListener;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class GeofenceRemover implements
            ConnectionCallbacks,
            OnConnectionFailedListener,
            OnRemoveGeofencesResultListener {
    private Context mContext;
    private List<String> mCurrentGeofenceIds;
    private LocationClient mLocationClient;
    private PendingIntent mCurrentIntent;
    private GeofenceUtils.REMOVE_TYPE mRequestType;
    private boolean mInProgress;
    public GeofenceRemover(Context context) {
        // Save the context
        mContext = context;

        // Initialize the globals to null
        mCurrentGeofenceIds = null;
        mLocationClient = null;
        mInProgress = false;
    }

    public void setInProgressFlag(boolean flag) {
        // Set the "In Progress" flag.
        mInProgress = flag;
    }

    public boolean getInProgressFlag() {
        return mInProgress;
    }

    public void removeGeofencesById(List<String> geofenceIds) throws
        IllegalArgumentException, UnsupportedOperationException {
        if ((null == geofenceIds) || (geofenceIds.size() == 0)) {
            throw new IllegalArgumentException();
        } else {
            if (!mInProgress) {
                mRequestType = GeofenceUtils.REMOVE_TYPE.LIST;
                mCurrentGeofenceIds = geofenceIds;
                requestConnection();
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    public void removeGeofencesByIntent(PendingIntent requestIntent) {
        if (!mInProgress) {
            mRequestType = GeofenceUtils.REMOVE_TYPE.INTENT;
            mCurrentIntent = requestIntent;
            requestConnection();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void continueRemoveGeofences() {
        switch (mRequestType) {
            case INTENT :
                mLocationClient.removeGeofences(mCurrentIntent, this);
                break;
            case LIST :
                mLocationClient.removeGeofences(mCurrentGeofenceIds, this);
                break;
        }
    }

    private void requestConnection() {
        getLocationClient().connect();
    }

    private GooglePlayServicesClient getLocationClient() {
        if (mLocationClient == null) {
            mLocationClient = new LocationClient(mContext, this, this);
        }
        return mLocationClient;
    }

    @Override
    public void onRemoveGeofencesByPendingIntentResult(int statusCode,
            PendingIntent requestIntent) {
        Intent broadcastIntent = new Intent();
        if (statusCode == LocationStatusCodes.SUCCESS) {
            Log.d(GeofenceUtils.APPTAG, mContext.getString(net.lasley.hgdo.R.string.remove_geofences_intent_success));
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);
            broadcastIntent.putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, mContext.getString(net.lasley.hgdo.R.string.remove_geofences_intent_success));
        } else {
            Log.e(GeofenceUtils.APPTAG, mContext.getString(net.lasley.hgdo.R.string.remove_geofences_intent_failure, statusCode));
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);
            broadcastIntent.putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, mContext.getString(net.lasley.hgdo.R.string.remove_geofences_intent_failure, statusCode));
        }
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(broadcastIntent);
        requestDisconnection();
    }

    @Override
    public void onRemoveGeofencesByRequestIdsResult(int statusCode, String[] geofenceRequestIds) {
        Intent broadcastIntent = new Intent();
        String msg;
        if (LocationStatusCodes.SUCCESS == statusCode) {
            msg = mContext.getString(net.lasley.hgdo.R.string.remove_geofences_id_success,
                    Arrays.toString(geofenceRequestIds));
            Log.d(GeofenceUtils.APPTAG, msg);
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED)
                           .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                           .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, msg);
        } else {
            msg = mContext.getString(
                    net.lasley.hgdo.R.string.remove_geofences_id_failure,
                    statusCode,
                    Arrays.toString(geofenceRequestIds)
            );
            Log.e(GeofenceUtils.APPTAG, msg);
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_ERROR)
                           .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                           .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, msg);
        }
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(broadcastIntent);
        requestDisconnection();
    }

    private void requestDisconnection() {
        mInProgress = false;
        getLocationClient().disconnect();
        if (mRequestType == GeofenceUtils.REMOVE_TYPE.INTENT) {
            mCurrentIntent.cancel();
        }
    }

    @Override
    public void onConnected(Bundle arg0) {
        Log.d(GeofenceUtils.APPTAG, mContext.getString(net.lasley.hgdo.R.string.connected));
        continueRemoveGeofences();
    }

    @Override
    public void onDisconnected() {
        mInProgress = false;
        Log.d(GeofenceUtils.APPTAG, mContext.getString(net.lasley.hgdo.R.string.disconnected));
        mLocationClient = null;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mInProgress = false;
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult((Activity) mContext, GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (SendIntentException e) {
                e.printStackTrace();
            }

        } else {
            Intent errorBroadcastIntent = new Intent(GeofenceUtils.ACTION_CONNECTION_ERROR);
            errorBroadcastIntent.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                                .putExtra(GeofenceUtils.EXTRA_CONNECTION_ERROR_CODE,
                                        connectionResult.getErrorCode());
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(errorBroadcastIntent);
        }
    }
}
