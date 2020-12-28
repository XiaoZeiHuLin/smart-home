package com.example.a54961.lvxinjingnang;

import android.content.Intent;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;


public class Main2Activity extends AppCompatActivity implements LocationSource,AMapLocationListener,RadioGroup.OnCheckedChangeListener,GeocodeSearch.OnGeocodeSearchListener{

    private MapView mapView;
    private AMap aMap;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    private TextView mLocationErrText;
    private Marker geoMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        try {
            mapView = (MapView)findViewById(R.id.map);
            mapView.onCreate(savedInstanceState);

            init();

            aMap = mapView.getMap();
            geoMarker = aMap.addMarker(new MarkerOptions().anchor(0.5f,0.5f).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            aMap.setLocationSource(this);
            aMap.getUiSettings().setMyLocationButtonEnabled(true);
            aMap.setMyLocationEnabled(true);

            MyLocationStyle myLocationStyle = new MyLocationStyle();
            aMap.setMyLocationStyle(myLocationStyle);

            mLocationClient = new AMapLocationClient(getApplicationContext());
            mLocationClient.setLocationListener(this);
            mLocationOption = new AMapLocationClientOption();
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy); // 设置高精度模式
            mLocationClient.setLocationOption(mLocationOption); // 载入定位操作
            mLocationClient.startLocation();  // 开始定位

            ToggleButton tb = (ToggleButton)findViewById(R.id.tb);
            tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // 检测切换按钮
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        aMap.setMapType(AMap.MAP_TYPE_SATELLITE); // 卫星图
                    }else{
                        aMap.setMapType(AMap.MAP_TYPE_NORMAL); // 普通图
                    }
                }
            });
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    private void init(){
        try {
            RadioGroup mGPSModeGroup = (RadioGroup) findViewById(R.id.gps_radio_group);
            mGPSModeGroup.setOnCheckedChangeListener(this);
            mLocationErrText = (TextView)findViewById(R.id.location_errInfo_text);
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) { // 模式切换
        try {
            switch (checkedId){
                case R.id.gps_locate_button:
                    aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE); // 定位模式
                    break;
                case R.id.gps_follow_button:
                    aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW); // 跟随模式
                    break;
                case R.id.gps_rotate_button:
                    aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_ROTATE); // 3D模式
                    break;
            }
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            mapView.onResume();
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mapView.onPause();
            deactivate();
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }

    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        try {
            mapView.onDestroy();
            if(null != mLocationClient){
                mLocationClient.onDestroy();
            }
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }

    }
    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        try {
            mapView.onSaveInstanceState(outState);
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) { // 当位置改变时
        try {
            if(mListener != null){
                if(aMapLocation.getErrorCode() == 0){
                    aMapLocation.getLocationType();
                    aMapLocation.getLatitude();
                    aMapLocation.getLongitude();
                    aMapLocation.getAccuracy();
                    String latitude = aMapLocation.toString().split("#")[0].split("=")[1]; // 获取经度
                    String longitude = aMapLocation.toString().split("#")[1].split("=")[1]; // 获取纬度
                    String city = aMapLocation.toString().split("#")[3].split("=")[1]; // 获取城市
                    Intent intent = new Intent("com.example.a54961.lvxinjingnang.MYRECEIVER");
                    intent.putExtra("la",latitude);
                    intent.putExtra("lo",longitude);
                    intent.putExtra("city",city);
                    sendBroadcast(intent); // 发送位置信息广播

                    aMap.moveCamera(CameraUpdateFactory.zoomTo(17)); // 移动镜头
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(aMapLocation.getLatitude(),aMapLocation.getLongitude()))); // 移动镜头
                    mListener.onLocationChanged(aMapLocation); // 再次检测
                }else{
                    String errText = "定位失败，" + aMapLocation.getErrorCode() + "：" + aMapLocation.getErrorInfo();
                    Log.e("AmapErr",errText);

                    Toast.makeText(this, errText,Toast.LENGTH_LONG).show();
                    mLocationErrText.setText(errText);
                    mLocationErrText.setVisibility(View.VISIBLE);
                }
            }else {
                mLocationErrText.setText("123");
            }
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void activate(OnLocationChangedListener listener) { // 活动状态
        try {
            mListener = listener;
            //if(mLocationClient == null){
            mLocationClient = new AMapLocationClient(getApplicationContext());
            mLocationClient.setLocationListener(this);
            mLocationOption = new AMapLocationClientOption();
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationOption.setNeedAddress(true);
            mLocationClient.setLocationOption(mLocationOption);
            mLocationClient.startLocation();

            MyLocationStyle myLocationStyle = new MyLocationStyle();
            aMap.setMyLocationStyle(myLocationStyle);
            //}
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void deactivate() { // 不活动状态
        try {
            mListener = null;
            if(mLocationClient != null){
                mLocationClient.stopLocation();
                mLocationClient.onDestroy();
            }
            mLocationClient = null;
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) { // 获取地理码
        try {
            GeocodeAddress address = geocodeResult.getGeocodeAddressList().get(0);
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(address.getLatLonPoint().getLatitude(),address.getLatLonPoint().getLongitude()),15));
            geoMarker.setPosition(new LatLng(address.getLatLonPoint().getLatitude(),address.getLatLonPoint().getLongitude()));
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }
}