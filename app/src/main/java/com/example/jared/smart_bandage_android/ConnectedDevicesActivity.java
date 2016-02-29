package com.example.jared.smart_bandage_android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ConnectedDevicesActivity extends AppCompatActivity {
    private final static String TAG = ConnectedDevicesActivity.class.getSimpleName();

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    private ConnectionListAdapter connectionListAdapter;
    public static HashMap<String,SmartBandage> deviceList;
    public static String DEVICE_LIST ="deviceList";
    private ListView deviceListview;
    private BluetoothAdapter bluetoothAdapter;
    private HashMap<String,SmartBandage> deviceConnectionStatus;
    private Activity myself = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_devices);
        deviceList = (HashMap<String,SmartBandage>) getIntent().getSerializableExtra(DEVICE_LIST);
        deviceListview = (ListView)findViewById(R.id.listView);
        deviceConnectionStatus = new HashMap<String,SmartBandage>();
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        connectionListAdapter = new ConnectionListAdapter(this);
        deviceListview.setAdapter(connectionListAdapter);
        deviceListview.setOnItemClickListener(listviewListener);
        for(String key : deviceList.keySet()){
            Log.d(TAG, "Attempting Connection to Device " + key);
            BluetoothDevice device =  bluetoothAdapter.getRemoteDevice(key);
            device.connectGatt(this,true,mGattCallback);
        }
    }
    private OnItemClickListener listviewListener =  new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            SmartBandage sm = (SmartBandage)parent.getItemAtPosition(position);
            Log.d(TAG,"Item Selected:" + sm.getBandageName() + " " + sm.getBandageAddress());
            Intent i = new Intent(myself,DeviceServiceViewActivity.class);
            i.putExtra(DeviceServiceViewActivity.BANDAGE, sm);
            startActivity(i);
        }
    };

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                SmartBandage sm = new SmartBandage(gatt,SmartBandage.CONNECTED);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                processResult(sm);
                Log.i(TAG, "Attempting to start service discovery:" +
                        gatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                SmartBandage sm = new SmartBandage(gatt,SmartBandage.DISCONNECTED);
                processResult(sm);


            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

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
}
