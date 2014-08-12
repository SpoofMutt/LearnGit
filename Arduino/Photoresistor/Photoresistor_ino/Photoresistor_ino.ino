int lightPin = 0;  //define a pin for Photo resistor
int ledPin=13;     //define a pin for LED

void setup()
{
    Serial.begin(9600);  //Begin serial communcation
    pinMode( ledPin, OUTPUT );
}

void loop()
{
    Serial.println(analogRead(lightPin)); //Write the value of the photoresistor to the serial monitor.
    int led = analogRead(lightPin);
    if(led < 250) {
      digitalWrite(ledPin, HIGH);  //send the value to the ledPin. Depending on value of resistor 
                                                //you have  to divide the value. for example, 
                                                //with a 10k resistor divide the value by 2, for 100k resistor divide by 4.
    } else {
      digitalWrite(ledPin, LOW);  //send the value to the ledPin. Depending on value of resistor 
    }
   delay(10); //short delay for faster response to light.
}
