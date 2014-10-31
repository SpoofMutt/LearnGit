#include <Ultrasonic.h>
#include <Adafruit_CC3000.h>
#include <ccspi.h>
#include <SPI.h>
#include <Time.h>
#include <Timezone.h>
//#include "utility/debug.h"
#include "utility/socket.h"
//#include "crc.h";

//#define ENABLE_NTP    1
//#define EMULATOR_MODE 1
#define ENABLE_DEBUG_LEVEL  1 
#define ENABLE_DEVUG_LEVEL_1 1
#define ENABLE_DEVUG_LEVEL_2 1
#define ENABLE_DEVUG_LEVEL_3 1
//#define ENABLE_DEVUG_LEVEL_4 1
//#define ENABLE_DEVUG_LEVEL_5 1

int lightPin = 0;  //define a pin for Photo resistor

// Sonic Ranging
#define TRIGGER_PIN  12 // Move this to 8 or 6...
#define ECHO_PIN     9

// Door opener relay
#define RELAY_PIN     7

// LED status pins
#define LED1  8
#define LED2  15

#define ADAFRUIT_CC3000_IRQ    3  // MUST be an interrupt pin!
#define ADAFRUIT_CC3000_VBAT   5  // These can be any pins
#define ADAFRUIT_CC3000_CS     10 // These can be any pins
#define ADAFRUIT_MOSI          11 //
#define ADAFRUIT_MISO          12 // MISO
#define ADAFRUIT_SCK           13 // Use hardware SPI for the remaining pins. On an UNO, SCK = 13, MISO = 12, and MOSI = 11


#define WLAN_SSID       "WHITESPRUCE2"        // cannot be longer than 32 characters!
#define WLAN_PASS       "underdog"

// Security can be WLAN_SEC_UNSEC, WLAN_SEC_WEP, WLAN_SEC_WPA or WLAN_SEC_WPA2
#define WLAN_SECURITY   WLAN_SEC_WPA2
#define GARAGE_PORT 55555
Adafruit_CC3000_Client client;
Adafruit_CC3000_Server server(GARAGE_PORT);

/*
Duration to close the switch on the door opener. This should be long
enough for the mechanism to start; typically it doesn't to remain 
activated for the door to complete its motion. It is the same as the
time you'd hold down the button to start the door moving. 
*/
#define DOOR_ACTIVATION_PERIOD 600 // [ms]

Ultrasonic ultrasonic(TRIGGER_PIN, ECHO_PIN);

Adafruit_CC3000 cc3000 = Adafruit_CC3000(ADAFRUIT_CC3000_CS, ADAFRUIT_CC3000_IRQ, ADAFRUIT_CC3000_VBAT,
SPI_CLOCK_DIVIDER); // you can change this clock speed but DI

TimeChangeRule myDST = {"MDT", Second, Sun, Mar, 2, -360};    //Daylight time = UTC - 6 hours
TimeChangeRule mySTD = {"MST", First, Sun, Nov, 2, -420};     //Standard time = UTC - 7 hours
Timezone myTZ(myDST, mySTD);
time_t utc, local;
TimeChangeRule *tcr;        //pointer to the time change rule, use to get TZ abbrev
long range_read;

// Command Data
// 1 byte length
// 1 byte Version = 0x01
// 1 action byte
// command byte, door  status 1 byte , string...
//               light status 2 byte   ....
//               range status 3 byte
// 4 byte crc - not used right now.

#define LENGTH_V1_NDX  0
#define VERSION_V1_NDX 1
#define VERSION        data[VERSION_V1_NDX] = 0x01
#define ACTION_V1_NDX  2
#define COMMAND_V1_NDX 3
#define COMMAND_REPLY_V1_NDX 3
#define STR_START_V1_NDX 3
#define STATUS_DOOR_V1_NDX 3
#define STATUS_LIGHT_V1_NDX 4
#define STATUS_RANGE_V1_NDX 5

// Version 1 Lengths
#define COMMAND_LENGTH_V1        data[LENGTH_V1_NDX] = 8
#define COMMAND_REPLY_LENGTH_V1  data[LENGTH_V1_NDX] = 8
#define STATUS_REQUEST_LENGTH_V1 data[LENGTH_V1_NDX] = 7
#define DATA_REQUEST_LENGTH_V1   data[LENGTH_V1_NDX] = 7
#define STATUS_REPLY_LENGTH_V1   data[LENGTH_V1_NDX] = 10
// Not including STRING commands.
#define MAX_SIZE                                       10 
#define HISTORY_SIZE                                   20
// Msg Prefix, body, postfix
#define DATA_SIZE                                      (HISTORY_SIZE*MAX_SIZE+3+4)
// Action Byte
#define COMMAND     10
#define COMMANDREPLY 45
#define STATUSREQ   21
#define STATUSREPLY 33
#define STRING      16
#define DATAREQ     87

// Status Byte1
#define DOOR_CLOSED  0
#define DOOR_OPEN    1
#define DOOR_OPENING 2
#define DOOR_CLOSING 3
#define DOOR_BUSY    4

// Status Byte2
#define LIGHT_OFF   0
#define LIGHT_ON    1

// Command Byte
#define OPEN_DOOR   0
#define CLOSE_DOOR  1
#define TOGGLE_DOOR 2

#define TIME_TO_OPEN 15 // Seconds
unsigned long time_of_last_door_command = 0;
uint8_t       last_door_command = CLOSE_DOOR;
uint8_t       data[DATA_SIZE]; 
int   toggle = 0;
float inMsec;

#ifdef EMULATOR_MODE
int door_state  = DOOR_CLOSED;
int light_state = LIGHT_OFF;
#endif

uint8_t next_ndx = 0;
uint8_t saved_commands[HISTORY_SIZE][MAX_SIZE];

const unsigned long responseTimeout = 5L * 1000L; // Max time to wait for data from server
const unsigned long connectTimeout  = 5L * 1000L; // Max time to wait for server connection

void setup(void)
{
#ifdef ENABLE_DEBUG_LEVEL
  Serial.begin(9600);
  Serial.println(F("Garage Here!"));
  Serial.println(F("Configuring pinouts."));
#endif

  SetupDoorControl();

#if defined (ENABLE_DEBUG_LEVEL) && defined (ENABLE_DEBUG_LEVEL_4)
  Serial.println(F("\nInitialising the CC3000 ..."));
#endif

  if (!cc3000.begin()) {
#ifdef ENABLE_DEBUG_LEVEL
    Serial.println(F("Unable to initialise the CC3000! Check your wiring?"));
#endif
    for(;;);
  }

  displayMACAddress();

#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
  Serial.println(F("\nDeleting old connection profiles"));
#endif

  if (!cc3000.deleteProfiles()) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
    Serial.println(F("Failed!"));
#endif

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
#if defined ENABLE_DEBUG_LEVEL
    Serial.println(F("Failed to set static IP!"));
#endif

    while(1);
  }

  /* Attempt to connect to an access point */
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
  char *ssid = WLAN_SSID;             /* Max 32 chars */
  Serial.print(F("\nAttempting to connect to ")); 
  Serial.println(ssid);
#endif


  /* NOTE: Secure connections are not available in 'Tiny' mode! */
  if (!cc3000.connectToAP(WLAN_SSID, WLAN_PASS, WLAN_SECURITY)) {
#if defined ENABLE_DEBUG_LEVEL
    Serial.println(F("Failed Secure Connection!"));
#endif

    while(1);
  }

#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
  Serial.println(F("Connected!"));
  Serial.println(F("Request DHCP"));
#endif


  /* Wait for DHCP to complete */
  while (!cc3000.checkDHCP()) {
    delay(100); // ToDo: Insert a DHCP timeout!
  }

  /* Display the IP address DNS, Gateway, etc. */  
  while (!displayConnectionDetails()) {
    delay(1000);
  }
  server.begin();

#ifdef ENABLE_NTP
  setSyncProvider(getServerTime);
  setSyncInterval(24*60*60);
  int count = 0;
  while(timeStatus() == timeNotSet && count < 3) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
    Serial.println("Waiting 5 secs to try again.");
#endif

    delay(5000L);
    now();
    count++;
  }
#endif

#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
  Serial.println(F("Empty history buffer"));
#endif

  for(int x = 0; x < HISTORY_SIZE; x++) {
    for(int y = 0; y < MAX_SIZE; y++) {
      saved_commands[x][y] = 0;
    }
  }
#if defined ENABLE_DEBUG_LEVEL
  Serial.println(F("Listening..."));
#endif

}


// To reduce load on NTP servers, time is polled once per roughly 24 hour period.
// Otherwise use millis() to estimate time since last query.  Plenty accurate.
void loop(void) {
  // Try to get a client which is connected.
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
  Serial.println(":  Looking for clients.");
#endif

  //  if (client) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
  Serial.print("Toggle: ");
  Serial.println(toggle);
#endif

  if (toggle) {
    Adafruit_CC3000_ClientRef client = server.available();
    
    if(client) {
      utc = now();
      local = myTZ.toLocal(utc, &tcr);
      printTime(local, tcr -> abbrev);
#if defined ENABLE_DEBUG_LEVEL
      Serial.println("Found client");
#endif

      // Check if there is data available to read.
      if (client.available() > 0) {
        digitalWrite(LED2, HIGH);
        // Read a byte and write it to all clients.
        int size_read = client.read(data,80,0);
        digitalWrite(LED2, LOW);
        if(size_read == data[LENGTH_V1_NDX]) {
          if(data[ACTION_V1_NDX] != STRING) {
            for(int y = 0; y < MAX_SIZE; y++) {
              if(y < size_read) {
                saved_commands[next_ndx][y] = data[y];
              } else {
                saved_commands[next_ndx][y] = 0;
              }
            }
            next_ndx++;
            if(next_ndx >= HISTORY_SIZE) {
              next_ndx = 0;
            }
          }
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
          Serial.print("Size: ");
          Serial.println(data[LENGTH_V1_NDX]);
          Serial.print("Version: ");
          Serial.println(data[VERSION_V1_NDX]);
          Serial.print("Action: ");
#endif
          if(data[ACTION_V1_NDX] == STRING) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
            Serial.println("STRING");
#endif
            VERSION;
            sendData(); // Just echo
          } else if(data[ACTION_V1_NDX] == COMMAND) { // Door command.
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
            Serial.println("COMMAND");
            Serial.print("Order: ");
#endif
            if(data[COMMAND_V1_NDX] == OPEN_DOOR) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
              Serial.println("OPEN_DOOR");
#endif
            } else {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
              Serial.println("CLOSE_DOOR");
#endif
            }
            if(time_of_last_door_command + TIME_TO_OPEN < now()) { // Door is static.
              COMMAND_REPLY_LENGTH_V1;
              VERSION;
              data[ACTION_V1_NDX] = COMMANDREPLY;
              int doorState = (inMsec > 0.0 && inMsec < 50.0) ? DOOR_OPEN : DOOR_CLOSED ; // Any range means its open.
              #ifdef EMULATOR_MODE
              doorState = door_state;
              #endif         
              if(data[COMMAND_V1_NDX] == TOGGLE_DOOR) {
                if(doorState == DOOR_CLOSED) {
                  data[COMMAND_V1_NDX] = OPEN_DOOR;
                } else {
                  data[COMMAND_V1_NDX] = CLOSE_DOOR;
                }
              }
              if(data[COMMAND_V1_NDX] == OPEN_DOOR && doorState == DOOR_CLOSED) { // Open door
                time_of_last_door_command = now();
                last_door_command = data[COMMAND_V1_NDX];
                data[COMMAND_REPLY_V1_NDX] = DOOR_OPENING;
                sendData();
                ActivateGarageDoor();
                #ifdef EMULATOR_MODE
                door_state = DOOR_OPEN;
                light_state= LIGHT_ON;
                #endif         
              } else if(data[COMMAND_V1_NDX] == CLOSE_DOOR && doorState == DOOR_OPEN) { // Close door.
                time_of_last_door_command = now();
                last_door_command = data[COMMAND_V1_NDX];
                data[COMMAND_REPLY_V1_NDX] = DOOR_CLOSING;
                sendData();
                ActivateGarageDoor();
                #ifdef EMULATOR_MODE
                door_state = DOOR_CLOSED;
                light_state= LIGHT_OFF;
                #endif         
              } else { // Already there. Just give status.
                sendStatusReply();
              }
            } else {
              sendStatusReply();
            }
          } else if(data[ACTION_V1_NDX] == STATUSREQ) {    // Status Request
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
            Serial.println("STATUSREQ");
#endif
            sendStatusReply();
          } else if(data[ACTION_V1_NDX] == DATAREQ) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
            Serial.println("DATAREQ");
#endif

            sendData();
          }
        } else {
#if defined ENABLE_DEBUG_LEVEL
          Serial.print("Bad Read: ");
          Serial.print(size_read);
          Serial.print(" vs ");
          Serial.println(data[LENGTH_V1_NDX]);
#endif
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
          data[LENGTH_V1_NDX] = STR_START_V1_NDX+12;
          sendData();
        }
      } else {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
        Serial.println("There are 0 clients.");
#endif
      }
    } else {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
      Serial.println("Unable to acquire client.");
#endif
    }
  } else {
    range_read = ultrasonic.timing();
    inMsec = ultrasonic.convert(range_read, Ultrasonic::IN);
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
    Serial.print("MS: ");
    Serial.print(range_read);
    Serial.print(", IN: ");
    Serial.println(inMsec);
#endif
  }
  toggle++;
  if(toggle > 4) {
    toggle = 0;
  }
  delay(1000);
}

void sendStatusReply() {
  STATUS_REPLY_LENGTH_V1;
  data[ACTION_V1_NDX] = STATUSREPLY;

#ifdef EMULATOR_MODE
  if(time_of_last_door_command + TIME_TO_OPEN > now()) {
    data[STATUS_DOOR_V1_NDX] = DOOR_BUSY;
  } else {
    data[STATUS_DOOR_V1_NDX] = door_state;
  }
  data[STATUS_LIGHT_V1_NDX] = light_state;
  data[STATUS_RANGE_V1_NDX] = 42;
#else
  if(time_of_last_door_command + TIME_TO_OPEN > now()) {
    data[STATUS_DOOR_V1_NDX] = DOOR_BUSY;
  } else {
    data[STATUS_DOOR_V1_NDX] = (inMsec > 0.0 && inMsec < 50.0) ? DOOR_OPEN : DOOR_CLOSED ; // Any range means its opend.
  }
  data[STATUS_RANGE_V1_NDX] = int(inMsec);
  int led = analogRead(lightPin);
  data[STATUS_LIGHT_V1_NDX] = (led < 50) ? LIGHT_OFF : LIGHT_ON;  // Range 0 - ~500
#endif         

  sendData();
}

void sendData() {
  digitalWrite(LED2, HIGH);	
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
  Serial.println();
  Serial.println("Response:");
  Serial.print("Size: ");
  Serial.println(data[LENGTH_V1_NDX]);
  Serial.print("Version: ");
  Serial.println(data[VERSION_V1_NDX]);
  Serial.print("Action: ");
#endif
  if(data[ACTION_V1_NDX] == STRING) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
    Serial.println("STRING");
#endif
  } else if(data[ACTION_V1_NDX] == COMMAND) { // Door command.
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
    Serial.println("COMMAND");
    Serial.print("Order: ");
#endif
    if(data[COMMAND_V1_NDX] == OPEN_DOOR) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
      Serial.println("OPEN_DOOR");
#endif
    } else if(data[COMMAND_V1_NDX] == CLOSE_DOOR){
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
      Serial.println("CLOSE_DOOR");
#endif
    } else {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
      Serial.println("DOOR_ORDER_UNKNOWN");
#endif
   }
  } else if(data[ACTION_V1_NDX] == COMMANDREPLY) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
    Serial.println("COMMANDREPLY");
    Serial.print("Reply: ");
#endif
    if(data[COMMAND_REPLY_V1_NDX] == DOOR_OPENING) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
      Serial.println("DOOR_OPENING");
#endif
    } else if(data[COMMAND_REPLY_V1_NDX] == DOOR_CLOSING) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
      Serial.println("DOOR_CLOSING");
#endif
    } else {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
      Serial.println("DOOR_REPLY_UNKNOWN");
#endif
    }
  } else if(data[ACTION_V1_NDX] == STATUSREPLY) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
    Serial.println("STATUSREPLY");
#endif
    if(data[STATUS_DOOR_V1_NDX] == DOOR_OPEN) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
      Serial.println("DOOR_OPEN");
#endif
    } else if(data[STATUS_DOOR_V1_NDX] == DOOR_CLOSED){
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
      Serial.println("DOOR_CLOSED");
#endif
    } else if(data[STATUS_DOOR_V1_NDX] == DOOR_BUSY){
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
      Serial.println("DOOR_BUSY");
#endif
    } else {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
      Serial.println("DOOR_STATUS_UNKNOWN");
#endif

    }
    if(data[STATUS_LIGHT_V1_NDX] == LIGHT_ON) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
      Serial.println("LIGHT_ON");
#endif
    } else if(data[STATUS_LIGHT_V1_NDX] == LIGHT_OFF) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
      Serial.println("LIGHT_OFF");
#endif
    } else {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
      Serial.println("LIGHT_STATUS_UNKNOWN");
#endif
    }
  } else if(data[ACTION_V1_NDX] == DATAREQ) {
    data[LENGTH_V1_NDX] = DATA_SIZE;
    uint8_t pos = STR_START_V1_NDX;
    for(int x = next_ndx + 1; x < HISTORY_SIZE; x++) {
      for(int y = 0; y < MAX_SIZE; y++) {
        data[pos] = saved_commands[x][y];
        pos++;
      }
    }
    for(int x = 0; x < next_ndx; x++) {
      for(int y = 0; y < MAX_SIZE; y++) {
        data[pos] = saved_commands[x][y];
        pos++;
      }
    }
  }
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_2
  Serial.println("===================================");
#endif

  data[data[LENGTH_V1_NDX]-4] = 1;           // Bogus CRC
  data[data[LENGTH_V1_NDX]-3] = 2;
  data[data[LENGTH_V1_NDX]-2] = 3;
  data[data[LENGTH_V1_NDX]-1] = 4;
  server.write(data,data[LENGTH_V1_NDX]);   // Just echo string.
  digitalWrite(LED2, LOW);
}

//Function to print time with time zone
void printTime(time_t t, char *tz)
{
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_1
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
#endif
}

//Print an integer in "00" format (with leading zero).
//Input value assumed to be between 0 and 99.
void sPrintI00(int val)
{
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_1
  if (val < 10) Serial.print('0');
  Serial.print(val, DEC);
#endif
  return;
}

//Print an integer in ":00" format (with leading zero).
//Input value assumed to be between 0 and 99.
void sPrintDigits(int val)
{
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_1
  Serial.print(':');
  if(val < 10) Serial.print('0');
  Serial.print(val, DEC);
#endif
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
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
    Serial.println(F("Unable to retrieve MAC Address!\r\n"));
#endif
  }
  else
  {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
    Serial.print(F("MAC Address : "));
    cc3000.printHex((byte*)&macAddress, 6);
#endif
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
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
    Serial.println(F("Unable to retrieve the IP Address!\r\n"));
#endif

    return false;
  }
  else
  {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
    Serial.print(F("\nIP Addr: ")); cc3000.printIPdotsRev(ipAddress);
    Serial.println();
#endif

    return true;
  }
}

// Minimalist time server query; adapted from Adafruit Gutenbird sketch,
// which in turn has roots in Arduino UdpNTPClient tutorial.
unsigned long getServerTime(void) {

  uint8_t       buf[48];
  unsigned long ip, startTime, t = 0L;

#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
  Serial.print(F("Locating time server..."));
#endif

  // Hostname to IP lookup; use NTP pool (rotates through servers)
  if(cc3000.getHostByName("0.pool.ntp.org", &ip)) {
    static const char PROGMEM
    timeReqA[] = { 227,  0,  6, 236 },
    timeReqB[] = {  49, 78, 49,  52 };

#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
    Serial.println(F("\r\nAttempting connection..."));
#endif

    startTime = millis();
    do {
      client = cc3000.connectUDP(ip, 123);
    } while((!client.connected()) &&
    ((millis() - startTime) < connectTimeout));

    if(client.connected()) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
      Serial.print(F("connected!\r\nIssuing request..."));
#endif

      // Assemble and issue request packet
      memset(buf, 0, sizeof(buf));
      memcpy_P( buf    , timeReqA, sizeof(timeReqA));
      memcpy_P(&buf[12], timeReqB, sizeof(timeReqB));
      client.write(buf, sizeof(buf));

#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
      Serial.print(F("\r\nAwaiting response..."));
#endif
      memset(buf, 0, sizeof(buf));
      startTime = millis();
      int count = 1;
      while((!client.available()) &&
      ((millis() - startTime) < responseTimeout)) {
        count++;
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
        Serial.print("Client availibility is ");
        Serial.print(client.available()); 
        Serial.println();
        Serial.print("Elapsed time is: ");
        Serial.print((millis()-startTime)); 
        Serial.println();
        Serial.print("Timeout is     : ");
        Serial.print(responseTimeout); 
        Serial.println();
#endif
        delay(100);
      }
      if(client.available()) {
        client.read(buf, sizeof(buf));
        t = (((unsigned long)buf[40] << 24) |
        ((unsigned long)buf[41] << 16) |
        ((unsigned long)buf[42] <<  8) |
        (unsigned long)buf[43]) - 2208988800UL;
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
        Serial.print(F("OK\r\n"));
#endif
      }
      client.close();
    }
  }
  if(!t) {
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
  Serial.println(F("error"));
#endif
  }
  return t;
}

/*
Configures pin mode for the digital output that controls the garage door
opener & sets the to a default (deactivated state). 
*/
void SetupDoorControl()
{
  pinMode(LED1, OUTPUT);     
  pinMode(LED2, OUTPUT);     
  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(LED1, LOW);
  digitalWrite(LED2, LOW);
  digitalWrite(RELAY_PIN, LOW);
  BlinkLED(true,true,1000,1000,3);
}

/*
Briefly triggers the garage door opener & flashes the indicator. 
*/
void ActivateGarageDoor()
{
#if defined ENABLE_DEBUG_LEVEL && defined ENABLE_DEBUG_LEVEL_4
  Serial.println(F("Door activated."));
#endif


#ifndef EMULATOR_MODE
  digitalWrite(LED1, HIGH);   // set the LED on
  digitalWrite(RELAY_PIN, HIGH);  // Open door.
  delay(DOOR_ACTIVATION_PERIOD);              
  digitalWrite(LED1, LOW);    // set the LED off
  digitalWrite(RELAY_PIN, LOW);  // Door will continue to open by itself.
#endif
}

void BlinkLED(bool blink1, bool blink2, int timeon, int timeoff, int repeatcount) {
  for(int x=0; x<repeatcount; x++) {
    if(blink1) {
      digitalWrite(LED1,HIGH);
    } else {
      digitalWrite(LED1,LOW);
    }
    if(blink2) {
      digitalWrite(LED2,HIGH);
    } else {
      digitalWrite(LED2,LOW);
    }
    delay(timeon);
    digitalWrite(LED1,LOW);
    digitalWrite(LED2,LOW);
    delay(timeoff);
  }
}
