/*
 * Blink
 *
 * The basic Arduino example.  Turns on an LED on for one second,
 * then off for one second, and so on...  We use pin 13 because,
 * depending on your Arduino board, it has either a built-in LED
 * or a built-in resistor so that you need only an LED.
 *
 * http://www.arduino.cc/en/Tutorial/Blink
 */

void setup()                    // run once, when the sketch starts
{
  for(int i=2;i<20;i++) {
    pinMode(i, OUTPUT);      // sets the digital pin as output
  }
}

void loop()                     // run over and over again
{
  for(int i = 2; i<20; i=i+2) {
      digitalWrite(i, HIGH);   // sets the LED on
      delay(800);                  // waits for a second
      digitalWrite(i, LOW);    // sets the LED off
  }
  for(int i = 3; i<20; i=i+2) {
      digitalWrite(i, HIGH);   // sets the LED on
      delay(800);                  // waits for a second
      digitalWrite(i, LOW);    // sets the LED off
  }
  for(int i = 2; i<20; i=i+2) {
      digitalWrite(i, HIGH);   // sets the LED on
      digitalWrite(i+1, HIGH);   // sets the LED on
      delay(800);                  // waits for a second
      digitalWrite(i, LOW);    // sets the LED off
      digitalWrite(i+1, LOW);    // sets the LED off
  }
  
}
