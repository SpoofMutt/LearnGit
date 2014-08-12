/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lasley.android.geofence;

import android.content.Context;
import android.location.Location;

public final class GeofenceUtils {
    public enum REMOVE_TYPE {INTENT, LIST}
    public enum REQUEST_TYPE {ADD, REMOVE}

    public static final String APPTAG = "Geofence Detection";

    public static final String ACTION_CONNECTION_ERROR = "net.lasley.android.geofence.ACTION_CONNECTION_ERROR";
    public static final String ACTION_CONNECTION_SUCCESS = "net.lasley.android.geofence.ACTION_CONNECTION_SUCCESS";
    public static final String ACTION_GEOFENCES_ADDED = "net.lasley.android.geofence.ACTION_GEOFENCES_ADDED";
    public static final String ACTION_GEOFENCES_REMOVED = "net.lasley.android.geofence.ACTION_GEOFENCES_DELETED";
    public static final String ACTION_GEOFENCE_ERROR = "net.lasley.android.geofence.ACTION_GEOFENCES_ERROR";
    public static final String ACTION_GEOFENCE_ENTER = "net.lasley.android.geofence.ACTION_GEOFENCE_ENTER";
    public static final String ACTION_GEOFENCE_EXIT = "net.lasley.android.geofence.ACTION_GEOFENCE_EXIT";
    public static final String ACTION_GEOFENCE_TRANSITION_ERROR = "net.lasley.android.geofence.ACTION_GEOFENCE_TRANSITION_ERROR";

    public static final String CATEGORY_LOCATION_SERVICES = "net.lasley.android.geofence.CATEGORY_LOCATION_SERVICES";

    public static final String EXTRA_CONNECTION_CODE = "net.lasley.android.EXTRA_CONNECTION_CODE";
    public static final String EXTRA_CONNECTION_ERROR_CODE = "net.lasley.android.geofence.EXTRA_CONNECTION_ERROR_CODE";
    public static final String EXTRA_CONNECTION_ERROR_MESSAGE = "net.lasley.android.geofence.EXTRA_CONNECTION_ERROR_MESSAGE";
    public static final String EXTRA_GEOFENCE_STATUS = "net.lasley.android.geofence.EXTRA_GEOFENCE_STATUS";

    public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final String EMPTY_STRING = new String();
    public static final CharSequence GEOFENCE_ID_DELIMITER = ",";

    public static String getLatLng(Context context, Location currentLocation) {
        if (currentLocation != null) {
            return context.getString(
                    net.lasley.android.geofence.R.string.latitude_longitude,
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());
        } else {
            return EMPTY_STRING;
        }
    }

}
