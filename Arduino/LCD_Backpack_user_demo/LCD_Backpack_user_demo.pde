void setup()
{
Serial.begin(115200);
//Change backlight value
Serial.print(0x7C,BYTE); // cmd
Serial.print(0x02,BYTE); // brightness
Serial.print(0x40,BYTE); // value between 0-100
//Run the built in demo
Serial.print(0x7C,BYTE); // cmd
Serial.print(0x04,BYTE); // demo
}
void loop() {}
