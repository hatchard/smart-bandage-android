package com.example.jared.smart_bandage_android;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DeviceServiceViewActivity extends AppCompatActivity {
    private final static String TAG = DeviceServiceViewActivity.class.getSimpleName();

    public final static String BANDAGE = "BANDAGE";
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
    public final static String BANDAGE_TEMP_AVAILABLE =
            "com.example.bluetooth.le.BANDAGE_TEMP_AVAILABLE";
    public static final String BANDAGE_HUMIDITY_AVAILABLE =
            "com.example.smart_bandage_android.BANDAGE_HUMIDITY_AVAILABLE";

    private static final ParcelUuid UUID_HEART_RATE_MEASUREMENT = SmartBandage.BANDAGE_SERVICE;
    private BluetoothAdapter bluetoothAdapter;
    ExpandableListView serviceListView;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private boolean doSingleWrite = false;
    private SmartBandage smartBandage;
    BluetoothGatt mBluetoothGatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_service_view);
        Toast.makeText(DeviceServiceViewActivity.this, "Discovering Services", Toast.LENGTH_SHORT).show();
        smartBandage = (SmartBandage)getIntent().getSerializableExtra(BANDAGE);
        TextView tv = (TextView) findViewById(R.id.textView3);
        tv.setText(smartBandage.getBandageName());

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        serviceListView = (ExpandableListView)findViewById(R.id.expandableListView);
        serviceListView.setOnChildClickListener(servicesListClickListner);
        mBluetoothGatt = bluetoothAdapter.getRemoteDevice(smartBandage.getBandageAddress()).connectGatt(this, true, myCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver,makeGattUpdateIntentFilter());
    }

    private final BluetoothGattCallback myCallback = new BluetoothGattCallback() {
        int tries = 0;
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                tries = 0;
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.setCharacteristicNotification(gatt.getService(SmartBandageGatt.UUID_SMART_BANDAGE_SERVICE).getCharacteristic(SmartBandageGatt.UUID_READINGS), false);
                intentAction = ACTION_GATT_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBluetoothGatt.requestMtu(256);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("Application MTU size updated: " + Integer.toString(mtu));

                SetEnableCharacteristicNotifications(
                        gatt.getService(SmartBandageGatt.UUID_SMART_BANDAGE_SERVICE)
                                .getCharacteristic(SmartBandageGatt.UUID_READINGS), true);
            } else {
                System.err.println("Application MTU size update failed. Current MTU: " + Integer.toString(mtu));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("BLE read success " + characteristic.getUuid().toString());
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            } else {
                System.err.println("BLE read failed");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("BLE write succeeded");
            } else {
                System.err.println("BLE write failed");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            System.out.println("BLE data updated " + characteristic.getUuid().toString());
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("Bluetooth descriptor write success: " + descriptor.getUuid().toString());
            } else {
                System.err.println("Bluetooth descriptor write failed: " + descriptor.getUuid().toString());
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("Bluetooth descriptor read success: " + descriptor.getUuid().toString());
            } else {
                System.err.println("Bluetooth descriptor read failed: " + descriptor.getUuid().toString());
            }
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_GATT_CONNECTED.equals(action)) {

            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {

            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothGatt.getServices());

            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                Toast.makeText(DeviceServiceViewActivity.this,intent.getStringExtra(EXTRA_DATA),Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Temperature Value") {
            final byte[] data = characteristic.getValue();
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Temperatures: \n");
            for (int i = 0; i < data.length/2; ++i) {
                stringBuilder.append((((0x0FF & data[2*i+1]) << 8 | (0x0FF & data[2*i])))/16.);
                stringBuilder.append(" C\n");
            }
            intent.putExtra(BANDAGE_TEMP_AVAILABLE, stringBuilder.toString());
        } else if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Humidity Value") {
            final byte[] data = characteristic.getValue();
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Humidity: \n");
            for (int i = 0; i < data.length/2; ++i) {
                stringBuilder.append((((0x0FF & data[2*i+1]) << 8 | (0x0FF & data[2*i])))/16.);
                stringBuilder.append("%RH\n");
            }
            intent.putExtra(BANDAGE_HUMIDITY_AVAILABLE, stringBuilder.toString());
        } else if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Battery Charge") {
            final byte[] data = characteristic.getValue();
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Battery Voltage: \n");
            for (int i = 0; i < data.length/2; ++i) {
                stringBuilder.append((((0x0FF & data[2*i+1]) << 8 | (0x0FF & data[2*i])))/16.);
                stringBuilder.append("mv\n");
            }
            intent.putExtra(EXTRA_DATA, stringBuilder.toString());
        } else if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Reading Size") {
            final byte[] data = characteristic.getValue();
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Reading Size: \n");
            for (int i = 0; i < data.length/2; ++i) {
                stringBuilder.append((((0x0FF & data[2*i+1]) << 8 | (0x0FF & data[2*i]))));
                stringBuilder.append("bytes\n");
            }
            intent.putExtra(EXTRA_DATA, stringBuilder.toString());
        } else if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Reading Count") {
            final byte[] data = characteristic.getValue();
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Number of Available Readings: \n");
            int value = 0;
            for (int i = 0; i < data.length; ++i) {
                value |= (0x0FF & data[i]) << (8 * i);
//                stringBuilder.append((((0x0FF & data[2*i+1]) << 8 | (0x0FF & data[i]))));
            }
            stringBuilder.append(value);
            stringBuilder.append("\n");
            intent.putExtra(EXTRA_DATA, stringBuilder.toString());

        } else if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Readings") {
            final byte[] data = characteristic.getValue();
            if (doSingleWrite && data != null && data.length > 0) {
                doSingleWrite = false;
                final BluetoothGattCharacteristic writeChar =
                        mGattCharacteristics.get(2).get(10);
                byte[] v = new byte[2];
                v[0] = 22;
                v[1] = 0;
                writeChar.setValue(v);
                if ((writeChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    if (mBluetoothGatt.writeCharacteristic(writeChar)) {
                        System.out.println("Started BLE write OK");
                    } else {
                        System.out.println("BLE Write failed");
                    }
                } else if ((writeChar.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                    if (mBluetoothGatt.writeCharacteristic(writeChar)) {
                        System.out.println("Started BLE write_no_resp OK");
                    } else {
                        System.out.println("BLE write_no_resp failed");
                    }
                } else {
                    System.out.println("Characteristic not writeable");
                }
            }
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        serviceListView.setAdapter(gattServiceAdapter);
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();

                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            mBluetoothGatt.readCharacteristic(characteristic);
                        } else {
                            System.err.println("Read characteristic does not support notifications");
                        }
                        return true;
                    }
                    return false;
                }
            };

    private boolean SetEnableCharacteristicNotifications(BluetoothGattCharacteristic characteristic, boolean enable) {
        if (enable) {
            System.out.println("Enabling notifications for characteristic " + characteristic.getUuid().toString());
        } else {
            System.out.println("Disabling notifications for characteristic " + characteristic.getUuid().toString());
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, true);

        final BluetoothGattDescriptor desc = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
        if (null == desc) {
            System.err.println("Failed to get config descriptor");
            return false;
        }

        if (!desc.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
            System.err.println("Failed to set notification value");
            return false;
        }

        if (!mBluetoothGatt.writeDescriptor(desc)) {
            System.err.println("Failed to set descriptor");
            return false;
        }

        System.out.println("Characteristic descriptor written");

        return true;
    }
}