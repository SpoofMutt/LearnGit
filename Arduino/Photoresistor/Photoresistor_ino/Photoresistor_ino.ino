int lightPin = 0;  //define a pin for Photo resistor
int ledPin=13;     //define a pin for LED

void setup()
{
    Serial.begin(9600);  //Begin serial communcation
}

void loop()
{
    Serial.println(digitalRead(lightPin)); //Write the value of the photoresistor to the serial monitor.
   delay(10); //short delay for faster response to light.
}
