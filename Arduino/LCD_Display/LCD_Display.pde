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

int pause = 800;

unsigned char input[128];
unsigned char left_pins[8] = {12,13,18,16,15,11,17,10};
unsigned char right_pins[8] = {8,9,4,3,2,6,7,5};
unsigned char letters[26] = {126,230,240,206,242,
                             114,244,102,96,204,
                             110,224,1,124,252,
                             122,62,126,182,112,
                             236,1,1,1,174,
                             218};

unsigned char data[128];
void setup()                    // run once, when the sketch starts
{
  for(int x = 2; x < 20; x++) {
    pinMode(x,OUTPUT);
  }
  Serial.begin(9600);
  Serial.println("We're ready to go!");
}

bool first_time = 1;
void loop()                     // run over and over again
{
/*
  if(first_time) {
    first_time = 0;
    draw_left('B');
    draw_right('B');
  }
*/
  if(get_string_input(data)) {
    int length = 0;
    for(int x=0; x<128; x++) {
      if(data[x] != 0) length++;
      else break;
    }
    if(length) {
      char message[length+5];
      zero_string((unsigned char *)message,length+5);
      message[0] = message[1] = ' ';
      for(int x = 0; x<length; x++) {
        message[2+x] = data[x];
      }
      message[2+length] = message[3+length] = ' ';
      message[4+length] = 0;
      for(int x = 0; x < length+5; x++) {
        draw_left(message[x]);
        draw_right(message[x+1]);
        delay(pause);
      }
    }
  }

}

bool draw_left(unsigned char letter) {
  if((letter < 'A' || letter > 'Z') && letter != ' ')
    return false;
  char code;
  if(letter == ' ') code = 0;
  else code = letters[letter - 'A'];
//  Serial.println(code,DEC);
  for(int x=0; x<8; x++) {
//    Serial.print("Pin: ");
//    Serial.print(left_pins[x],DEC);
//    Serial.print("   State: ");
//    Serial.println(bitRead(code,x),DEC);
    digitalWrite(left_pins[x],bitRead(code,7-x));
  }
}

bool draw_right(unsigned char letter) {
  if((letter < 'A' || letter > 'Z') && letter != ' ')
    return false;
  char code;
  if(letter == ' ') code = 0;
  else code = letters[letter - 'A'];
  for(int x=0; x<8; x++) {
    digitalWrite(right_pins[x],bitRead(code,7-x));
  }
}

bool zero_string(unsigned char *str, int size) {
  for(int x=0;x<size;x++) {
    str[x] = 0;
  }
}

bool get_string_input(unsigned char *value) {
  int reading = 0;
  int ndx = 0;
  zero_string(input,128);
  while(Serial.available() > 0) {
    if(reading == 0) {
      reading = 1;
    }
    input[ndx++] = Serial.read();
    if(Serial.available() == 0)
      delay(500);
  }
  if(reading == 1) {
    for(int x=0; x<ndx;x++) {
      if(input[x] >=97) {
        input[x] -= 32;
      }
      value[x] = input[x];
    }
    value[ndx] = 0;
    print_string ("String is: ", (char *)value);
    return true;
  }
  return false;
}

bool get_integer_input(int *value) {
  int reading = 0;
  int ndx = 0;
  zero_string(input,128);
  while(Serial.available() > 0) {
    if(reading == 0) {
      reading = 1;
    }
    input[ndx++] = Serial.read();
    delay(10);
  }
  if(reading == 1) {
    int retval = 0;
    int length = ndx;
    int sign = 1;
    for(ndx = 0; ndx < length; ndx++) {
      if(ndx == 0 && (input[ndx] == '-' || input[ndx] == '+')) {
        if(input[ndx] == '-') {
          sign = -1;
        }
        length = -1;
        continue;
      }
      if(input[ndx] >= 48 && input[ndx]<= 57) {
        input[ndx] -= 48;
        retval += input[ndx] * pow(10,(length - (ndx+1)));
      }
    }
    *value = retval * sign;
    print_int ("Value is: ", *value, DEC);
    return true;
  }
  return false;
}

void print_int(char *str, int data, int type) {
    Serial.print(str);
    Serial.println(data,type);
}

void print_string(char *str, char *data) {
    Serial.print(str);
    Serial.println(data);
}

