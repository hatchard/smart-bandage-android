package com.example.jared.smart_bandage_android;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;


public class ConnectedDevicesActivity extends AppCompatActivity {
    private final static String TAG = ConnectedDevicesActivity.class.getSimpleName();

    private ConnectionListAdapter connectionListAdapter;
    public static Map<String, SmartBandage> deviceList;
    public static String DEVICE_LIST ="deviceList";
    private ListView deviceListview;
    private BluetoothAdapter bluetoothAdapter;
    private HashMap<String,SmartBandage> deviceConnectionStatus;
    private Activity myself = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_devices);

        HashMap<String, SmartBandage> importList = (HashMap<String,SmartBandage>) getIntent().getSerializableExtra(DEVICE_LIST);
        deviceList = SmartBandageConnService.getBandages();
        for (String key: importList.keySet()) {
            if (!deviceList.containsKey(key)) {
                SmartBandageConnService.addDevice(key);
            }
        }

        deviceListview = (ListView)findViewById(R.id.listView);
        deviceConnectionStatus = new HashMap<>();
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        connectionListAdapter = new ConnectionListAdapter(this);
        deviceListview.setAdapter(connectionListAdapter);
        deviceListview.setOnItemClickListener(listviewListener);

        connectionListAdapter.addAll(deviceList.values());
//        for(String key : deviceList.keySet()){
//            Log.d(TAG, "Attempting Connection to Device " + key);
//            if (null == deviceList.get(key)) {
//                deviceList.put(key, new SmartBandage(this, bluetoothAdapter.getRemoteDevice(key)));
//            }
//        }
    }
    private OnItemClickListener listviewListener =  new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            SmartBandage sm = (SmartBandage)parent.getItemAtPosition(position);
            Log.d(TAG, "Item Selected:" + sm.getBandageName() + " " + sm.getBandageAddress());
            Intent i = new Intent(myself,DisplayBandageReadingsActivity.class);
            i.putExtra(CustomActions.CURRENT_BANDAGE, sm.getBandageAddress());
            startActivity(i);
        }
    };

    private class ConnectionListAdapter extends ArrayAdapter<SmartBandage> {

        public ConnectionListAdapter(Context context) {
            super(context, 0);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup){

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.connection_status_layout, viewGroup, false);
            }
            final SmartBandage smartBandage = getItem(position);

            if (null == smartBandage) {
                return convertView;
            }

            TextView deviceName = (TextView) convertView.findViewById(R.id.connectionName);
            deviceName.setText(smartBandage.getBandageName());

            TextView deviceAddress = (TextView) convertView.findViewById(R.id.connectionAddress);
            deviceAddress.setText(smartBandage.getBandageAddress());

            TextView deviceConnectionStatus = (TextView) convertView.findViewById(R.id.connectionStatus);
            if (smartBandage.getBandageConnectionStatus()){
                deviceConnectionStatus.setText("Connected");
                //convertView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light,null));
            }
            else {
                deviceConnectionStatus.setText("Disconnected");
                //convertView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light, null));
            }

            return convertView;
        }

    }

    private void processResult(SmartBandage sm){
        //Log.d(TAG,"New BLE Device:  " + result.getDevice().getName() + " @ " + result.getRssi());

        msgHandler.sendMessage(Message.obtain(null,0,sm));
    }
    private Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg){
            SmartBandage smartBandage = (SmartBandage) msg.obj;
            deviceConnectionStatus.put(smartBandage.getBandageAddress(), smartBandage);

            connectionListAdapter.setNotifyOnChange(false);
            connectionListAdapter.clear();
            connectionListAdapter.addAll(deviceConnectionStatus.values());
            connectionListAdapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CustomActions.ACTION_GATT_CONNECTED);
        intentFilter.addAction(CustomActions.ACTION_GATT_DISCONNECTED);

        return intentFilter;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (CustomActions.ACTION_GATT_CONNECTED.equals(action) || CustomActions.ACTION_GATT_DISCONNECTED.equals(action)) {
                connectionListAdapter.notifyDataSetChanged();
            }
        }
    };
}
