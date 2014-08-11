#include <NewSoftSerial.h>

#include "WProgram.h"
void setup();
void loop();
NewSoftSerial mySerial(11,12);

char buffer[32];
uint8_t  cmd       = 0x7C;
uint8_t  chgbaud   = 0x07;
uint8_t  clr       = 0x00;
uint8_t  demo      = 0x04;
uint8_t  b_cnt     = 6;
uint32_t bauds[6]  = {4800,9600,19200,38400,57600,115200};
char     cbauds[6] = {'1', '2', '3',  '4',  '5',  '6'};
uint8_t  default_baud = 5;

int reset_comm = 1;

void setup()  
{
  Serial.begin(57600);
  if(reset_comm) {
    mySerial.begin(115200);
    for(int i = 0; i < 10; i++) 
      mySerial.print("Hello! Changing baud rate now.");
    mySerial.print(cmd,BYTE); mySerial.print(chgbaud,BYTE); mySerial.print(cbauds[default_baud]);
    delay(300);
  }
  mySerial.begin(bauds[default_baud]);
  mySerial.println("Done!");
}

void loop()                     // run over and over again
{
  mySerial.print("Hello World!");
  delay(1000);
}


int main(void)
{
	init();

	setup();
    
	for (;;)
		loop();
        
	return 0;
}

