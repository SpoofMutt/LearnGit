// Fading LED 
// by BARRAGAN <http://people.interaction-ivrea.it/h.barragan> 

int value = 0;                            // variable to keep the actual value 
int ledpin = 13;                           // light connected to digital pin 13
int led2pin =3;
int  sos_size = 12;
char sos[12] = {1,1,1,3,3,3,1,1,1,0,0,0};
int done = 0;
void setup() 
{ 
  pinMode(led2pin,OUTPUT);
  Serial.begin(9600);
  Serial.println("Communications Active"); 
  delay(100);
} 
 
void loop() 
{ 
  if(! done) {
    for(int ndx = 0; ndx < sos_size; ndx++) {
      int wait = 150 * sos[ndx];
      Serial.print("Wait = ");
      Serial.println(wait);
      Serial.print("Ndx = ");
      Serial.println(ndx);
      Serial.print("sos[ndx] = ");
      delay(100);
      int value = sos[ndx];
      Serial.println(value);
      if(wait) {
        analogWrite(ledpin,255);
        analogWrite(led2pin,255);
      }
      delay(wait);
      analogWrite(ledpin,0);
      analogWrite(led2pin,0);
      delay(250);
    }
  }
//  done = 1;
} 



/*
void setup() 
{ 
  Serial.begin(9600); 
  
  // prints title with ending line break 
  Serial.println("ASCII Table ~ Character Map"); 
 
  // wait for the long string to be sent 
  delay(100); 
} 
 
int number = 33; // first visible character '!' is #33 
 
void loop() 
{ 
  Serial.print(number, BYTE);    // prints value unaltered, first will be '!' 
  
  Serial.print(", dec: "); 
  Serial.print(number);          // prints value as string in decimal (base 10) 
  // Serial.print(number, DEC);  // this also works 
  
  Serial.print(", hex: "); 
  Serial.print(number, HEX);     // prints value as string in hexadecimal (base 16) 
  
  Serial.print(", oct: "); 
  Serial.print(number, OCT);     // prints value as string in octal (base 8) 
  
  Serial.print(", bin: "); 
  Serial.println(number, BIN);   // prints value as string in binary (base 2) 
                                 // also prints ending line break 
 
  // if printed last visible character '~' #126 ... 
  if(number == 126) { 
    // loop forever 
    while(true) { 
      continue; 
    } 
  } 
 
  number++; // to the next character 
  
  delay(100); // allow some time for the Serial data to be sent 
} 
*/
