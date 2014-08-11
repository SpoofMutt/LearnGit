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

package com.example.android.geofence;

import android.content.Context;
import android.location.Location;

public final class GeofenceUtils {
    public enum REMOVE_TYPE {INTENT, LIST}
    public enum REQUEST_TYPE {ADD, REMOVE}

    public static final String APPTAG = "Geofence Detection";

    public static final String ACTION_CONNECTION_ERROR = "com.example.android.geofence.ACTION_CONNECTION_ERROR";
    public static final String ACTION_CONNECTION_SUCCESS = "com.example.android.geofence.ACTION_CONNECTION_SUCCESS";
    public static final String ACTION_GEOFENCES_ADDED = "com.example.android.geofence.ACTION_GEOFENCES_ADDED";
    public static final String ACTION_GEOFENCES_REMOVED = "com.example.android.geofence.ACTION_GEOFENCES_DELETED";
    public static final String ACTION_GEOFENCE_ERROR = "com.example.android.geofence.ACTION_GEOFENCES_ERROR";
    public static final String ACTION_GEOFENCE_ENTER = "com.example.android.geofence.ACTION_GEOFENCE_ENTER";
    public static final String ACTION_GEOFENCE_EXIT = "com.example.android.geofence.ACTION_GEOFENCE_EXIT";
    public static final String ACTION_GEOFENCE_TRANSITION_ERROR = "com.example.android.geofence.ACTION_GEOFENCE_TRANSITION_ERROR";

    public static final String CATEGORY_LOCATION_SERVICES = "com.example.android.geofence.CATEGORY_LOCATION_SERVICES";

    public static final String EXTRA_CONNECTION_CODE = "com.example.android.EXTRA_CONNECTION_CODE";
    public static final String EXTRA_CONNECTION_ERROR_CODE = "com.example.android.geofence.EXTRA_CONNECTION_ERROR_CODE";
    public static final String EXTRA_CONNECTION_ERROR_MESSAGE = "com.example.android.geofence.EXTRA_CONNECTION_ERROR_MESSAGE";
    public static final String EXTRA_GEOFENCE_STATUS = "com.example.android.geofence.EXTRA_GEOFENCE_STATUS";

    public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final String EMPTY_STRING = new String();
    public static final CharSequence GEOFENCE_ID_DELIMITER = ",";

    public static String getLatLng(Context context, Location currentLocation) {
        if (currentLocation != null) {
            return context.getString(
                    R.string.latitude_longitude,
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());
        } else {
            return EMPTY_STRING;
        }
    }

}
