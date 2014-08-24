package net.lasley.hgdo;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

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
  public static final String SERVICE_COMM_STATE     = "net.lasley.hgdo.COMM_STATE";

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

  private static final String EXTRA_PARAM1    = "net.lasley.hgdo.extra.PARAM1";
  private static final String EXTRA_PARAM2    = "net.lasley.hgdo.extra.PARAM2";
  private static final int    GARAGE_PORT     = 55555;
  private static final String SERVER_HOSTNAME = hgdoApp.getAppContext().getString(R.string.server_hostname);
  private boolean     m_ReadyToMonitorWifi;
  private WifiManager m_Wifi;
  private boolean     m_MonitorWifi;

  public HGDOService() {
    super("HGDOService");
    m_Wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    m_MonitorWifi = false;
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (intent != null) {
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
              // TODO Get Home Network WiFi requested Intent.
              if (m_MonitorWifi) {
                toggleDoor();
              }
            }
          }
        } else {
          // wifi connection was lost
        }
      }
    }
  }

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
      // TODO Set progress bar intent.
      // m_CommProgressBar.setVisibility(View.VISIBLE);
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
      // TODO Set progress bar intent.
      // m_CommProgressBar.setVisibility(View.INVISIBLE);
      // TODO Send Reply Intent.
      // decodeReply(result);
    }
  }
}
