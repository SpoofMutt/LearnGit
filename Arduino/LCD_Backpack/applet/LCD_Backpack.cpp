#include "WProgram.h"
void setup();
void loop();
char buffer[32];
uint8_t  cmd       = 0x7C;
uint8_t  chgbaud   = 0x07;
uint8_t  clr       = 0x00;
uint8_t  demo      = 0x04;
uint8_t  b_cnt     = 6;
uint16_t bauds[6]  = {4800,9600,19200,38400,57600,115200};
char    *cbauds[6] = {"1", "2", "3",  "4",  "5",  "6"};
uint8_t  default_baud = 6;

void setup()  
{
//  for(int i=b_cnt-1; i>0; i--) {
 //   Serial.begin(bauds[i]);
 //   Serial.write(cmd); Serial.write(chgbaud); Serial.write(cbauds[i]);
 //   Serial.write(cmd); Serial.write(clr);
 //   delay(200);
//  }
  Serial.begin(115200);
  Serial.write(cmd); Serial.write(chgbaud); Serial.write(cbauds[default_baud]);
//  delay(200);
//  Serial.begin(bauds[default_baud]);
//  Serial.write(cmd); Serial.write(clr);
//  delay(200);  
  
  Serial.print("Sending Demo Command.");
  delay(200);
  char buff[3] = {cmd, demo, 0};
  Serial.write(buff); 
}

void loop()                     // run over and over again
{
}

int main(void)
{
	init();

	setup();
    
	for (;;)
		loop();
        
	return 0;
}

