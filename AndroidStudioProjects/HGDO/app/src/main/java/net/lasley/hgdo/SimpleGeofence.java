package net.lasley.hgdo;

/**
 * Created by Kent on 8/20/2014.
 */

import com.google.android.gms.location.Geofence;

class SimpleGeofence {
  private final String mId;
  private final double mLatitude;
  private final double mLongitude;
  private final float  mRadius;
  private       long   mExpirationDuration;
  private       int    mTransitionType;

  public SimpleGeofence(String geofenceId, double latitude, double longitude, float radius, long expiration,
                        int transition) {
    this.mId = geofenceId;
    this.mLatitude = latitude;
    this.mLongitude = longitude;
    this.mRadius = radius;
    this.mExpirationDuration = expiration;
    this.mTransitionType = transition;
  }

  public long getExpirationDuration() {
    return mExpirationDuration;
  }

  public int getTransitionType() {
    return mTransitionType;
  }

  public Geofence toGeofence() {
    return new Geofence.Builder().setRequestId(getId()).setTransitionTypes(mTransitionType).setNotificationResponsiveness(
            0).setCircularRegion(getLatitude(), getLongitude(), getRadius()).setExpirationDuration(
            mExpirationDuration).build();
  }

  String getId() {
    return mId;
  }

  double getLatitude() {
    return mLatitude;
  }

  double getLongitude() {
    return mLongitude;
  }

  float getRadius() {
    return mRadius;
  }
}
