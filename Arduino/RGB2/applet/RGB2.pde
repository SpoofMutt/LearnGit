// Fading LED 
// by BARRAGAN <http://people.interaction-ivrea.it/h.barragan> 

int value = 0;                            // variable to keep the actual value 
int ledpin = 13;                           // light connected to digital pin 13
int leds[3] = {3,5,6};
unsigned long maximum = 0x00FFFFFF;
void setup() 
{ 
  for(int x=0;x<3;x++) {
    pinMode(leds[x],OUTPUT);
    analogWrite(leds[x],HIGH);
  }
  Serial.begin(57600);
  Serial.println("Communications Active"); 
  delay(100);
} 
 
void loop() 
{ 
  for(unsigned long x=maximum; x>=0; x--) {
    Serial.print("Color: ");
    Serial.println(x,HEX);
    set_pins(x);
    delay(100);
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

//  analogWrite(leds[0],red);
//  analogWrite(leds[1],green);
  analogWrite(leds[2],blue);
}
