#define sample 20
int x[sample], y[sample], z[sample]; 
int x_acc[sample], y_acc[sample];
int last_x, last_y, last_z;
int last_x_acc, last_y_acc;
int sc;
int incrX, incrY, incrZ;

void setup()
{
  incrX = incrY = incrZ = 1;
  for(int ndx=0; ndx < sample; ndx++) x[ndx] = y[ndx] = z[ndx] = x_acc[ndx] = y_acc[ndx] = 0;
  last_x = last_y = last_z = last_x_acc = last_y_acc = 0;
  sc = 0;
  Serial.begin(9600);           // sets the serial port to 9600
  analogReference(EXTERNAL);
}

void get_sample(int *array, int read_val) {
  for(int ndx=1; ndx<sample; ndx++) {
    array[ndx-1] = array[ndx];
  }
  array[sample-1] = read_val;
}

int avg_sample(int *array) {
  long sum = 0;
  for(int ndx=0; ndx<sample; ndx++) {
    sum += array[ndx];
  }
  sum = sum / sample;
  return sum;
}

void loop() 
{
  get_sample(x,analogRead(0));       // read analog input pin 0
  get_sample(y,analogRead(1));       // read analog input pin 1
  get_sample(z,analogRead(2));       // read analog input pin 1
  get_sample(x_acc,analogRead(3));
  get_sample(y_acc,analogRead(4));
  if((last_x < avg_sample(x) - incrX || last_x > avg_sample(x) + incrX) ||
     (last_y < avg_sample(y) - incrY || last_y > avg_sample(y) + incrY) ||
     (last_z < avg_sample(z) - incrZ || last_z > avg_sample(z) + incrZ)) {
    incrX = avg_sample(x) / 200.0;
    incrY = avg_sample(y) / 200.0;
    incrZ = avg_sample(z) / 100.0;
    Serial.print("x, y, z, xacc, yacc, sc: ");
    Serial.print(avg_sample(x), DEC);    // print the acceleration in the X axis
    Serial.print(" ");       // prints a space between the numbers
    Serial.print(avg_sample(y), DEC);    // print the acceleration in the Y axis
    Serial.print(" ");       // prints a space between the numbers
    Serial.print(avg_sample(z), DEC);  // print the acceleration in the Z axis
    Serial.print(" ");       // prints a space between the numbers
    Serial.print(avg_sample(x_acc), DEC);  // print the acceleration in the X_acc axis
    Serial.print(" ");       // prints a space between the numbers
    Serial.print(avg_sample(y_acc), DEC);  // print the acceleration in the Y_acc axis
    Serial.print(" (");       // prints a space between the numbers
    Serial.print(sc, DEC);
    Serial.println(")");
    
    last_x = avg_sample(x);
    last_y = avg_sample(y);
    last_z = avg_sample(z);
    last_x_acc = avg_sample(x_acc);
    last_y_acc = avg_sample(y_acc);
    sc = 0;
  } else {
    sc++;
  }
//  delay(100);              // wait 100ms for next reading
}


