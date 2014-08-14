#include <Adafruit_CC3000.h>
#include <ccspi.h>
#include <SPI.h>
#include <Time.h>
#include <Timezone.h>
//#include "utility/debug.h"
#include "utility/socket.h"
//#include "crc.h";

// These are the interrupt and control pins
#define ADAFRUIT_CC3000_IRQ   3  // MUST be an interrupt pin!
// These can be any two pins
#define ADAFRUIT_CC3000_VBAT  5
#define ADAFRUIT_CC3000_CS    10
// Use hardware SPI for the remaining pins
// On an UNO, SCK = 13, MISO = 12, and MOSI = 11
Adafruit_CC3000 cc3000 = Adafruit_CC3000(ADAFRUIT_CC3000_CS, ADAFRUIT_CC3000_IRQ, ADAFRUIT_CC3000_VBAT,
                                         SPI_CLOCK_DIVIDER); // you can change this clock speed but DI

#define WLAN_SSID       "WHITESPRUCE2"        // cannot be longer than 32 characters!
#define WLAN_PASS       "underdog"

// Security can be WLAN_SEC_UNSEC, WLAN_SEC_WEP, WLAN_SEC_WPA or WLAN_SEC_WPA2
#define WLAN_SECURITY   WLAN_SEC_WPA2
#define GARAGE_PORT 55555
Adafruit_CC3000_Client client;
Adafruit_CC3000_Server server(GARAGE_PORT);

TimeChangeRule myDST = {"MDT", Second, Sun, Mar, 2, -360};    //Daylight time = UTC - 6 hours
TimeChangeRule mySTD = {"MST", First, Sun, Nov, 2, -420};     //Standard time = UTC - 7 hours
Timezone myTZ(myDST, mySTD);
time_t utc, local;
TimeChangeRule *tcr;        //pointer to the time change rule, use to get TZ abbrev

// Command Data
// 1 byte length
// 1 byte Version = 0x01
// 1 action byte
// command byte, status 1 byte , string...
//               status 2 byte   ....
// 4 byte crc - not used right now.

#define LENGTH_V1_NDX  0
#define VERSION_V1_NDX 1
#define VERSION        data[VERSION_V1_NDX] = 0x01
#define ACTION_V1_NDX  2
#define COMMAND_V1_NDX 3
#define COMMAND_REPLY_V1_NDX 3
#define STR_START_V1_NDX 3
#define STATUS1_V1_NDX 3
#define STATUS2_V1_NDX 4

// Version 1 Lengths
#define COMMAND_LENGTH_V1 data[LENGTH_V1_NDX]         = 8
#define COMMAND_REPLY_LENGTH_V1 data[LENGTH_V1_NDX]   = 8
#define STATUS_REQUEST_LENGTH_V1  data[LENGTH_V1_NDX] = 7
#define STATUS_REPLY_LENGTH_V1  data[LENGTH_V1_NDX]   = 9

// Action Byte
#define COMMAND     10
#define COMMANDREPLY 45
#define STATUSREQ   21
#define STATUSREPLY 33
#define STRING      16

// Status Byte1
#define DOOR_CLOSED  0
#define DOOR_OPENED  1
#define DOOR_OPENING 2
#define DOOR_CLOSING 3

// Status Byte2
#define LIGHT_OFF   0
#define LIGHT_ON    1

// Command Byte
#define OPEN_DOOR   0
#define CLOSE_DOOR  1

#define TIME_TO_OPEN 30 // Seconds
unsigned long time_of_last_door_command = 0;
uint8_t       last_door_command;

const unsigned long
  connectTimeout  = 15L * 1000L, // Max time to wait for server connection
  responseTimeout = 15L * 1000L; // Max time to wait for data from server
unsigned long
  lastPolledTime  = 0L; // Last value retrieved from time server

void setup(void)
{
  Serial.begin(9600);
  Serial.println(F("Garage Here!\n")); 

  Serial.println(F("\nInitialising the CC3000 ..."));
  if (!cc3000.begin()) {
    Serial.println(F("Unable to initialise the CC3000! Check your wiring?"));
    for(;;);
  }

  displayMACAddress();
  
  Serial.println(F("\nDeleting old connection profiles"));
  if (!cc3000.deleteProfiles()) {
    Serial.println(F("Failed!"));
    while(1);
  }

  /* Optional: Set a static IP address instead of using DHCP.
     Note that the setStaticIPAddress function will save its state
     in the CC3000's internal non-volatile memory and the details
     will be used the next time the CC3000 connects to a network.
     This means you only need to call the function once and the
     CC3000 will remember the connection details.  To switch back
     to using DHCP, call the setDHCP() function (again only needs
     to be called once).
  */
  
  uint32_t ipAddress = cc3000.IP2U32(192, 168, 1, 254);
  uint32_t netMask = cc3000.IP2U32(255, 255, 255, 0);
  uint32_t defaultGateway = cc3000.IP2U32(192, 168, 1, 1);
  uint32_t dns = cc3000.IP2U32(192, 168, 1, 1);
  if (!cc3000.setStaticIPAddress(ipAddress, netMask, defaultGateway, dns)) {
    Serial.println(F("Failed to set static IP!"));
    while(1);
  }
  
  /* Attempt to connect to an access point */
  char *ssid = WLAN_SSID;             /* Max 32 chars */
  Serial.print(F("\nAttempting to connect to ")); Serial.println(ssid);
  
  /* NOTE: Secure connections are not available in 'Tiny' mode! */
  if (!cc3000.connectToAP(WLAN_SSID, WLAN_PASS, WLAN_SECURITY)) {
    Serial.println(F("Failed!"));
    while(1);
  }
   
  Serial.println(F("Connected!"));
  
  /* Wait for DHCP to complete */
  Serial.println(F("Request DHCP"));
  while (!cc3000.checkDHCP()) {
    delay(100); // ToDo: Insert a DHCP timeout!
  }

  /* Display the IP address DNS, Gateway, etc. */  
  while (!displayConnectionDetails()) {
    delay(1000);
  }
  server.begin();
  
  setSyncProvider(getServerTime);
  setSyncInterval(24*60*60);
  while(timeStatus() == timeNotSet) {
    Serial.println("Waiting 5 secs to try again.");
    delay(5000L);
    now();
  }
  Serial.println(F("Listening..."));
}


// To reduce load on NTP servers, time is polled once per roughly 24 hour period.
// Otherwise use millis() to estimate time since last query.  Plenty accurate.
void loop(void) {

  utc = now();
  if(utc-lastPolledTime > 15) {
    local = myTZ.toLocal(utc, &tcr);
    printTime(local, tcr -> abbrev);
    lastPolledTime = utc;
  }
  delay(1000);
  
  // Try to get a client which is connected.
  Serial.print(utc);
  Serial.println(":  Looking for clients.");
  Adafruit_CC3000_ClientRef client = server.available();
  if (client) {
     Serial.println("Found client");
     // Check if there is data available to read.
     if (client.available() > 0) {
       // Read a byte and write it to all clients.
       uint8_t data[80];
       int size_read = client.read(data,80,0);

       if(size_read == data[LENGTH_V1_NDX]) {
         Serial.println(size_read);
         if(data[ACTION_V1_NDX] == STRING) {
           VERSION;
           data[data[LENGTH_V1_NDX]-4] = 1;           // Bogus CRC
           data[data[LENGTH_V1_NDX]-3] = 2;
           data[data[LENGTH_V1_NDX]-2] = 3;
           data[data[LENGTH_V1_NDX]-1] = 4;
           server.write(data,data[LENGTH_V1_NDX]);   // Just echo string.
         } else if(data[ACTION_V1_NDX] == COMMAND) { // Door command.
           if(time_of_last_door_command + TIME_TO_OPEN < now()) {
             COMMAND_REPLY_LENGTH_V1;
             VERSION;
             data[ACTION_V1_NDX] = COMMANDREPLY;
             if(data[COMMAND_V1_NDX] == OPEN_DOOR) {
               data[COMMAND_REPLY_V1_NDX] = DOOR_OPENING;
             } else if(data[COMMAND_V1_NDX] == CLOSE_DOOR) {
               data[COMMAND_REPLY_V1_NDX] = DOOR_CLOSING;
             }
             data[data[LENGTH_V1_NDX]-4] = 1;           // Bogus CRC
             data[data[LENGTH_V1_NDX]-3] = 2;
             data[data[LENGTH_V1_NDX]-2] = 3;
             data[data[LENGTH_V1_NDX]-1] = 4;
             server.write(data,data[data[LENGTH_V1_NDX]]); // Send reply
             time_of_last_door_command = now();
             last_door_command = data[COMMAND_V1_NDX];
           } else {
             if(last_door_command == data[COMMAND_V1_NDX]) {
               COMMAND_REPLY_LENGTH_V1;
               data[ACTION_V1_NDX] = COMMANDREPLY;
               if(last_door_command == OPEN_DOOR) {
                 data[COMMAND_REPLY_V1_NDX] = DOOR_OPENING;
               } else {
                 data[COMMAND_REPLY_V1_NDX] = DOOR_CLOSING;
               }
               data[data[LENGTH_V1_NDX]-4] = 1;           // Bogus CRC
               data[data[LENGTH_V1_NDX]-3] = 2;
               data[data[LENGTH_V1_NDX]-2] = 3;
               data[data[LENGTH_V1_NDX]-1] = 4;
               server.write(data,data[LENGTH_V1_NDX]);
             } else {
               int time_to_wait = (TIME_TO_OPEN + time_of_last_door_command) - now();
               COMMAND_REPLY_LENGTH_V1;
               data[ACTION_V1_NDX] = COMMANDREPLY;
               if(last_door_command == OPEN_DOOR) {
                 data[COMMAND_REPLY_V1_NDX] = DOOR_OPENING;
               } else {
                 data[COMMAND_REPLY_V1_NDX] = DOOR_CLOSING;
               }
               data[data[LENGTH_V1_NDX]-4] = 1;           // Bogus CRC
               data[data[LENGTH_V1_NDX]-3] = 2;
               data[data[LENGTH_V1_NDX]-2] = 3;
               data[data[LENGTH_V1_NDX]-1] = 4;
               server.write(data,data[LENGTH_V1_NDX]);
             }
           }
         } else if(data[ACTION_V1_NDX] == STATUSREQ) {
           STATUS_REPLY_LENGTH_V1;
           data[ACTION_V1_NDX] = STATUSREPLY;
           data[STATUS1_V1_NDX] = 5; // Bogus data
           data[STATUS2_V1_NDX] = 6;
           data[data[LENGTH_V1_NDX]-4] = 1;           // Bogus CRC
           data[data[LENGTH_V1_NDX]-3] = 2;
           data[data[LENGTH_V1_NDX]-2] = 3;
           data[data[LENGTH_V1_NDX]-1] = 4;
           server.write(data,data[LENGTH_V1_NDX]);
         }
       } else {
         Serial.print("Bad Read: ");
         Serial.print(size_read);
         Serial.print(" vs ");
         Serial.println(data[LENGTH_V1_NDX]);
         VERSION;
         data[ACTION_V1_NDX] = STRING;
         data[STR_START_V1_NDX]   = 'B';
         data[STR_START_V1_NDX+1] = 'a';
         data[STR_START_V1_NDX+2] = 'd';
         data[STR_START_V1_NDX+3] = ' ';
         data[STR_START_V1_NDX+4] = 'R';
         data[STR_START_V1_NDX+5] = 'e';
         data[STR_START_V1_NDX+6] = 'a';
         data[STR_START_V1_NDX+7] = 'd';
         data[STR_START_V1_NDX+8] = 1;           // Bogus CRC
         data[STR_START_V1_NDX+9] = 2;
         data[STR_START_V1_NDX+10] = 3;
         data[STR_START_V1_NDX+11] = 4;
         data[LENGTH_V1_NDX] = STR_START_V1_NDX+12;
         server.write(data,data[LENGTH_V1_NDX]);   // Just echo string.

         server.write("Bad Read");
       }
     }
  }
}


//Function to print time with time zone
void printTime(time_t t, char *tz)
{
    sPrintI00(hour(t));
    sPrintDigits(minute(t));
    sPrintDigits(second(t));
    Serial.print(' ');
    Serial.print(dayShortStr(weekday(t)));
    Serial.print(' ');
    sPrintI00(day(t));
    Serial.print(' ');
    Serial.print(monthShortStr(month(t)));
    Serial.print(' ');
    Serial.print(year(t));
    Serial.print(' ');
    Serial.print(tz);
    Serial.println();
}

//Print an integer in "00" format (with leading zero).
//Input value assumed to be between 0 and 99.
void sPrintI00(int val)
{
    if (val < 10) Serial.print('0');
    Serial.print(val, DEC);
    return;
}

//Print an integer in ":00" format (with leading zero).
//Input value assumed to be between 0 and 99.
void sPrintDigits(int val)
{
    Serial.print(':');
    if(val < 10) Serial.print('0');
    Serial.print(val, DEC);
}

/**************************************************************************/
/*!
    @brief  Tries to read the 6-byte MAC address of the CC3000 module
*/
/**************************************************************************/
void displayMACAddress(void)
{
  uint8_t macAddress[6];
  
  if(!cc3000.getMacAddress(macAddress))
  {
    Serial.println(F("Unable to retrieve MAC Address!\r\n"));
  }
  else
  {
    Serial.print(F("MAC Address : "));
    cc3000.printHex((byte*)&macAddress, 6);
  }
}


/**************************************************************************/
/*!
    @brief  Tries to read the IP address and other connection details
*/
/**************************************************************************/
bool displayConnectionDetails(void)
{
  uint32_t ipAddress, netmask, gateway, dhcpserv, dnsserv;
  
  if(!cc3000.getIPAddress(&ipAddress, &netmask, &gateway, &dhcpserv, &dnsserv))
  {
    Serial.println(F("Unable to retrieve the IP Address!\r\n"));
    return false;
  }
  else
  {
    Serial.print(F("\nIP Addr: ")); cc3000.printIPdotsRev(ipAddress);
    Serial.println();
    return true;
  }
}

// Minimalist time server query; adapted from Adafruit Gutenbird sketch,
// which in turn has roots in Arduino UdpNTPClient tutorial.
unsigned long getServerTime(void) {

  uint8_t       buf[48];
  unsigned long ip, startTime, t = 0L;

  Serial.print(F("Locating time server..."));

  // Hostname to IP lookup; use NTP pool (rotates through servers)
  if(cc3000.getHostByName("pool.ntp.org", &ip)) {
    static const char PROGMEM
      timeReqA[] = { 227,  0,  6, 236 },
      timeReqB[] = {  49, 78, 49,  52 };

    Serial.println(F("\r\nAttempting connection..."));
    startTime = millis();
    do {
      client = cc3000.connectUDP(ip, 123);
    } while((!client.connected()) &&
            ((millis() - startTime) < connectTimeout));

    if(client.connected()) {
      Serial.print(F("connected!\r\nIssuing request..."));

      // Assemble and issue request packet
      memset(buf, 0, sizeof(buf));
      memcpy_P( buf    , timeReqA, sizeof(timeReqA));
      memcpy_P(&buf[12], timeReqB, sizeof(timeReqB));
      client.write(buf, sizeof(buf));

      Serial.print(F("\r\nAwaiting response..."));
      memset(buf, 0, sizeof(buf));
      startTime = millis();
      int count = 1;
      while((!client.available()) &&
            ((millis() - startTime) < responseTimeout)) {
           if((millis() - startTime) > 5000*count) {
              count++;
              Serial.print("Client availibility is ");
              Serial.print(client.available()); Serial.println();
              Serial.print("Elapsed time is: ");
              Serial.print((millis()-startTime)); Serial.println();
              Serial.print("Timeout is     : ");
              Serial.print(responseTimeout); Serial.println();
            }
      }
      if(client.available()) {
        client.read(buf, sizeof(buf));
        t = (((unsigned long)buf[40] << 24) |
             ((unsigned long)buf[41] << 16) |
             ((unsigned long)buf[42] <<  8) |
              (unsigned long)buf[43]) - 2208988800UL;
        Serial.print(F("OK\r\n"));
      }
      client.close();
    }
  }
  if(!t) Serial.println(F("error"));
  return t;
}
