#include "WProgram.h"
void setup();
void loop();
void setup()
{
Serial.begin(115200);
//Change backlight value
//Serial.print(0x7C,BYTE); // cmd
//Serial.print(0x02,BYTE); // brightness
//Serial.print(0x40,BYTE); // value between 0-100
//Run the built in demo
}
int firsttime = 1;
void loop() {
  if(firsttime) {
    firsttime++;
    delay(200);
    if(firsttime > 1) {
      firsttime = 0;
      char var = 0x7c;
      Serial.print(var,BYTE); // cmd
      var = 0x13;
      Serial.print(var,BYTE); // demo
    }
  }
}

int main(void)
{
	init();

	setup();
    
	for (;;)
		loop();
        
	return 0;
}

