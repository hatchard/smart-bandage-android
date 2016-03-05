package com.example.jared.smart_bandage_android;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private SmartBandageAdapter smartBandageAdapter;
    private ListView lv;
    private HashMap<String, SmartBandage> myBandages;
    private Handler scanHandler;
    private HashMap<String,SmartBandage> rememberedSmartBandages;
    FileIO fileIO;
    Button scanBtn;
    private static final int SCAN_PERIOD = 10000;
    private Activity myself = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lv = (ListView) findViewById(R.id.deviceListView);
        smartBandageAdapter = new SmartBandageAdapter(this);
        scanHandler = new Handler();
        lv.setAdapter(smartBandageAdapter);
        scanBtn = (Button) findViewById(R.id.scanBtn);
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan(true);
            }
        });
        Button connectBtn = (Button) findViewById(R.id.connectBtn);
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter.isDiscovering()) {
                    Log.d(TAG, "Device still scanning");
                } else {
                    Log.d(TAG, "Device No longer Scanning");
                    final Intent intent = new Intent(myself, ConnectedDevicesActivity.class);
                    intent.putExtra(ConnectedDevicesActivity.DEVICE_LIST, rememberedSmartBandages);
                    startActivity(intent);
                }
            }
        });
        Button stopBtn = (Button) findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this,SmartBandageConnService.class);
                i.setAction(CustomActions.STOP_FOREGROUND_SERVICE);
                startService(i);
            }
        });

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        myBandages = new HashMap<String,SmartBandage>();
        fileIO = new FileIO();
    }

    @Override
    protected void onResume(){
        super.onResume();
        //App compat code, API 23+ requires runtime permission check
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //prompt explanation
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);

            }
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        /* This section of code attempts to read from file, if the file does not exist
         * it is created, if the file has unreadable data, then an empty hashmap is returned
         * it then updates the MainActivity to include the devices that are already remembered
         *
         */
        Log.d(TAG, "Reading remembered Devices from File");
        Intent startIntent = new Intent(MainActivity.this,SmartBandageConnService.class);
        startIntent.setAction(CustomActions.APP_START);
        startService(startIntent);
        String json = fileIO.readFile(getFilesDir() +
                FileIO.SAVE);
        rememberedSmartBandages = new HashMap<String,SmartBandage>();
        rememberedSmartBandages = fileIO.gsonSmartBandageHashMapDeserializer(json);
        myBandages.putAll(rememberedSmartBandages);
        smartBandageAdapter.setNotifyOnChange(false);
        smartBandageAdapter.addRememberedBandages(false);
        smartBandageAdapter.clear();
        smartBandageAdapter.addAll(myBandages.values());
        smartBandageAdapter.notifyDataSetChanged();
    }
    @Override
     protected void onStop() {
        super.onStop();
        Log.d(TAG, "Writing File to Storage On Stop");
        fileIO.writeFile(getFilesDir() + FileIO.SAVE,
                false,
                fileIO.gsonSmartBandageHashMapSerializer(rememberedSmartBandages));

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Writing File to Storage On Pause");
        fileIO.writeFile(getFilesDir() + FileIO.SAVE,
                false,
                fileIO.gsonSmartBandageHashMapSerializer(rememberedSmartBandages));

    }
    protected void startScan(final boolean enable){
        if (enable) {
        scanBtn.setEnabled(false);
        ScanFilter smartBandageFilter = new ScanFilter.Builder()
                .setServiceUuid(SmartBandage.BANDAGE_SERVICE)
                .build();
        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(smartBandageFilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();


            bluetoothLeScanner.startScan(filters, settings, scanCallback);
            scanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothLeScanner.stopScan(scanCallback);
                    scanBtn.setEnabled(true);
                }
            }, SCAN_PERIOD);
        }

    }


    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //Log.d(TAG,"onScanResult" );
            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG,"onBatchScanResults: " + results.size()+ " results");
            for (ScanResult result : results){
                processResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG, "Scan Failed " + errorCode);
        }
    };

    private void processResult(ScanResult result){
        //Log.d(TAG,"New BLE Device:  " + result.getDevice().getName() + " @ " + result.getRssi());

        SmartBandage smartBandage = new SmartBandage(result.getScanRecord(),
                result.getDevice().getAddress());
        msgHandler.sendMessage(Message.obtain(null,0,smartBandage));
    }

    private Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg){
            SmartBandage smartBandage = (SmartBandage) msg.obj;
            myBandages.put(smartBandage.getBandageAddress(),smartBandage);

            smartBandageAdapter.setNotifyOnChange(false);
            smartBandageAdapter.addRememberedBandages(false);
            smartBandageAdapter.clear();
            smartBandageAdapter.addAll(myBandages.values());
            smartBandageAdapter.notifyDataSetChanged();
        }
    };

    private class SmartBandageAdapter extends ArrayAdapter<SmartBandage> {

        public SmartBandageAdapter(Context context) {
            super(context, 0);
        }
        private boolean remembered = false;
        public void addRememberedBandages(boolean remembered){
            this.remembered = remembered;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup){

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.device_layout, viewGroup, false);
            }
            final SmartBandage smartBandage = getItem(position);

            TextView deviceName = (TextView) convertView.findViewById(R.id.textView);
            deviceName.setText(smartBandage.getBandageName());

            TextView deviceAddress = (TextView) convertView.findViewById(R.id.textView2);
            deviceAddress.setText(smartBandage.getBandageAddress());

            CheckBox ch = (CheckBox) convertView.findViewById(R.id.checkBox);

            if (remembered){
                ch.setChecked(true);
            }

            ch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        Log.d(TAG, "Remembering " + smartBandage.getBandageAddress());
                        rememberedSmartBandages.put(smartBandage.getBandageAddress(), smartBandage);
                    }
                    else{
                        Log.d(TAG, "Forgetting " + smartBandage.getBandageAddress());
                        rememberedSmartBandages.remove(smartBandage.getBandageAddress());
                        remembered = false;
                        Log.d(TAG,"remembered Device Exists: " + Boolean.toString(rememberedSmartBandages.containsKey(smartBandage.getBandageAddress())));
                    }
                }
            });
            return convertView;
        }

    }
}
