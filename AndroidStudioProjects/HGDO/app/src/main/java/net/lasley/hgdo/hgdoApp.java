package net.lasley.hgdo;

import android.app.Application;
import android.content.Context;

/**
 * Created by Kent on 8/23/2014.
 */
public class hgdoApp extends Application {

    private static Context context;

    public static Context getAppContext() {
        return hgdoApp.context;
    }

    public void onCreate() {
        super.onCreate();
        hgdoApp.context = getApplicationContext();
    }
}
