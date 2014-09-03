package net.lasley.hgdo;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Calendar;

public class HGDOService
        extends IntentService {
  public static final String SERVICE_COMM_ACTIVITY    = "net.lasley.hgdo.ACTIVITY";
  public static final String SERVICE_COMM_DATA        = "net.lasley.hgdo.DATA";
  public static final String SERVICE_COMM_INFO        = "net.lasley.hgdo.INFO";
  public static final String SERVICE_COMM_STATE       = "net.lasley.hgdo.COMM_STATE";
  public static final String SERVICE_COMMAND          = "net.lasley.hgdo.COMMAND";
  public static final String SERVICE_START_DOOR_TIMER = "net.lasley.hgdo.DOOR_TIMER";
  public static final String SERVICE_REQ_COMM_INFO    = "net.lasley.hgdo.REQ_INFO";
  public static final String SERVICE_WIFI_SELECTION   = "net.lasley.hgdo.WIFI";
  public static final String SERVICE_WIFI_STATE       = "net.lasley.hgdo.WIFI_STATE";

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
  public static final byte DATAREQ                  = 87;

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

  public static final  String EXTRA_PARAM1    = "net.lasley.hgdo.extra.PARAM1";
  public static final  String EXTRA_PARAM2    = "net.lasley.hgdo.extra.PARAM2";
  private static final int    GARAGE_PORT     = 55555;
  private static final String SERVER_HOSTNAME = hgdoApp.getAppContext().getString(R.string.server_hostname);
  private WifiManager        m_Wifi;
  private WiFiCountDownTimer m_wifiTimer;
  private boolean            m_MonitorWifi;
  private boolean            m_ReadyToMonitorWifi;
  public  byte userFirstInitial;

  public HGDOService() {
    super("HGDOService");
  }

  @Override
  public void onCreate() {
    super.onCreate();
    m_Wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    m_wifiTimer = new WiFiCountDownTimer(HGDOActivity.TIME_TO_WAIT_WIFI, HGDOActivity.TIME_TO_WAIT_WIFI + 1);
    getName();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (intent != null && intent.getAction() != null) {
      getSharedPrefs();
      final String action = intent.getAction();
      Log.d("HGDOService", action);
      if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        NetworkInfo ni = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        String strmsg = "Service: " + action.toString();
        if(ni != null) {
          strmsg += " / " + ni.getState().toString();
        }
        Intent dataIntent = new Intent();
        dataIntent.setAction(HGDOService.SERVICE_COMM_ACTIVITY).putExtra(HGDOService.EXTRA_PARAM1, strmsg);
        LocalBroadcastManager.getInstance(hgdoApp.getAppContext()).sendBroadcast(dataIntent);
        if (ni != null && ni.isConnected()) {
          if (m_ReadyToMonitorWifi) {
            //do stuff
            WifiInfo wi = m_Wifi.getConnectionInfo();
            Log.d("HGDOService", wi.getSSID());
            String ssid = wi.getSSID().trim().replace("\"", "");
            if (ssid.equals("WHITESPRUCE") || ssid.equals("WHITESPRUCE2")) {
              strmsg = "Service: Found HomeNet()";
              dataIntent = new Intent();
              dataIntent.setAction(HGDOService.SERVICE_COMM_ACTIVITY).putExtra(HGDOService.EXTRA_PARAM1, strmsg);
              LocalBroadcastManager.getInstance(hgdoApp.getAppContext()).sendBroadcast(dataIntent);
              if (m_MonitorWifi) {
                commandDoor(HGDOService.OPEN_DOOR);
              }
            }
          }
        } else {
          // wifi connection was lost
        }
      } else if (action.equals(SERVICE_WIFI_SELECTION)) {
        int state = intent.getIntExtra(HGDOService.EXTRA_PARAM1, -1);
        if (state == 1) {
          if (m_Wifi.isWifiEnabled()) {
            m_ReadyToMonitorWifi = true;
          } else {
            m_ReadyToMonitorWifi = false;
            m_Wifi.setWifiEnabled(true);
            if (!m_wifiTimer.busy) {
              m_wifiTimer.startTimer();
            }
          }
          saveSharedPrefs();
        }
      } else if (action.equals(SERVICE_REQ_COMM_INFO)) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(HGDOService.SERVICE_COMM_INFO).putExtra(HGDOService.SERVICE_WIFI_STATE, m_MonitorWifi);
      } else if (action.equals(SERVICE_COMMAND)) {
        byte cmd = intent.getByteExtra(HGDOService.EXTRA_PARAM1, (byte) -1);
        if (cmd == HGDOService.OPEN_DOOR ||
            cmd == HGDOService.CLOSE_DOOR ||
            cmd == HGDOService.TOGGLE_DOOR) {
          commandDoor(cmd);
        } else if (cmd == HGDOService.STATUSREQ) {
          sendStatusRequest();
        }
      }
    }
  }

  public void getName() {
    Cursor c = getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
    int count = c.getCount();
    String[] columnNames = c.getColumnNames();
    boolean b = c.moveToFirst();
    int position = c.getPosition();
    if (count == 1 && position == 0) {
      for (int j = 0; j < columnNames.length; j++) {
        String columnName = columnNames[j];
        if(columnName.equals("display_name")) {
          String columnValue = c.getString(c.getColumnIndex(columnName));
          if (columnValue != null) {
            Log.d("getName()", columnValue);
            userFirstInitial = columnValue.getBytes()[0];
          }
        }
      }
    }
    c.close();
  }

  public byte[] add_suffix(byte[] temp) {
      int len = temp.length;
      Calendar rightNow = Calendar.getInstance();
      temp[len - 4] = userFirstInitial;
      temp[len - 3] = (byte)rightNow.get(Calendar.HOUR_OF_DAY);
      temp[len - 2] = (byte)rightNow.get(Calendar.MINUTE);
      temp[len - 1] = (byte)rightNow.get(Calendar.SECOND);
      return temp;
  }

  public void commandDoor(byte cmd) {
    byte[] msg = new byte[8];

    msg[LENGTH_V1_NDX] = COMMAND_LENGTH_V1;
    msg[VERSION_V1_NDX] = VERSION;
    msg[ACTION_V1_NDX] = COMMAND;
    String strmsg = "Service: commandDoor(";
    if (cmd == HGDOService.OPEN_DOOR) {
      msg[COMMAND_V1_NDX] = OPEN_DOOR;
      strmsg += "open";
    } else if (cmd == HGDOService.CLOSE_DOOR) {
      msg[COMMAND_V1_NDX] = CLOSE_DOOR;
      strmsg += "close";
    } else {
      msg[COMMAND_V1_NDX] = TOGGLE_DOOR;
      strmsg += "toggle";
    }
    msg = add_suffix(msg);
    strmsg += ")";
    Intent dataIntent = new Intent();
    dataIntent.setAction(HGDOService.SERVICE_START_DOOR_TIMER);
    LocalBroadcastManager.getInstance(hgdoApp.getAppContext()).sendBroadcast(dataIntent);

    new AsyncGarage().execute(msg);

    dataIntent = new Intent();
    dataIntent.setAction(HGDOService.SERVICE_COMM_ACTIVITY).putExtra(HGDOService.EXTRA_PARAM1, strmsg);
    LocalBroadcastManager.getInstance(hgdoApp.getAppContext()).sendBroadcast(dataIntent);
  }

  void sendStatusRequest() {
    byte[] msg = new byte[7];

    msg[LENGTH_V1_NDX] = STATUS_REQUEST_LENGTH_V1;
    msg[VERSION_V1_NDX] = VERSION;
    msg[ACTION_V1_NDX] = STATUSREQ;
    msg = add_suffix(msg);
    new AsyncGarage().execute(msg);
    String strmsg = "Service: StatusRequest()";
    Intent dataIntent = new Intent();
    dataIntent.setAction(HGDOService.SERVICE_COMM_ACTIVITY).putExtra(HGDOService.EXTRA_PARAM1, strmsg);
    LocalBroadcastManager.getInstance(hgdoApp.getAppContext()).sendBroadcast(dataIntent);
  }

  void sendDataRequest() {
    byte[] msg = new byte[7];

    msg[LENGTH_V1_NDX] = STATUS_REQUEST_LENGTH_V1;
    msg[VERSION_V1_NDX] = VERSION;
    msg[ACTION_V1_NDX] = DATAREQ;
    msg = add_suffix(msg);
    new AsyncGarage().execute(msg);
    String strmsg = "Service: DataRequest()";
    Intent dataIntent = new Intent();
    dataIntent.setAction(HGDOService.SERVICE_COMM_ACTIVITY).putExtra(HGDOService.EXTRA_PARAM1, strmsg);
    LocalBroadcastManager.getInstance(hgdoApp.getAppContext()).sendBroadcast(dataIntent);
  }

  public void getSharedPrefs() {
    SharedPreferences mPrefs = hgdoApp.getAppContext().getSharedPreferences(getString(R.string.PREFERENCES), MODE_PRIVATE);
    m_MonitorWifi = mPrefs.getBoolean("wifiState", false);
    m_ReadyToMonitorWifi = mPrefs.getBoolean("readyToMonitor", false);
    userFirstInitial = mPrefs.getString("FirstInitial"," ").getBytes()[0];;
  }

  public void saveSharedPrefs() {
    SharedPreferences mPrefs = hgdoApp.getAppContext().getSharedPreferences(getString(R.string.PREFERENCES), MODE_PRIVATE);
    SharedPreferences.Editor ed = mPrefs.edit();
    ed.putBoolean("wifiState", m_MonitorWifi);
    ed.putBoolean("readyToMonitor", m_ReadyToMonitorWifi);
    ed.putString("FirstInitial", Byte.toString(userFirstInitial));
    ed.commit();
  }

  private class AsyncGarage
          extends AsyncTask<byte[], Void, byte[]> {

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
    protected void onPreExecute() {
      super.onPreExecute();
      Intent broadcastIntent = new Intent();
      broadcastIntent.setAction(HGDOService.SERVICE_COMM_STATE).putExtra(HGDOService.EXTRA_PARAM1, View.VISIBLE);
      LocalBroadcastManager.getInstance(hgdoApp.getAppContext()).sendBroadcast(broadcastIntent);
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
    public boolean busy;
    long st;

    public WiFiCountDownTimer(long startTime, long interval) {
      super(startTime, interval);
      busy = false;
    }

    public void startTimer() {
      busy = true;
    }

    @Override
    public void onTick(long millisUntilFinished) {
    }

    @Override
    public void onFinish() {
      m_ReadyToMonitorWifi = true;
      busy = false;
      saveSharedPrefs();
      Log.i("HGDOService", "Ready to watch for WiFi");
    }
  }


}
