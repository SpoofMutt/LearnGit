package net.lasley.hgdo;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class HGDOService
        extends IntentService {
  public static final String SERVICE_WIFI_SELECTION = "net.lasley.hgdo.WIFI";
  public static final String SERVICE_COMM_DATA      = "net.lasley.hgdo.DATA";
  public static final String SERVICE_COMM_INFO     = "net.lasley.hgdo.INFO";
  public static final String SERVICE_REQ_COMM_INFO = "net.lasley.hgdo.REQ_INFO";
  public static final String SERVICE_COMM_STATE     = "net.lasley.hgdo.COMM_STATE";
  public static final String SERVICE_WIFI_STATE    = "net.lasley.hgdo.WIFI_STATE";
  public static final String SERVICE_COMMAND       = "net.lasley.hgdo.COMMAND";

  public static final int  LENGTH_V1_NDX            = 0;
  public static final int  VERSION_V1_NDX           = 1;
  public static final int  MSG_VERSION              = 0x01;
  public static final int  ACTION_V1_NDX            = 2;
  public static final int  COMMAND_V1_NDX           = 3;
  public static final int  COMMAND_REPLY_V1_NDX     = 3;
  public static final int  STR_START_V1_NDX         = 3;
  public static final int  STATUS_DOOR_V1_NDX       = 3;
  public static final int  STATUS_LIGHT_V1_NDX      = 4;
  public static final int  STATUS_RANGE_V1_NDX      = 5;
  // Version 1 Lengths
  public static final int  COMMAND_LENGTH_V1        = 8;
  public static final int  COMMAND_REPLY_LENGTH_V1  = 8;
  public static final int  STATUS_REQUEST_LENGTH_V1 = 7;
  public static final int  STATUS_REPLY_LENGTH_V1   = 10;
  public static final byte VERSION                  = 1;
  public static final byte STRING                   = 16;
  public static final byte COMMAND                  = 10;
  public static final byte COMMANDREPLY             = 45;
  public static final byte STATUSREQ                = 21;
  public static final byte STATUSREPLY              = 33;
  public static final byte DOOR_CLOSED              = 0;
  public static final byte DOOR_OPEN                = 1;
  public static final byte DOOR_OPENING             = 2;
  public static final byte DOOR_CLOSING             = 3;
  public static final byte DOOR_BUSY                = 4;
  public static final byte LIGHT_OFF                = 0;
  public static final byte LIGHT_ON                 = 1;
  public static final byte OPEN_DOOR                = 0;
  public static final byte CLOSE_DOOR               = 1;
  public static final byte TOGGLE_DOOR              = 2;

  public static final String EXTRA_PARAM1 = "net.lasley.hgdo.extra.PARAM1";
  public static final String EXTRA_PARAM2 = "net.lasley.hgdo.extra.PARAM2";
  private static final int    GARAGE_PORT     = 55555;
  private static final String SERVER_HOSTNAME = hgdoApp.getAppContext().getString(R.string.server_hostname);
  private boolean     m_ReadyToMonitorWifi;
  private WifiManager m_Wifi;
  private WiFiCountDownTimer m_wifiTimer;
  private boolean     m_MonitorWifi;

  public HGDOService() {
    super("HGDOService");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    unregisterReceiver(receiver);
  }

  @Override
  public void onCreate() {
    super.onCreate();

    IntentFilter filter = new IntentFilter();
    filter.addAction("android.net.wifi.STATE_CHANGE");
    filter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
    filter.addAction("android.intent.action.BATTERY_LOW");
    filter.addAction("android.intent.action.BOOT_COMPLETED");
    filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
    filter.addAction("android.net.wifi.supplicant.CONNECTION_CHANGE");
    filter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
    registerReceiver(receiver, filter);

    m_MonitorWifi = false;
    m_Wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    m_wifiTimer = new WiFiCountDownTimer(HGDOActivity.TIME_TO_WAIT_WIFI, HGDOActivity.TIME_TO_WAIT_WIFI + 1);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (intent != null && intent.getAction() != null) {
      final String action = intent.getAction();
      Log.d("HGDOService", action);
      if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        NetworkInfo ni = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (ni.isConnected()) {
          if (m_ReadyToMonitorWifi) {
            //do stuff
            WifiInfo wi = m_Wifi.getConnectionInfo();
            Log.d("HGDOService", wi.getSSID());
            String ssid = wi.getSSID().trim().replace("\"", "");
            if (ssid.equals("WHITESPRUCE") || ssid.equals("WHITESPRUCE2")) {
              if (m_MonitorWifi) {
                toggleDoor();
              }
            }
          }
        } else {
          // wifi connection was lost
        }
      } else if (action.equals(SERVICE_WIFI_SELECTION)) {
        int state = intent.getIntExtra(HGDOService.EXTRA_PARAM1, -1);
        if (state == 0) {
          m_MonitorWifi = false;
        } else if (state == 1) {
          if (m_Wifi.isWifiEnabled()) {
            m_MonitorWifi = true;
          } else {
            m_MonitorWifi = false;
            m_Wifi.setWifiEnabled(true);
            m_wifiTimer.start();
          }
        }
      } else if (action.equals(SERVICE_REQ_COMM_INFO)) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(HGDOService.SERVICE_COMM_INFO).putExtra(HGDOService.SERVICE_WIFI_STATE, m_MonitorWifi);
      } else if (action.equals(SERVICE_COMMAND)) {
        byte cmd = intent.getByteExtra(HGDOService.EXTRA_PARAM1, (byte) -1);
        if (cmd == HGDOService.OPEN_DOOR) {
        } else if (cmd == HGDOService.CLOSE_DOOR) {
          toggleDoor();
        } else if (cmd == HGDOService.TOGGLE_DOOR) {
          toggleDoor();
        } else if (cmd == HGDOService.STATUSREQ) {
          sendStatusRequest();
        }
      }
    }
  }

  private final BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      onHandleIntent(intent);
    }
  };

  public void toggleDoor() {
    byte[] msg = new byte[8];
    msg[LENGTH_V1_NDX] = COMMAND_LENGTH_V1;
    msg[VERSION_V1_NDX] = VERSION;
    msg[ACTION_V1_NDX] = COMMAND;
    msg[COMMAND_V1_NDX] = TOGGLE_DOOR;
    msg[COMMAND_LENGTH_V1 - 4] = 1;
    msg[COMMAND_LENGTH_V1 - 3] = 2;
    msg[COMMAND_LENGTH_V1 - 2] = 3;
    msg[COMMAND_LENGTH_V1 - 1] = 4;
    new AsyncGarage().execute(msg);
  }

  void sendStatusRequest() {
    byte[] msg = new byte[7];
    msg[LENGTH_V1_NDX] = STATUS_REQUEST_LENGTH_V1;
    msg[VERSION_V1_NDX] = VERSION;
    msg[ACTION_V1_NDX] = STATUSREQ;
    msg[STATUS_REQUEST_LENGTH_V1 - 4] = 1;
    msg[STATUS_REQUEST_LENGTH_V1 - 3] = 2;
    msg[STATUS_REQUEST_LENGTH_V1 - 2] = 3;
    msg[STATUS_REQUEST_LENGTH_V1 - 1] = 4;
    new AsyncGarage().execute(msg);
  }

  private class AsyncGarage
          extends AsyncTask<byte[], Void, byte[]> {

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      Intent broadcastIntent = new Intent();
      broadcastIntent.setAction(HGDOService.SERVICE_COMM_STATE).putExtra(HGDOService.EXTRA_PARAM1, View.VISIBLE);
      LocalBroadcastManager.getInstance(hgdoApp.getAppContext()).sendBroadcast(broadcastIntent);
    }

    @Override
    protected byte[] doInBackground(byte[]... outbuffers) {
      Socket nsocket = new Socket();   //Network Socket
      byte[] tempdata = new byte[20];

      try {
        ConnectivityManager cm =
                (ConnectivityManager) hgdoApp.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        while (!ni.isConnected()) {
          ni = cm.getActiveNetworkInfo();
        }
        Log.i("HGDOService", "Creating socket");
        SocketAddress sockaddr = new InetSocketAddress(SERVER_HOSTNAME, GARAGE_PORT);
        nsocket = new Socket();
        nsocket.connect(sockaddr, 5000); //10 second connection timeout
        if (nsocket.isConnected()) {
          InputStream nis = nsocket.getInputStream(); //Network Input Stream
          OutputStream nos = nsocket.getOutputStream(); //Network Output Stream

          nos.write(outbuffers[0]);

          byte[] buffer = new byte[4096];
          int read = nis.read(buffer, 0, 4096); //This is blocking
          tempdata = new byte[read];
          System.arraycopy(buffer, 0, tempdata, 0, read);
        }
      } catch (IOException e) {
        e.printStackTrace();
        Log.i("HGDOService", "IOException");
      } catch (Exception e) {
        e.printStackTrace();
        Log.i("HGDOService", "Exception");
      } finally {
        try {
          nsocket.close();
        } catch (IOException e) {
          e.printStackTrace();
        } catch (Exception e) {
          e.printStackTrace();
        }
        Log.i("HGDOService", "Finished");
      }
      return tempdata;
    }

    @Override
    protected void onPostExecute(byte[] result) {
      super.onPostExecute(result);
      Intent broadcastIntent = new Intent();
      broadcastIntent.setAction(HGDOService.SERVICE_COMM_STATE).putExtra(HGDOService.EXTRA_PARAM1, View.INVISIBLE);
      LocalBroadcastManager.getInstance(hgdoApp.getAppContext()).sendBroadcast(broadcastIntent);
      Intent dataIntent = new Intent();
      dataIntent.setAction(HGDOService.SERVICE_COMM_DATA).putExtra(HGDOService.EXTRA_PARAM1, result);
      LocalBroadcastManager.getInstance(hgdoApp.getAppContext()).sendBroadcast(dataIntent);
    }
  }

  public class WiFiCountDownTimer
          extends CountDownTimer {
    long st;

    public WiFiCountDownTimer(long startTime, long interval) {
      super(startTime, interval);
    }

    @Override
    public void onTick(long millisUntilFinished) {
    }

    @Override
    public void onFinish() {
      m_ReadyToMonitorWifi = true;
      Log.i("HGDOService", "Ready to watch for WiFi");
    }
  }


}
