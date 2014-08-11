int latchPin = 8;
int clockPin = 12;
int dataPin = 11;

union dataU {
  unsigned char byte[2];
  unsigned short data;
};

class pauseC {
  public:
    enum TYPE {ACCELERATED, LINEAR, SLOW, CONSTANT};
    pauseC(enum TYPE _type = ACCELERATED) {
      init(_type);
    };
    void init(int _incr = 10, enum TYPE _type = ACCELERATED) {
      type = _type;
      pause = 130;
      increment = _incr;
    };
    unsigned char accel() {
      shift = pause / increment;
      if(shift == 0) shift = (increment > 0) ? 1 : -1;
      int new_value = pause + shift;
      if((new_value <= 0) || new_value >= 200) {
        increment = -increment;
        shift = -shift;
        new_value = pause + shift;
      }
      return new_value;
    };
    unsigned char linear() {
      shift = increment;
      int new_value = pause + shift;
      if((new_value <= 0) || new_value >= 200) {
        increment = -increment;
        shift = -shift;
        new_value = pause + shift;
      }
      return new_value;
    };
    unsigned char constant() {
      return increment;
    }
    unsigned char get_pause() {
      if(type == ACCELERATED) pause = accel();
      else if(type == LINEAR) pause = linear();
      else if(type == CONSTANT) pause = constant();
      Serial.print("Pause = "); Serial.print(pause,DEC);
      Serial.print("   Shift = "); Serial.println(shift,DEC);
      return pause;
    };
    enum TYPE type;
    unsigned char pause;
    int shift;
    char increment;
} Delay ;

void setup() {
  //set pins to output because they are addressed in the main loop
  pinMode(latchPin, OUTPUT);
  pinMode(clockPin, OUTPUT);
  pinMode(dataPin, OUTPUT);
  randomSeed(analogRead(0));
  Serial.begin(57600);
  Serial.println("Ready!");
}

void loop() {
  static bool once = false;
//  if(once) return;
  bounce(false);

  counter(false);
  counter(true);
  chevrons1(true);
  chevrons2(true);
  chevrons2(false);
  chevrons1(false);
  swingLeds();
   for(int x= 1; x<=16; x++) {
    swishLeds(3,x);
  }
   for(int x= 16; x>=0; x--) {
    swishLeds(3,x);
  }
  sparkleLeds();

  once = true;
}

void bounce(bool forward) {
  float y1 = 0;        // led high
  float y2;
  float v = 10.0;       // velocity led / s
  float gravity = -4.0; // led / s^2
  unsigned long time = millis();
  unsigned long now;
  unsigned long start = millis();
  float delta;
  int counter = 0;
  shiftOut(0);
  do {
    do {
      now = millis();
      delta =  (now - time) / 1000.0;
    } while (delta < 0.01);
/*
    if(counter++ % 20 == 0)
      Serial.print("now\tdelta\ty1\ty2\tv\n");
    Serial.print(now);Serial.print("\t");
    Serial.print(delta);Serial.print("\t");
    Serial.print(y1);Serial.print("\t");
    Serial.print(y2);Serial.print("\t");
    Serial.print(v);Serial.print("\n");
    */
    y2 = y1 + v * delta + gravity * delta * delta ;
    v = (y2 - y1)/delta;
    if(y2 < 0) {
      y2 = -y2;
      v = -v * 0.8;
    }
    y1 = y2;
    time = now;
/*
    Serial.print("\t\t");
    Serial.print(y1);Serial.print("\t");
    Serial.print(y2);Serial.print("\t");
    Serial.print(v);Serial.print("\n");
    */
    unsigned short out = 1 << int(y2);
    if(int(y2) != int(y2+0.2))
      out |= 1 << int(y2+0.2);
    if((y2 > 0.2) && (int(y2) != int(y2-0.2)))
      out |= 1 << int(y2-0.2);
    shiftOut(out);
  } while (abs(y2) > 0.5 || abs(v) > 0.5);
}

void counter(bool forward) {
  long start = (forward) ? 0 : 0xffff;
  long end =   (forward) ? 0xffff : 0;
  int incr =  (forward) ? 1 : -1;
  for(long x = start; x != end + incr; x = x+incr) {
    unsigned long val = x;
    shiftOut(val);
  }
}

void chevrons2(bool forward) {
  unsigned long time = millis();
  unsigned long now;
  union dataU data;
  int start = (forward) ? 0 : 7;
  int end =   (forward) ? 7 : 0;
  int incr =  (forward) ? 1 : -1;
  do {
    now = millis();
    for(int x = start; x != end + incr; x = x + incr) {
      int y = x - 4;
      if(y < 0) y = y + 8;
      data.byte[0] = 0x80 >> x;
      data.byte[1] = 1 << x;
      data.byte[0] |= 0x80 >> y;
      data.byte[1] |= 1 << y;
      shiftOut(data.data);
      delay(50);
    }
  } while(now - time < 15000);
}

void chevrons1(bool forward) {
  unsigned long time = millis();
  unsigned long now;
  int start = (forward) ? 0 : 7;
  int end =   (forward) ? 7 : 0;
  int incr =  (forward) ? 1 : -1;
  union dataU data;
  do {
    now = millis();
    for(int x = start; x != end + incr; x = x + incr) {
      data.byte[0] = 0x80 >> x;
      data.byte[1] = 1 << x;
      shiftOut(data.data);
      delay(50);
    }
  } while(now - time < 15000);
}

void sparkleLeds() {
  unsigned long time = millis();
  unsigned long now;
  unsigned short randNum;
  do {
    now = millis();
    randNum = random(0,0xffff);
    shiftOut(randNum);
    delay(50);
  } while(now - time < 15000);
}

void swishLeds(int loopCount, int ledCount) {
  unsigned long time = millis();
  unsigned long now;
//  Serial.println("Entering swishLeds");
  union dataU data;
  unsigned char maxShift = 16 - (ledCount-1);
  Delay.init();
  
  unsigned short value = 0;
  for (int j = 0; j < ledCount; j++) {
    value = value << 1;
    value++;
  }
  do {
    for (int j = 0; j < maxShift; j++) {
      unsigned short number = value << j;
//      Serial.print("Number = "); Serial.println(number);
      shiftOut(number);
      delay(Delay.get_pause());
    }
    for (int j = maxShift -1 ; j >= 0; j--) {
      unsigned short number = value << j;
//     Serial.print("Number = "); Serial.println(number);
      shiftOut(number);
      delay(Delay.get_pause());
    }
  } while(now - time < 15000);
}

void swingLeds() {
  union dataU data;
  static unsigned char pause = 100;
  static char increment = 10;
//  Serial.print("Pause = ");
//  Serial.println(pause,DEC);

  unsigned long time = millis();
  unsigned long now;
  do {
    for (int j = 0; j < 8; j++) {
      data.byte[0] = ledByte(7-j);
      data.byte[1] = ledByte(j);
      shiftOut(data.data);
      delay(pause);
    }

    for (int j = 0; j < 8; j++) {
      data.byte[0] = ledByte(j);
      data.byte[1] = ledByte(7-j);
      shiftOut(data.data);
      delay(pause);
    } 
    if((pause+increment <= 0) || pause+increment >= 130) increment = -increment;
    pause +=  increment;
  } while(now - time < 15000);
}

unsigned char ledByte(int p) {
  unsigned char byte;
  byte = 1<< p;
  return byte;
}

void shiftOut(unsigned short data) {
  int pinState;
  digitalWrite(latchPin, 0);
  for (int i=15; i>=0; i--)  {
    digitalWrite(clockPin, 0);
    if ( data & (1<<i) ) {
      pinState= 1;
    } else {	
      pinState= 0;
    }

    digitalWrite(dataPin, pinState);
    digitalWrite(clockPin, 1);
    digitalWrite(dataPin, 0);
  }

  //stop shifting
  digitalWrite(clockPin, 0);
  digitalWrite(latchPin, 1);
}


