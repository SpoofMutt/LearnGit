#include <NewSoftSerial.h>

NewSoftSerial mySerial(3,2);

// 128 x 64
// Chars are 6 x 8 - Upper Left Corner - Up and to the right.

void setup() {
  mySerial.begin(115200);      // Open the serial port at 9600 bps:   
  mySerial.print(0x7c,BYTE);
  mySerial.print(0x00,BYTE);
  mySerial.flush();
  randomSeed(analogRead(0));
}

void loop() {
  long rX = random(128 -6)+ 6;
  long rY = random(64 - 8)+8;
  mySerial.print(byte(0x7c));
  mySerial.print(byte(0x18));
  mySerial.print(byte(char(rX)));
  mySerial.flush();
  delay(30);
  mySerial.print(byte(0x7c));
  mySerial.print(byte(0x19));
  mySerial.print(byte(char(rY)));
  mySerial.flush();
  delay(30);
  mySerial.print(" Ryan ");  // Write some text to the LCD
  mySerial.flush();
  delay(30);
}
