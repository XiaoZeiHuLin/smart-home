#include<SoftwareSerial.h>
#include<Servo.h>
#include<dht11.h>

SoftwareSerial BT(53, 52);
Servo myservo;
dht11 DHT11;

#define fluidPin A15
#define pressPin A14
#define smokePin A13
#define waterPin A12
#define firePin A11
#define lightPin A10
#define magnetPin A9

const int servoPin = 2;
const int vibrationPin = 21;
const int TrigPin = 22; 
const int EchoPin = 23;
const int TrigPin2 = 24; 
const int EchoPin2 = 25;
const int DHT11Pin = 26;

const int LED_yellow_Pin = 28;
const int LED_green_Pin = 29;
const int LED_red_Pin = 30;


int fluid_current=0,fluid_last=0;//水流变化检测
int cm,cm2;//两个超声波传感器的测量数据
int Status=0;//实时状态的表示码
int a_current=0,a_last=0,b_current=0,b_last=0;//两个超声波传感器的四种先后状态
int vibration_state = 0;//振动传感器读取得数据
int chk = 0;//温湿度传感器读取得数据

int val_BT,val_Ser;
char c_BT,c_Ser;
String str_BT,str2_BT,str_Ser,str2_Ser;
bool automode = true;

void setup() 
{ 
  Serial.begin(9600); 
  BT.begin(9600);
  pinMode(TrigPin, OUTPUT);//超声波1写入
  pinMode(EchoPin, INPUT);//超声波1读取
  pinMode(TrigPin2, OUTPUT);//超声波2写入
  pinMode(EchoPin2, INPUT);//超声波2读取
  pinMode(vibrationPin,INPUT);//振动传感器读取
  //attachInterrupt(2,blink,CHANGE);//针脚21位置上的变化调用函数
  
  pinMode(LED_red_Pin,OUTPUT);
  pinMode(LED_green_Pin,OUTPUT);
  pinMode(LED_yellow_Pin,OUTPUT);
  myservo.attach(servoPin);//舵机写入
} 
void loop() 
{ 
  digitalWrite(TrigPin, LOW); //低高低电平发一个短时间脉冲去TrigPin 
  delayMicroseconds(2); 
  digitalWrite(TrigPin, HIGH); 
  delayMicroseconds(10); 
  digitalWrite(TrigPin, LOW); 
  cm = pulseIn(EchoPin, HIGH) / 58.0; //将回波时间换算成cm
  cm=cm/10;

  digitalWrite(TrigPin2, LOW); //低高低电平发一个短时间脉冲去TrigPin
  delayMicroseconds(2); 
  digitalWrite(TrigPin2, HIGH); 
  delayMicroseconds(10); 
  digitalWrite(TrigPin2, LOW);
  cm2 = pulseIn(EchoPin2, HIGH) / 58.0; //将回波时间换算成cm
  cm2=cm2/10;
  
  if(cm > 27){
    cm=27;
  }
  if(cm2 > 27){
    cm2=27;
  }
  
  ////////
  a_current = cm;
  b_current = cm2;
  if(a_current<a_last-10){
    Status=Status*10+1;
  }else if(b_current<b_last-10){
    Status=Status*10+2;
  }
  //Serial.println(Status);
  switch(Status){
    case 12:Serial.print("In ");break;
    case 21:Serial.print("Out ");break;
    case 11:Serial.print("In&Return ");break;
    case 22:Serial.print("Out&Return ");break;
    default:Serial.print("NoPerson ");break;
  }
  if(Status>10){
    Status=0;
  }
  a_last=a_current;
  b_last=b_current;
  ////////
  
  if(analogRead(pressPin)>990 && analogRead(pressPin)<1005){//读取压力传感器数据
    Serial.print("Press ");
  }else{
    Serial.print("noPress ");
  }

  if(analogRead(magnetPin) < 100){//读取门磁传感器
    Serial.print("close ");
  }else{
    Serial.print("open ");
  }

  fluid_current=analogRead(fluidPin);//水流传感器
  if(abs(fluid_current-fluid_last)>500){
    Serial.print("flow ");
  }else{
    Serial.print("static ");
  }
  fluid_last=fluid_current;

  if(analogRead(lightPin) > 950){//光敏传感器
    Serial.print("dark ");
  }else if(analogRead(lightPin) < 950 && analogRead(lightPin) > 800){
    Serial.print("normal ");
  }else if(analogRead(lightPin) < 800 && analogRead(lightPin) >500){
    Serial.print("bright ");
  }else{
    Serial.print("veryBright ");
  }

  chk = DHT11.read(DHT11Pin);//温湿度传感器
  if(DHT11.temperature > 30 || DHT11.temperature < 10){
    Serial.print(DHT11.temperature);
  }else{
    Serial.print("temperatureNormally");
  }
  Serial.print(" ");
  if(DHT11.humidity > 50 || DHT11.humidity < 5){
    Serial.print(DHT11.humidity);
  }else{
    Serial.print("humidityNormally");
  }
  Serial.print(" ");

  if(analogRead(smokePin)>100){//烟雾传感器
    Serial.print("leak ");
  }else{
    Serial.print("gasSafe ");
  }

  if(analogRead(waterPin)>200){//水深传感器
    Serial.print("overflow ");
  }else{
    Serial.print("waterSafe ");
  }

  if(analogRead(firePin)<100){//火焰传感器
    Serial.print("burn ");
  }else{
    Serial.print("fireSafe ");
  }

  if(vibration_state != 0 ){//振动传感器
    vibration_state = 0;
    Serial.print("vibrate;");
  }else{
    Serial.print("stable;");
  }

  
  if(automode == true){// 自动模式
    if((analogRead(smokePin) > 100 || analogRead(lightPin) > 950 || DHT11.humidity < 5 || DHT11.humidity > 120) && analogRead(waterPin) < 200){//在不下雨的情况下，室内有烟雾或光照不足或湿度太大时开窗
      myservo.write(90);
    }else if(analogRead(waterPin) > 200 && analogRead(smokePin) < 100){//只有在下雨且室内没有烟雾的时候关窗
      myservo.write(0);
    }
    if(analogRead(lightPin) > 950){//根据不同环境需要调整阈值
      digitalWrite(LED_yellow_Pin,HIGH);
      digitalWrite(LED_green_Pin,HIGH);
      digitalWrite(LED_red_Pin,HIGH);
    }else if(analogRead(lightPin) < 800){//根据不同环境需要调整阈值
      digitalWrite(LED_yellow_Pin,LOW);
      digitalWrite(LED_green_Pin,LOW);
      digitalWrite(LED_red_Pin,LOW);
    }
  }
  
  if(BT.available()){//蓝牙接收与控制
    while(BT.available()){
      val_BT = BT.read();
      c_BT = (char)val_BT;
      str_BT = (String)c_BT;
      str2_BT = str2_BT + str_BT;
    }

    if(str2_BT.startsWith("light")){
      String str3 = str2_BT.substring(6);
      //Serial.print(str3);
      if(str3.substring(0,1).toInt() == 1){
        automode = false;
        if(str3.substring(2,3).toInt() == 0){
          digitalWrite(LED_yellow_Pin,HIGH);
          digitalWrite(LED_green_Pin,HIGH);
          digitalWrite(LED_red_Pin,HIGH);
        }else{
          digitalWrite(str3.substring(2,3).toInt()+27,HIGH);
        }
      }else if(str3.substring(0,1).toInt() == 0){
        automode = false;
        if(str3.substring(2,3).toInt() == 0){
          digitalWrite(LED_yellow_Pin,LOW);
          digitalWrite(LED_green_Pin,LOW);
          digitalWrite(LED_red_Pin,LOW);
        }else{
          digitalWrite(str3.substring(2,3).toInt()+27,LOW);
        }
      }
    }else if(str2_BT.startsWith("windows")){
      automode = false;
      myservo.write(str2_BT.substring(8).toInt());
    }else if(str2_BT.startsWith("location")){
      Serial.print(str2_BT.substring(9));
    }else if(str2_BT.startsWith("on")){
      automode = true;
    }
    str2_BT = "";
  }
  
  if(Serial.available()){//串口接收与控制
    while(Serial.available()){
      val_Ser = Serial.read();
      c_Ser = (char)val_Ser;
      str_Ser = (String)c_Ser;
      str2_Ser = str2_Ser + str_Ser;
    }
        
    if(str2_Ser == "on"){
      automode = true;
    }
    
    if(str2_Ser.indexOf("aa") != -1){
      automode = false;
      digitalWrite(LED_yellow_Pin,HIGH);
    }else{
      automode = false;
      digitalWrite(LED_yellow_Pin,LOW);
    }
    if(str2_Ser.indexOf("bb") != -1){
      automode = false;
      digitalWrite(LED_green_Pin,HIGH);
    }else{
      automode = false;
      digitalWrite(LED_green_Pin,LOW);
    }
    if(str2_Ser.indexOf("cc") != -1){
      automode = false;
      digitalWrite(LED_red_Pin,HIGH);
    }else{
      automode = false;
      digitalWrite(LED_red_Pin,LOW);
    }
    if(str2_Ser.indexOf("dd") != -1){
      automode = false;
      myservo.write(0);
    }else{
      automode = false;
      myservo.write(90);
    }
  }
  
  Serial.println();
  delay(500); 
}
void blink()
{
  vibration_state++;
}