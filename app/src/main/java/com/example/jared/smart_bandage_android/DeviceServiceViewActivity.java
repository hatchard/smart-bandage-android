package com.example.jared.smart_bandage_android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class DeviceServiceViewActivity extends AppCompatActivity {
    public final static String BANDAGE = "BANDAGE";

    private BluetoothAdapter bluetoothAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_service_view);
        Toast.makeText(DeviceServiceViewActivity.this, "Discovering Services", Toast.LENGTH_SHORT).show();
        SmartBandage sm = (SmartBandage)getIntent().getSerializableExtra(BANDAGE);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();



    }
}
