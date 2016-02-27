package com.example.jared.smart_bandage_android;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ExpandableListView;

import java.util.HashMap;
import java.util.Iterator;

public class ConnectedDevicesActivity extends AppCompatActivity {
    private final static String TAG = ConnectedDevicesActivity.class.getSimpleName();
    public static HashMap<String,SmartBandage> deviceList;
    public static String DEVICE_LIST ="deviceList";
    private ExpandableListView deviceListview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_devices);
        deviceList = (HashMap<String,SmartBandage>) getIntent().getSerializableExtra(DEVICE_LIST);
        deviceListview = (ExpandableListView)findViewById(R.id.listView);
        for (String key : deviceList.keySet()){
            Log.i(TAG + "found an address",key);
        }

    }
}
