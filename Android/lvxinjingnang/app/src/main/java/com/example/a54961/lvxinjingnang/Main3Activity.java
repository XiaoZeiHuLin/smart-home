package com.example.a54961.lvxinjingnang;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


public class Main3Activity extends AppCompatActivity {

    String cityName,cityname;
    URL url;
    HttpURLConnection uConnection;
    InputStream is;
    BufferedReader br;
    String result = "";
    String readLine = null;
    TextView tv,City;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().detectLeakedClosableObjects().penaltyLog().penaltyDeath().build());

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        cityname = bundle.getString("CityName"); // 获取所在城市数据

        City = (TextView)findViewById(R.id.City);
        City.setText(cityname);

        tv = (TextView)findViewById(R.id.tv);
        try {
            cityName = URLEncoder.encode(cityname,"UTF-8"); // 转码
            String it = "http://v.juhe.cn/weather/index?format=2&cityname=" + cityName + "&key=91a781a05b0884ed700be2569481e317"; // 调用聚合数据天气查询API
            url = new URL(it);
            uConnection = (HttpURLConnection)url.openConnection(); // 打开http连接
            uConnection.setDoOutput(true);
            is = uConnection.getInputStream(); // 获取输入流
            br = new BufferedReader(new InputStreamReader(is,"UTF-8"));
            while((readLine = br.readLine()) != null) {
                result += readLine;
            }
            is.close();
            br.close(); // 关闭
        } catch (Exception e) {
            e.printStackTrace();
        }
        uConnection.disconnect();

        // 解析json格式返回数据
        JsonParser parser = new JsonParser();
        JsonObject json=(JsonObject)parser.parse(result);
        JsonObject Result = json.get("result").getAsJsonObject();
        JsonObject Today = Result.get("today").getAsJsonObject();

        tv.setText(Today.get("temperature").getAsString() + "\n" + Today.get("weather").getAsString() + "\n" + Today.get("wind").getAsString() + "\n" + Today.get("dressing_index").getAsString() + "\n" + Today.get("dressing_advice"));
    }
}


