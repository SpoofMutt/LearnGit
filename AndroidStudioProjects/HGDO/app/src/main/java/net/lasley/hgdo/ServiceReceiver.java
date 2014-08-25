package net.lasley.hgdo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServiceReceiver
        extends BroadcastReceiver {
  public ServiceReceiver() {
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    Intent broadcastIntent = new Intent();
    broadcastIntent = (Intent) intent.clone();
    broadcastIntent.setClass(hgdoApp.getAppContext(), HGDOService.class);
    hgdoApp.getAppContext().startService(broadcastIntent);
  }
}
