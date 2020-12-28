package com.example.a54961.lvxinjingnang;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import android.widget.Toast;
import android.telephony.SmsManager;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.google.gson.Gson;

public class MainActivity extends AppCompatActivity implements MyReceiver.Message { // 引用了包含广播接收器的抽象类的接口，因为一个子类只能继承一个父类

    MyReceiver myReceiver;
    String VoiceResult;
    String info;
    String CITY;
    String LONGITUDE;
    String LATITUDE;
    String item = "0";

    /*--- 数据库 ---*/
    DBHelper dbHelper;
    SQLiteDatabase sqLiteDatabase;

    /*--- 蓝牙 ---*/
    BluetoothAdapter bluetoothAdapter;
    private OutputStream outputStream;
    BluetoothSocket socket = null;
    Boolean BT_CONNECTED = false;
    String address = "98:D3:31:F6:0A:3A";

    /*--- 摔倒检测 ---*/
    Sensor sensor;
    SensorManager sensorManager;
    String info_current = "";
    String info_last = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*----------------- 设置线性加速度传感器用于跌落检测 -----------------*/
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float[] values = sensorEvent.values;
                int a = (int)Math.sqrt(Math.pow(values[0], 2) + Math.pow(values[1], 2) + Math.pow(values[2], 2));
                if (a > 50){
                    Emergency(getApplicationContext());
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) { }
        }, sensor, sensorManager.SENSOR_DELAY_GAME);

        /*-------------- 蓝牙：请求系统开启蓝牙 --------------*/
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enable_BT_Intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable_BT_Intent, 1);
        }

        /*------------------------- 初始化讯飞API对象并加入ID -------------------------*/
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=5b1f6f1c");

        /*------- 广播接收器 -------*/
        myReceiver = new MyReceiver(); // 初始化定义类
        IntentFilter intentFilter = new IntentFilter(); // 初始化内容过滤器
        intentFilter.addAction("com.example.a54961.lvxinjingnang.MYRECEIVER"); // 给内容过滤器加入
        registerReceiver(myReceiver, intentFilter); //注册广播接收器
        myReceiver.setMessage(this); // 绑定

        /*--------------------- 蓝牙状态广播接收器 ---------------------*/
        BroadcastReceiver stateChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED )){
                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_OFF){
                        Toast.makeText(getApplicationContext(),"广播:蓝牙已关闭",Toast.LENGTH_SHORT).show();
                        BT_CONNECTED = false;
                    }
                }
                if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
                    Toast.makeText(getApplicationContext(),"广播:蓝牙设备已断开",Toast.LENGTH_SHORT).show();
                    BT_CONNECTED = false;
                }
            }
        };
        IntentFilter stateChangeFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        IntentFilter disconnectedFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(stateChangeReceiver, stateChangeFilter);
        registerReceiver(stateChangeReceiver, disconnectedFilter);
    }

    /*------ 语音识别 -------*/
    public void open(View view) { // 主界面那个大 button 的 onClick
        initSpeech(this);
    }

    /*------------------ 语音识别：初始化设置 ------------------*/
    public void initSpeech(final Context context) { // 语音初始化
        try {
            // 创建RecognizerDialog对象
            RecognizerDialog mDialog = new RecognizerDialog(context, null);
            mDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn"); // 设置语音为中文
            mDialog.setParameter(SpeechConstant.ACCENT, "mandarin"); // 设置口音为普通话
            // 设置回调接口
            mDialog.setListener(new RecognizerDialogListener() {
                @Override
                public void onResult(RecognizerResult recognizerResult, boolean isLast) { // 定义接口，处理接收到的语音
                    if (!isLast) { // 是否说完
                        //解析语音
                        VoiceResult = parseVoice(recognizerResult.getResultString()); // 调用解析函数，解析json格式的语音转文本文件 recognizerResult.getResultString() 成字符串

                        VoiceInvoke(VoiceResult); // 将字符串交给页面跳转函数判断跳转
                    }
                }

                @Override
                public void onError(SpeechError speechError) {
                }
            });
            mDialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "3", Toast.LENGTH_SHORT).show(); // 简易的代码错误位置判断
        }
    }

    /*----------- 语音识别：通过 Gson 解析识别结果 -----------*/
    public String parseVoice(String resultString) { // 语音解析
        StringBuilder sb = new StringBuilder();
        Gson gson = new Gson();
        Voice voiceBean = gson.fromJson(resultString, Voice.class); // 通过解析类 Voice 使得 Gson 解析 json 格式的语音转文本数据到 Voice 对象 VoiceBean
        ArrayList<Voice.WSBean> ws = voiceBean.ws; // 获取语音句子
        for (Voice.WSBean wsBean : ws) { // 遍历句子中的每个词
            String word = wsBean.cw.get(0).w;
            sb.append(word);
        }
        return sb.toString();
    }

    /*--- 语音识别：定义 Gson 解析类，用于 Gson 解析识别结果时所需 ---*/
    public class Voice { // 是一个由字组成词、由词组成句的基本语音单元。
        ArrayList<WSBean> ws; // 句

        class WSBean {
            ArrayList<CWBean> cw; // 词
        }

        class CWBean {
            String w; // 字
        }
    }

    /*---------- 语音识别：功能设置 ----------*/
    public void VoiceInvoke(String VoiceResult) {
        if (VoiceResult.contains("位置")) {
            Intent intent = new Intent();
            intent.setClass(this, Main2Activity.class);
            startActivity(intent);
        } else if (VoiceResult.contains("天气")) {
            Intent intent = new Intent();
            intent.setClass(this, Main3Activity.class);
            intent.putExtra("CityName", CITY);
            startActivity(intent);
        } else if (VoiceResult.contains("报警")) {
            SMS();
        } else if (VoiceResult.contains("同步")) {
            if (!BT_CONNECTED){
                Connect();
            }
            if (BT_CONNECTED){
                Sync();
            }
            // 二者顺序不可反
        } else if (VoiceResult.contains("关窗")) {
            if (!BT_CONNECTED){
                Connect();
            }
            if (BT_CONNECTED){
                windows_control(0);
            }
            // 二者顺序不可反
        } else if (VoiceResult.contains("开窗")) {
            if (!BT_CONNECTED){
                Connect();
            }
            if (BT_CONNECTED){
                windows_control(90);
            }
            // 二者顺序不可反
        } else if (VoiceResult.contains("开一点")) {
            if (!BT_CONNECTED){
                Connect();
            }
            if (BT_CONNECTED){
                windows_control(45);
            }
            // 二者顺序不可反
        } else if (VoiceResult.contains("开灯")) {
            if (!BT_CONNECTED){
                Connect();
            }
            if (BT_CONNECTED){
                light_control(1,0);
            }
            // 二者顺序不可反
        } else if (VoiceResult.contains("打开客厅")) {
            if (!BT_CONNECTED){
                Connect();
            }
            if (BT_CONNECTED){
                light_control(1,1);
            }
            // 二者顺序不可反
        } else if (VoiceResult.contains("打开厨房")) {
            if (!BT_CONNECTED){
                Connect();
            }
            if (BT_CONNECTED){
                light_control(1,2);
            }
            // 二者顺序不可反
        } else if (VoiceResult.contains("打开卫生间")) {
            if (!BT_CONNECTED){
                Connect();
            }
            if (BT_CONNECTED){
                light_control(1,3);
            }
            // 二者顺序不可反
        } else if (VoiceResult.contains("关灯")) {
            if (!BT_CONNECTED){
                Connect();
            }
            if (BT_CONNECTED){
                light_control(0,0);
            }
            // 二者顺序不可反
        } else if (VoiceResult.contains("关闭客厅")) {
            if (!BT_CONNECTED){
                Connect();
            }
            if (BT_CONNECTED){
                light_control(0,1);
            }
            // 二者顺序不可反
        } else if (VoiceResult.contains("关闭厨房")) {
            if (!BT_CONNECTED){
                Connect();
            }
            if (BT_CONNECTED){
                light_control(0,2);
            }
            // 二者顺序不可反
        } else if (VoiceResult.contains("关闭卫生间")) {
            if (!BT_CONNECTED){
                Connect();
            }
            if (BT_CONNECTED){
                light_control(0,3);
            }
            // 二者顺序不可反
        }else if (VoiceResult.contains("自动模式")) {
            if (!BT_CONNECTED){
                Connect();
            }
            if (BT_CONNECTED){
                auto_mode();
            }
            // 二者顺序不可反
        }
    }

    /*----------- 蓝牙连接：若蓝牙未连接，则寻找指定蓝牙设备并自动连接 -----------*/
    public void Connect(){
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getAddress().equals(address)) {

                    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");                 // 通用 ID

                    try {
                        socket = device.createRfcommSocketToServiceRecord(uuid);                         // 通过 UUID 获取构建蓝牙连接的 Socket
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "获取 Socket 失败，请重试", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        socket.connect();                                                                // 连接该 Socket
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "连接 Socket 失败，请重试", Toast.LENGTH_SHORT).show();
                        try {
                            socket.close();
                        } catch (IOException e1) {
                            Toast.makeText(getApplicationContext(), "关闭 Socket 失败，请重试", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        return;
                    }

                    try {
                        BT_CONNECTED = true;
                        outputStream = socket.getOutputStream();// 从该 Socket 中获取数据输出流对象
                    } catch (Exception e) {
                        BT_CONNECTED = false;
                        Toast.makeText(getApplicationContext(), "获取 OutPutStream 失败，请重试", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    /*----- 蓝牙传输：获取数据库内的所有数据，并通过蓝牙一条一条的循环发送完毕，然后清空数据库 -----*/
    public void Sync() {
        try {
            dbHelper = new DBHelper(this);
            sqLiteDatabase = dbHelper.getWritableDatabase();
            Cursor cursor = sqLiteDatabase.query(
                    DBHelper.TABLE_NAME,
                    new String[]{DBHelper.TIME, DBHelper.CITY, DBHelper.LONGITUDE, DBHelper.LATITUDE},
                    null,
                    null,
                    null,
                    null,
                    DBHelper.TIME + " desc"
            );
            int TIME_index = cursor.getColumnIndex(DBHelper.TIME);
            int CITY_index = cursor.getColumnIndex(DBHelper.CITY);
            int LONGITUDE_index = cursor.getColumnIndex(DBHelper.LONGITUDE);
            int LATITUDE_index = cursor.getColumnIndex(DBHelper.LATITUDE);
            while (cursor.moveToNext()) {
                String time = cursor.getString(TIME_index);
                String city = cursor.getString(CITY_index);
                String longitude = cursor.getString(LONGITUDE_index);
                String latitude = cursor.getString(LATITUDE_index);
                String sensor = "location:" + time + " " + city + " " + longitude + " " + latitude;
                byte[] bytes = sensor.getBytes("UTF-8");
                outputStream.write(bytes);
                Thread.sleep(1000);  //  每次发送完毕后等待一会儿，给 Arduino 以反应的时间，防止粘包。若还发现粘包，则将数字不断调大即可
            }
            // ClearSQLite();
            cursor.close();
            sqLiteDatabase.close();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this,e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    /*-------------- 短信：编辑 --------------*/
    public void SMS() { // 编辑短信内容和发送号码
        try {
            info = "经度" + LONGITUDE + "纬度" + LATITUDE + "处，有一老人需要急救！";
            SendMsg(info, "15555059654");
            Toast.makeText(MainActivity.this, "发送成功！", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "未获取地理信息" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /*---------------------------------------------- 短信：发送 ----------------------------------------------*/
    public void SendMsg(String message, String phone) { // 通过系统权限将设定好的短信内容自动发送到设定好的号码上
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> list = smsManager.divideMessage(message); // 将短信内容分段，以防短信内容太大一次发送不完
        for (String text : list) { // 对于内容太大的短信进行多次发送
            smsManager.sendTextMessage(phone, null, text, null, null); // 发送短信
        }
    }

    /*------- 定义接口函数获取接收到的位置数据并存入数据库 -------*/
    @Override
    public void getMsg(String str) { // str 为包含了位置数据的字符串
        dbHelper = new DBHelper(this);
        sqLiteDatabase = dbHelper.getWritableDatabase();

        CITY = str.split("\n")[0]; // 获取城市信息
        LONGITUDE = str.split("\n")[1]; // 获取经度信息
        LATITUDE = str.split("\n")[2]; // 获取纬度信息

        // 取小数点后五位
        int dot_pos_lo = LONGITUDE.indexOf(".");
        LONGITUDE = LONGITUDE.substring(0,dot_pos_lo + 6);
        int dot_pos_la = LATITUDE.indexOf(".");
        LATITUDE = LATITUDE.substring(0,dot_pos_la + 6);

        /*- 存储进 SQLite 数据库 -*/
        if (!LONGITUDE.equals(item)) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:SS");
            Date date = new Date(System.currentTimeMillis());
            ContentValues values = new ContentValues();
            values.put(DBHelper.TIME, simpleDateFormat.format(date));
            values.put(DBHelper.CITY, CITY);
            values.put(DBHelper.LONGITUDE, LONGITUDE);
            values.put(DBHelper.LATITUDE, LATITUDE);
            sqLiteDatabase.insert(DBHelper.TABLE_NAME, null, values);
            Toast.makeText(this, "insert success", Toast.LENGTH_SHORT).show();
            item = LONGITUDE;
        }
    }

    /*------ 清空数据库 ------*/
    public void ClearSQLite() {
        dbHelper = new DBHelper(this);
        sqLiteDatabase = dbHelper.getWritableDatabase();
        int count = sqLiteDatabase.delete(DBHelper.TABLE_NAME, null, null);
        sqLiteDatabase.close();
        Toast.makeText(this, "delete " + count + " data(s)", Toast.LENGTH_SHORT).show();
    }

    /*------------------------- 紧急状态下，直接获取当前 GPS 定位数据并发送求救短信 -------------------------*/
    protected void Emergency(Context context) {
        double la = 0.0;
        double lo = 0.0;
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null){
                la = location.getLatitude();
                lo = location.getLongitude();
            } else {
                LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                    }
                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {
                    }
                    @Override
                    public void onProviderEnabled(String s) {
                    }
                    @Override
                    public void onProviderDisabled(String s) {
                    }
                };
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000,0,locationListener);

                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null){
                    la = location.getLatitude();
                    lo = location.getLongitude();
                }
            }

            /*-------------------- 编辑求救短信并发送 --------------------*/
            try {
                info_current = "经度" + lo + "纬度" + la + "处，有一老人需要急救！";
                if (!info_current.equals(info_last)){ // 滤重
                    SendMsg(info_current, "15555059654");
                    info_last = info_current;
                    Toast.makeText(MainActivity.this, "发送成功！", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "未获取地理信息" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void windows_control(int angle){
        String a = String.valueOf(angle);
        a = "windows:" + a;
        try {
            outputStream.write(a.getBytes());
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_SHORT).show();
        }
    }

    public void light_control(int state, int loc){
        String s = String.valueOf(state);
        String l = String.valueOf(loc);
        String light_control ="light:" + s + " " + l;
        try {
            outputStream.write(light_control.getBytes());
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_SHORT).show();
        }
    }

    public void auto_mode(){
        String auto_mode_code = "on";
        try {
            outputStream.write(auto_mode_code.getBytes());
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_SHORT).show();
        }
    }

    public void setQueryBTN(View view) {
        dbHelper = new DBHelper(this);
        sqLiteDatabase = dbHelper.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.query(DBHelper.TABLE_NAME, new String[]{DBHelper.TIME, DBHelper.CITY, DBHelper.LONGITUDE, DBHelper.LATITUDE}, null, null, null, null, DBHelper.TIME + " desc");
        int TIME_index = cursor.getColumnIndex(DBHelper.TIME);
        int CITY_index = cursor.getColumnIndex(DBHelper.CITY);
        int LONGITUDE_index = cursor.getColumnIndex(DBHelper.LONGITUDE);
        int LATITUDE_index = cursor.getColumnIndex(DBHelper.LATITUDE);
        while (cursor.moveToNext()) {
            String time = cursor.getString(TIME_index);
            String city = cursor.getString(CITY_index);
            String longitude = cursor.getString(LONGITUDE_index);
            String latitude = cursor.getString(LATITUDE_index);
            Toast.makeText(this, time + " " + city + " " + longitude + " " + latitude, Toast.LENGTH_SHORT).show();
        }
        cursor.close();
        sqLiteDatabase.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        try  {
            unregisterReceiver(myReceiver); // 注销广播接收器
            sensorManager.unregisterListener((SensorEventListener)this);
            socket.close();
            BT_CONNECTED = false;
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), e.toString() + "111", Toast.LENGTH_SHORT).show();
        }
        super.onDestroy();
    }
}