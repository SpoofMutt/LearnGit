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

package com.example.android.mocklocation;

/**
 *  Constants used in other classes in the app
 */
public final class LocationUtils {

    // Debugging tag for the application
    public static final String APPTAG = "Location Mock Tester";

    // Create an empty string for initializing strings
    public static final String EMPTY_STRING = new String();

    // Conversion factor for boot time
    public static final long NANOSECONDS_PER_MILLISECOND = 1000000;

    // Conversion factor for time values
    public static final long MILLISECONDS_PER_SECOND = 1000;

    // Conversion factor for time values
    public static final long NANOSECONDS_PER_SECOND =
                    NANOSECONDS_PER_MILLISECOND * MILLISECONDS_PER_SECOND;

    /*
     * Action values sent by Intent from the main activity to the service
     */
    // Request a one-time test
    public static final String ACTION_START_ONCE =
            "com.example.android.mocklocation.ACTION_START_ONCE";

    // Request continuous testing
    public static final String ACTION_START_CONTINUOUS =
                    "com.example.android.mocklocation.ACTION_START_CONTINUOUS";

    // Stop a continuous test
    public static final String ACTION_STOP_TEST =
                    "com.example.android.mocklocation.ACTION_STOP_TEST";

    /*
     * Extended data keys for the broadcast Intent sent from the service to the main activity.
     * Key1 is the base connection message.
     * Key2 is extra data or error codes.
     */
    public static final String KEY_EXTRA_CODE1 =
            "com.example.android.mocklocation.KEY_EXTRA_CODE1";

    public static final String KEY_EXTRA_CODE2 =
            "com.example.android.mocklocation.KEY_EXTRA_CODE2";

    /*
     * Codes for communicating status back to the main activity
     */

    // The location client is disconnected
    public static final int CODE_DISCONNECTED = 0;

    // The location client is connected
    public static final int CODE_CONNECTED = 1;

    // The client failed to connect to Location Services
    public static final int CODE_CONNECTION_FAILED = -1;

    // Report in the broadcast Intent that the test finished
    public static final int CODE_TEST_FINISHED = 3;

    /*
     * Report in the broadcast Intent that the activity requested the start to a test, but a
     * test is already underway
     *
     */
    public static final int CODE_IN_TEST = -2;

    // The test was interrupted by clicking "Stop testing"
    public static final int CODE_TEST_STOPPED = -3;

    // The name used for all mock locations
    public static final String LOCATION_PROVIDER = "fused";

    public static final double[] WAYPOINTS_LAT = {
            38.931272,
            38.931001,
            38.9308725,
            38.930744,
            38.9306155,
            38.930487,
            38.930283,
            38.93012,
            38.930124,
            38.930128,
            38.930222,
            38.930316,
            38.930424,
            38.930532,
            38.9306975,
            38.930809,
            38.930905,
            38.931001,
            38.931038,
            38.931284,
            38.931581,
            38.931852,
            38.9320125,
            38.932173,
            38.932185125,
            38.93208025,
            38.931975375,
            38.9318705,
            38.931765625,
            38.93166075,
            38.931555875,
            38.931451,
            38.931272,
            38.931001,
            38.9308725,
            38.930744,
            38.9306155,
            38.930487,
            38.930283,
            38.93012,
            38.930124,
            38.930128,
            38.930222,
            38.930316,
            38.930424,
            38.930532,
            38.9306975,
            38.930809,
            38.930905,
            38.931001,
            38.931038,
            38.931284,
            38.931581,
            38.931852,
            38.9320125,
            38.932173,
            38.932185125,
            38.93208025,
            38.931975375,
            38.9318705,
            38.931765625,
            38.93166075,
            38.931555875,
            38.931451,
            38.931451,
            38.931555875,
            38.93166075,
            38.931765625,
            38.9318705,
            38.931975375,
            38.93208025,
            38.932185125,
            38.93229,
            38.932173,
            38.9320125,
            38.931852,
            38.931581,
            38.931284,
            38.931038,
            38.931001,
            38.930905,
            38.930809,
            38.9306975,
            38.930586,
            38.930424,
            38.930316,
            38.930222,
            38.930128,
            38.930124,
            38.93012,
            38.930283,
            38.930487,
            38.9306155,
            38.930744,
            38.9308725,
            38.931001,
            38.931272,
            38.931451,
            38.931555875,
            38.93166075,
            38.931765625,
            38.9318705,
            38.931975375,
            38.93208025,
            38.932185125,
            38.93229,
            38.932173,
            38.9320125,
            38.931852,
            38.931581,
            38.931284,
            38.931038,
            38.931001,
            38.930905,
            38.930809,
            38.9306975,
            38.930586,
            38.930424,
            38.930316,
            38.930222,
            38.930128,
            38.930124,
            38.93012,
            38.930283,
            38.930487,
            38.9306155,
            38.930744,
            38.9308725,
            38.931001,
            38.931272};

    public static final double[] WAYPOINTS_LNG = {
            -104.726207,
            -104.725966,
            -104.7262195,
            -104.726473,
            -104.7267265,
            -104.72698,
            -104.726803,
            -104.726524,
            -104.7262905,
            -104.726057,
            -104.72584,
            -104.725623,
            -104.7254275,
            -104.725232,
            -104.7249075,
            -104.724689,
            -104.7244665,
            -104.724244,
            -104.724067,
            -104.724094,
            -104.724137,
            -104.724324,
            -104.72445,
            -104.724576,
            -104.724883125,
            -104.72509025,
            -104.725297375,
            -104.7255045,
            -104.725711625,
            -104.72591875,
            -104.726125875,
            -104.726333,
            -104.726207,
            -104.725966,
            -104.7262195,
            -104.726473,
            -104.7267265,
            -104.72698,
            -104.726803,
            -104.726524,
            -104.7262905,
            -104.726057,
            -104.72584,
            -104.725623,
            -104.7254275,
            -104.725232,
            -104.7249075,
            -104.724689,
            -104.7244665,
            -104.724244,
            -104.724067,
            -104.724094,
            -104.724137,
            -104.724324,
            -104.72445,
            -104.724576,
            -104.724883125,
            -104.72509025,
            -104.725297375,
            -104.7255045,
            -104.725711625,
            -104.72591875,
            -104.726125875,
            -104.726333,
            -104.726333,
            -104.726125875,
            -104.72591875,
            -104.725711625,
            -104.7255045,
            -104.725297375,
            -104.72509025,
            -104.724883125,
            -104.724676,
            -104.724576,
            -104.72445,
            -104.724324,
            -104.724137,
            -104.724094,
            -104.724067,
            -104.724244,
            -104.7244665,
            -104.724689,
            -104.7249075,
            -104.725126,
            -104.7254275,
            -104.725623,
            -104.72584,
            -104.726057,
            -104.7262905,
            -104.726524,
            -104.726803,
            -104.72698,
            -104.7267265,
            -104.726473,
            -104.7262195,
            -104.725966,
            -104.726207,
            -104.726333,
            -104.726125875,
            -104.72591875,
            -104.725711625,
            -104.7255045,
            -104.725297375,
            -104.72509025,
            -104.724883125,
            -104.724676,
            -104.724576,
            -104.72445,
            -104.724324,
            -104.724137,
            -104.724094,
            -104.724067,
            -104.724244,
            -104.7244665,
            -104.724689,
            -104.7249075,
            -104.725126,
            -104.7254275,
            -104.725623,
            -104.72584,
            -104.726057,
            -104.7262905,
            -104.726524,
            -104.726803,
            -104.72698,
            -104.7267265,
            -104.726473,
            -104.7262195,
            -104.725966,
            -104.726207};

    public static final float[] WAYPOINTS_ACCURACY = {
            3.31866455078125f,
            3.70162963867188f,
            3.14913940429688f,
            3.90945434570313f,
            3.14361572265625f,
            3.92477416992188f,
            3.54385375976563f,
            3.05807495117188f,
            3.12322998046875f,
            3.43798828125f,
            3.46255493164063f,
            3.48382568359375f,
            3.630615234375f,
            3.5716552734375f,
            3.064697265625f,
            3.75558471679688f,
            3.31698608398438f,
            3.79837036132813f,
            3.15399169921875f,
            3.29873657226563f,
            3.8970947265625f,
            3.50173950195313f,
            3.495849609375f,
            3.5675048828125f,
            3.25665283203125f,
            3.77911376953125f,
            3.0352783203125f,
            3.51641845703125f,
            3.66339111328125f,
            3.93307495117188f,
            3.04638671875f,
            3.27667236328125f,
            3.36297607421875f,
            3.80926513671875f,
            3.74496459960938f,
            3.44378662109375f,
            3.34078979492188f,
            3.6356201171875f,
            3.39480590820313f,
            3.88296508789063f,
            3.531005859375f,
            3.52020263671875f,
            3.43984985351563f,
            3.45819091796875f,
            3.25430297851563f,
            3.26321411132813f,
            3.6927490234375f,
            3.5914306640625f,
            3.1651611328125f,
            3.6400146484375f,
            3.11129760742188f,
            3.05816650390625f,
            3.49029541015625f,
            3.8106689453125f,
            3.19412231445313f,
            3.92556762695313f,
            3.83938598632813f,
            3.01318359375f,
            3.52667236328125f,
            3.03927612304688f,
            3.36917114257813f,
            3.19577026367188f,
            3.50888061523438f,
            3.96563720703125f,
            3.58242797851563f,
            3.74508666992188f,
            3.79574584960938f,
            3.79486083984375f,
            3.54293823242188f,
            3.96627807617188f,
            3.48199462890625f,
            3.71200561523438f,
            3.78778076171875f,
            3.47140502929688f,
            3.5621337890625f,
            3.74176025390625f,
            3.71798706054688f,
            3.13201904296875f,
            3.697265625f,
            3.57327270507813f,
            3.74908447265625f,
            3.7528076171875f,
            3.81961059570313f,
            3.87127685546875f,
            3.00265502929688f,
            3.12188720703125f,
            3.6063232421875f,
            3.979736328125f,
            3.82009887695313f,
            3.59442138671875f,
            3.35360717773438f,
            3.4246826171875f,
            3.87991333007813f,
            3.86581420898438f,
            3.28253173828125f,
            3.12155151367188f,
            3.19989013671875f,
            3.09036254882813f,
            3.8076171875f,
            3.69430541992188f,
            3.80502319335938f,
            3.00198364257813f,
            3.79983520507813f,
            3.81198120117188f,
            3.34304809570313f,
            3.19320678710938f,
            3.37454223632813f,
            3.94903564453125f,
            3.42819213867188f,
            3.20858764648438f,
            3.23666381835938f,
            3.47579956054688f,
            3.99484252929688f,
            3.60867309570313f,
            3.11312866210938f,
            3.48529052734375f,
            3.4422607421875f,
            3.66116333007813f,
            3.30496215820313f,
            3.26254272460938f,
            3.85134887695313f,
            3.81503295898438f,
            3.88937377929688f,
            3.802734375f,
            3.05526733398438f,
            3.61434936523438f,
            3.6038818359375f,
            3.28466796875f,
            3.84451293945313f,
            3.20761108398438f};


    // Mark the broadcast Intent with an action
    public static final String ACTION_SERVICE_MESSAGE =
            "com.example.android.mocklocation.ACTION_SERVICE_MESSAGE";

    /*
     * Key for extended data in the Activity's outgoing Intent that records the type of test
     * requested.
     */
    public static final String EXTRA_TEST_ACTION =
            "com.example.android.mocklocation.EXTRA_TEST_ACTION";

    /*
     * Key for extended data in the Activity's outgoing Intent that records the requested pause
     * value.
     */
    public static final String EXTRA_PAUSE_VALUE =
            "com.example.android.mocklocation.EXTRA_PAUSE_VALUE";

    /*
     * Key for extended data in the Activity's outgoing Intent that records the requested interval
     * for mock locations sent to Location Services.
     */
    public static final String EXTRA_SEND_INTERVAL =
            "com.example.android.mocklocation.EXTRA_SEND_INTERVAL";
}
