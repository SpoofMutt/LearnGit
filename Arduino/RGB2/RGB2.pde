// Fading LED 
// by BARRAGAN <http://people.interaction-ivrea.it/h.barragan> 

int value = 0;                            // variable to keep the actual value 
int ledpin = 13;                           // light connected to digital pin 13
int leds[3] = {
  3,5,6};
unsigned long maximum = 0x00FFFFFF;
void setup() 
{ 
  for(int x=0;x<3;x++) {
    pinMode(leds[x],OUTPUT);
    analogWrite(leds[x],255);
  }
  Serial.begin(57600);
  Serial.println("Communications Active"); 
  delay(100);
} 

void loop() 
{ 
  int value = 0;
  for(unsigned long x=1; x<=7; x++) {
    for(unsigned long intensity = 255; intensity >= 0; intensity -= 10) {
      unsigned long value = (x & 0x01) ? intensity : 0;
      value <<= 8;
      value |= (x & 0x02) ? intensity : 0;
      value <<= 8;
      value |= (x & 0x04) ? intensity : 0;
          Serial.print("Color: ");
          Serial.println(value,HEX);
      set_pins(value);
      delay(100);
    }
  }
}

void set_pins(unsigned long color) {
  unsigned long red = (color >> 16) & 0xFF;
  unsigned long green = (color >> 8) & 0xFF;
  unsigned long blue = color & 0xFF;

  //  Serial.print("RGB: ");
  //  Serial.print(red,HEX);
  //  Serial.print("\t");
  //  Serial.println(green,HEX);
  //  Serial.print("\t");
  //  Serial.println(blue,HEX);

  analogWrite(leds[0],red);
  analogWrite(leds[1],green);
  analogWrite(leds[2],blue);
}
