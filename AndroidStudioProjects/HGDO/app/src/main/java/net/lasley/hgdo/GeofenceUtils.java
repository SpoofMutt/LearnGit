package net.lasley.hgdo;

/**
 * Created by Kent on 8/20/2014.
 */

import android.content.Context;
import android.location.Location;

public final class GeofenceUtils {
    public static final String APPTAG = "Geofence Detection";
    public static final String ACTION_CONNECTION_ERROR = "net.lasley.hgdo.ACTION_CONNECTION_ERROR";
    public static final String ACTION_CONNECTION_SUCCESS = "net.lasley.hgdo.ACTION_CONNECTION_SUCCESS";
    public static final String ACTION_GEOFENCES_ADDED = "net.lasley.hgdo.ACTION_GEOFENCES_ADDED";
    public static final String ACTION_GEOFENCES_REMOVED = "net.lasley.hgdo.ACTION_GEOFENCES_DELETED";
    public static final String ACTION_GEOFENCE_ERROR = "net.lasley.hgdo.ACTION_GEOFENCES_ERROR";
    public static final String ACTION_GEOFENCE_ENTER = "net.lasley.hgdo.ACTION_GEOFENCE_ENTER";
    public static final String ACTION_GEOFENCE_EXIT = "net.lasley.hgdo.ACTION_GEOFENCE_EXIT";
    public static final String ACTION_GEOFENCE_TRANSITION_ERROR = "net.lasley.hgdo.ACTION_GEOFENCE_TRANSITION_ERROR";
    public static final String CATEGORY_LOCATION_SERVICES = "net.lasley.hgdo.CATEGORY_LOCATION_SERVICES";
    public static final String EXTRA_CONNECTION_CODE = "net.lasley.hgdo.EXTRA_CONNECTION_CODE";
    public static final String EXTRA_CONNECTION_ERROR_CODE = "net.lasley.hgdo.EXTRA_CONNECTION_ERROR_CODE";
    public static final String EXTRA_CONNECTION_ERROR_MESSAGE = "net.lasley.hgdo.EXTRA_CONNECTION_ERROR_MESSAGE";
    public static final String EXTRA_GEOFENCE_STATUS = "net.lasley.hgdo.EXTRA_GEOFENCE_STATUS";
    public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final String EMPTY_STRING = new String();
    public static final CharSequence GEOFENCE_ID_DELIMITER = ",";

    public static String getLatLng(Context context, Location currentLocation) {
        if (currentLocation != null) {
            return context.getString(
                    net.lasley.hgdo.R.string.latitude_longitude,
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());
        } else {
            return EMPTY_STRING;
        }
    }
    public enum REMOVE_TYPE {INTENT, LIST}

    public enum REQUEST_TYPE {ADD, REMOVE}

}
