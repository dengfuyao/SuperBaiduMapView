package com.superbaidumapview.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.superbaidumapview.R;
import com.superbaidumapview.SuperApplication;

import java.text.DecimalFormat;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class LocationActivity2 extends AppCompatActivity {

    @Bind(R.id.mapView)
    MapView mMapView;
    @Bind(R.id.tv_location)
    TextView mTvLocation;
    @Bind(R.id.iv_bigpin)
    ImageView mIvBigpin;
    @Bind(R.id.ll_location)
    LinearLayout mLlLocation;

    private BaiduMap mBaiduMap;
    private GeoCoder mGeoCoder;
    private LocationClient locationClient;
    private Boolean isFirstRequest = true;

    private double selectLat;
    private double selectLon;
    private String mAddress;
    private String mDescription;
    private AnimatorSet mAnimatorSet;
    private LatLng src_point;

    /**
     * 格式化数字，保留小数点后两位
     *
     * @param value
     * @return
     */
    public String formatValue(double value) {
        DecimalFormat formatter = new DecimalFormat("#.######");
        return formatter.format(value);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_location2);
        ButterKnife.bind(this);
        // 地理编码查询结果监听器
        mGeoCoder = GeoCoder.newInstance();

        // 初始化BaiduMap对象
        initMapView();
        // 声明LocationClient类
        initLocationOptions();

        Intent intent = getIntent();
        if (intent != null) {
            String src_latlon = intent.getStringExtra("src_latlon");
            String[] split = src_latlon.split(",");
            src_point = new LatLng(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(src_point);
            mBaiduMap.animateMapStatus(update);
            mLlLocation.postDelayed(srcLatLonRunnable, 500);
        }
    }

    private Runnable srcLatLonRunnable = new Runnable() {
        @Override
        public void run() {
            setPopupTipsInfo(src_point);
        }
    };

    private void initMapView() {
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMaxAndMinZoomLevel(20, 11);
        MapStatus mapStatus = new MapStatus.Builder().zoom(18).build();
        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatus);
        mBaiduMap.setMapStatus(mapStatusUpdate);
        mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapStatus mapStatus) {
                mLlLocation.setVisibility(View.GONE);
            }

            @Override
            public void onMapStatusChange(MapStatus mapStatus) {

            }

            @Override
            public void onMapStatusChangeFinish(MapStatus mapStatus) {
                LatLng ptCenter = mBaiduMap.getMapStatus().target;
                setPopupTipsInfo(ptCenter);
            }
        });

        mGeoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {

            @Override
            public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {

            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
                if (reverseGeoCodeResult == null || reverseGeoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
//                    Toast.makeText(LocationActivity2.this, "没找到该地址", Toast.LENGTH_SHORT).show();
                } else {
                    // 获取大头针的地理位置
                    mAddress = reverseGeoCodeResult.getAddress();
                    mDescription = reverseGeoCodeResult.getSematicDescription();
                    // 获取大头针的坐标
                    LatLng location = reverseGeoCodeResult.getLocation();
                    selectLat = location.latitude;
                    selectLon = location.longitude;
                    // 显示大头针所在的信息
                    if (mAnimatorSet == null) {
                        mAnimatorSet = new AnimatorSet();
                    }
                    mAnimatorSet.playTogether(ObjectAnimator.ofFloat(mIvBigpin, "translationY", 0.0f, -30.0f, 0.0f),
                            ObjectAnimator.ofFloat(mIvBigpin, "rotationY", 0.0f, 720.0f));
                    mAnimatorSet.setDuration(500);
                    mAnimatorSet.start();
                    mTvLocation.setText(mDescription);
                    mLlLocation.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void initLocationOptions() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setOpenGps(true);// 打开gps:默认不打开
        option.setCoorType("bd09ll");//返回的定位结果是百度经纬度,默认值gcj02
        option.setScanSpan(5000);//设置发起定位请求的间隔时间为5000ms
        option.setIsNeedAddress(true);//返回的定位结果包含地址信息
        locationClient = new LocationClient(getApplicationContext());
        locationClient.setLocOption(option);
        locationClient.registerLocationListener(new BDLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation bdLocation) {
                if (bdLocation == null) {
                    return;
                }
                // 第一次定位时，将地图位置移动到当前位置
                if (isFirstRequest) {
                    Log.e("NetUtil", "定位吧.......");
                    isFirstRequest = false;
//                    LatLng latLng = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
//                    MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(latLng);
//                    mBaiduMap.animateMapStatus(update);
                    MyLocationData locData = new MyLocationData.Builder()
                            .latitude(bdLocation.getLatitude())  //纬度
                            .longitude(bdLocation.getLongitude())//经度
                            .build();
                    mBaiduMap.setMyLocationData(locData);
//                    setPopupTipsInfo(latLng);
                }
            }
        });
    }

    private void setPopupTipsInfo(LatLng latLng) {
        //设置反地理编码位置坐标
        ReverseGeoCodeOption option = new ReverseGeoCodeOption();
        option.location(latLng);
        //发起反地理编码请求
        mGeoCoder.reverseGeoCode(option);
    }

    @OnClick({R.id.tv_return, R.id.iv_location})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_return:
                Intent data = new Intent();
                data.putExtra("address", mAddress + " " + mDescription);
                data.putExtra("latLon", formatValue(selectLat) + "," + formatValue(selectLon));
                setResult(RESULT_OK, data);
                LocationActivity2.this.finish();
                break;
            case R.id.iv_location:
                isFirstRequest = true;
                double lat = SuperApplication.lat;
                double lon = SuperApplication.lon;
                LatLng point = new LatLng(lat, lon);
                MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(point);
                mBaiduMap.animateMapStatus(update);
                break;
        }
    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        mBaiduMap.setMyLocationEnabled(true);
        if (!locationClient.isStarted()) {
            locationClient.start();
        }
        super.onStart();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
        mLlLocation.removeCallbacks(srcLatLonRunnable);
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        mMapView = null;
        mBaiduMap.setMyLocationEnabled(false);
        if (locationClient.isStarted()) {
            locationClient.stop();
        }
        mGeoCoder.destroy();
        super.onDestroy();
    }
}
