package net.lasley.hgdo;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import java.util.List;

public class ReceiveTransitionsIntentService extends IntentService {
    public ReceiveTransitionsIntentService() {
        super("ReceiveTransitionsIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);
        if (LocationClient.hasError(intent)) {
            int errorCode = LocationClient.getErrorCode(intent);
            String errorMessage = LocationServiceErrorMessages.getErrorString(this, errorCode);
            Log.e(GeofenceUtils.APPTAG,
                    getString(net.lasley.hgdo.R.string.geofence_transition_error_detail, errorMessage)
            );
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_ERROR)
                    .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, errorMessage);

            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
        } else {
            int transition = LocationClient.getGeofenceTransition(intent);
            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                List<Geofence> geofences = LocationClient.getTriggeringGeofences(intent);
                String[] geofenceIds = new String[geofences.size()];
                for (int index = 0; index < geofences.size(); index++) {
                    geofenceIds[index] = geofences.get(index).getRequestId();
                }
                String ids = TextUtils.join(GeofenceUtils.GEOFENCE_ID_DELIMITER, geofenceIds);
                String transitionType = getTransitionString(transition);

                broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_ENTER)
                        .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, getString(
                                net.lasley.hgdo.R.string.geofence_transition_notification_title,
                                transitionType,
                                ids));
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

                //sendNotification(transitionType, ids);

                Log.d(GeofenceUtils.APPTAG, getString(
                        net.lasley.hgdo.R.string.geofence_transition_notification_title,
                        transitionType,
                        ids));
            } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                List<Geofence> geofences = LocationClient.getTriggeringGeofences(intent);
                String[] geofenceIds = new String[geofences.size()];
                for (int index = 0; index < geofences.size(); index++) {
                    geofenceIds[index] = geofences.get(index).getRequestId();
                }
                String ids = TextUtils.join(GeofenceUtils.GEOFENCE_ID_DELIMITER, geofenceIds);
                String transitionType = getTransitionString(transition);

                broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_EXIT)
                        .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, getString(
                                net.lasley.hgdo.R.string.geofence_transition_notification_title,
                                transitionType,
                                ids));
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

                //sendNotification(transitionType, ids);

                Log.d(GeofenceUtils.APPTAG, getString(
                        net.lasley.hgdo.R.string.geofence_transition_notification_title,
                        transitionType,
                        ids));
            } else {
                Log.e(GeofenceUtils.APPTAG, getString(net.lasley.hgdo.R.string.geofence_transition_invalid_type, transition));
            }
        }
    }

    private void sendNotification(String transitionType, String ids) {
        Intent notificationIntent = new Intent(getApplicationContext(), HGDOActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(HGDOActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(net.lasley.hgdo.R.drawable.ic_notification)
                .setContentTitle(
                        getString(net.lasley.hgdo.R.string.geofence_transition_notification_title,
                                transitionType, ids))
                .setContentText(getString(net.lasley.hgdo.R.string.geofence_transition_notification_text))
                .setContentIntent(notificationPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, builder.build());
    }

    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(net.lasley.hgdo.R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(net.lasley.hgdo.R.string.geofence_transition_exited);
            default:
                return getString(net.lasley.hgdo.R.string.geofence_transition_unknown);
        }
    }
}
