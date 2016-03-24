package com.example.jared.smart_bandage_android;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class SmartBandageConnService extends Service {
    private static final String TAG = SmartBandageConnService.class.getSimpleName();
    HashMap<String,SmartBandage> rememberedBandages;
    public boolean readingsAvailable = false; //is set to true when there are historical readings available
    BluetoothAdapter bluetoothAdapter;
    Queue<Intent> broadcastQueue = new LinkedList<>();
    Queue<BluetoothGattDescriptor>  bleQueue = new LinkedList<>();
    public Queue<BluetoothGattCharacteristic> bleReadQueue = new LinkedList<>();
    BluetoothGatt mBluetoothGatt;
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";

    public SmartBandageConnService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FileIO f = new FileIO();
        String json = f.readFile(getFilesDir() +
                FileIO.SAVE);
        rememberedBandages = new HashMap<>();
        rememberedBandages = f.gsonSmartBandageHashMapDeserializer(json);
        Intent activityIntent = new Intent(this,MainActivity.class);
        activityIntent.setAction(CustomActions.MAIN_ACTION);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);
        Log.i(TAG, "Received Start Foreground Service Intent");
        Notification.Builder nBuilder = new Notification.Builder(this)
                .setContentTitle("Smart Bandage Service")
                .setContentText("Running..")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.black_circle)
                .setOngoing(true);
        Notification notification = nBuilder.build();
        startForeground(CustomActions.FOREGROUND_SERVICE_ID, notification);
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        for (String key : rememberedBandages.keySet()){
            Log.i(TAG,"Device " + key );

            mBluetoothGatt = bluetoothAdapter.getRemoteDevice(key).connectGatt(this, true, mGattCallback);
        }
        //mBluetoothGatt = bluetoothAdapter.getRemoteDevice(device.getBandageAddress()).connectGatt(this, true, myCallback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(CustomActions.STOP_FOREGROUND_SERVICE)){
            stopForeground(true);
            stopSelf();
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server in service.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        gatt.discoverServices());
                broadcastUpdate(ACTION_GATT_CONNECTED);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server in service.");
                // Disable notificiations upon disconnect
                SetEnableCharacteristicNotifications(
                        gatt.getService(SmartBandageGatt.UUID_SMART_BANDAGE_SERVICE)
                                .getCharacteristic(SmartBandageGatt.UUID_READINGS), false);
                broadcastUpdate(ACTION_GATT_DISCONNECTED);

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services Discovered YAAY");
                gatt.requestMtu(256);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            BluetoothGattCharacteristic bluetoothGattCharacteristic;
            boolean flag = true;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("Application MTU size updated from service: " + Integer.toString(mtu));

                BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(SampleGattAttributes.SMART_BANDAGE_SERVICE));
                List<BluetoothGattCharacteristic> charas = service.getCharacteristics();
                for ( BluetoothGattCharacteristic chara : charas ){
                    if (chara.getUuid().equals(SmartBandageGatt.UUID_READINGS)) {
                        continue;
                    }

                    bleReadQueue.add(chara);
                }
                readingCharacteristic(bleReadQueue);

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
                if(bleReadQueue.peek()!= null) {
                    System.out.println("More to read");
                    readingCharacteristic(bleReadQueue);
                } else if (readingsAvailable) {
                    readingsAvailable = false;
                    //after you have read all of the current characteristics, can enable notifications
                    SetEnableCharacteristicNotifications(
                            gatt.getService(SmartBandageGatt.UUID_SMART_BANDAGE_SERVICE)
                                    .getCharacteristic(SmartBandageGatt.UUID_READINGS), true);
                    broadcastUpdate(CustomActions.SMART_BANDAGE_READINGS_AVAILABLE, characteristic);
                }

            } else {
                System.err.println("BLE read failed");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("BLE write succeeded");
                if(bleReadQueue.peek()!= null) {
                    System.out.println("More to read");
                    readingCharacteristic(bleReadQueue);
                }
            } else {
                System.err.println("BLE write failed" + String.valueOf(status));
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


    private boolean readingCharacteristic(Queue<BluetoothGattCharacteristic> characteristicQueue) {
        BluetoothGattCharacteristic characteristic;
        characteristic = characteristicQueue.remove();

        if (characteristic.getUuid().equals(SmartBandageGatt.UUID_SYSTIME)) {
            // The sys_time attribute is different - write it with the current time
            int time = (int)(Calendar.getInstance().getTimeInMillis()/1000);
            byte[] value = new byte[Integer.SIZE/Byte.SIZE];
            for (int i = 0; i < Integer.SIZE/Byte.SIZE; ++i) {
                value[i] = (byte)( (time >> i*8) & 0xFF );
            }
            characteristic.setValue(value);
            return mBluetoothGatt.writeCharacteristic(characteristic);
        } else if (!mBluetoothGatt.readCharacteristic(characteristic)) {
            System.err.println("Failed to read characteristic");
            return false;
        } else {
            System.out.println("Characteristic read");
        }

        return true;

//        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//            return mBluetoothGatt.readCharacteristic(characteristic);
//        } else {
//
//        }
    }

    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG);
    private boolean SetEnableCharacteristicNotifications(BluetoothGattCharacteristic characteristic, boolean enable ) {

        if (enable) {
            System.out.println("Enabling notifications for characteristic " + characteristic.getUuid().toString());
        } else {
            System.out.println("Disabling notifications for characteristic " + characteristic.getUuid().toString());
        }

//        mNotifyCharacteristic = characteristic;
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

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        Log.i(TAG,"SAMPLE GATT ATTRIBUTE UUID: " + (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null)));
        if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Temperature Value") {
            final byte[] data = characteristic.getValue();
            Log.i(TAG, "TEMP: " + characteristic.getValue().toString());
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.setAction(CustomActions.BANDAGE_TEMP_AVAILABLE);
            Log.i(TAG, "TEMP value to send: " + characteristic.getValue());
            intent.putExtra("DATA_ARRAY", ArrayPasser.pack(SmartBandage.parseTemp(characteristic.getValue())));
            sendBroadcast(intent);
        }

        if  (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Humidity Value"){
            final byte[] data = characteristic.getValue();
            Log.i(TAG, "HUMIDITY: " + characteristic.getValue().toString());
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.setAction(CustomActions.BANDAGE_HUMIDITY_AVAILABLE);
            intent.putExtra("DATA_ARRAY",ArrayPasser.pack(SmartBandage.parseHumidity(characteristic.getValue())));
            sendBroadcast(intent);

        }

        if (SampleGattAttributes.SMART_BANDAGE_ID.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.BANDAGE_ID_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseID(characteristic.getValue()));
            sendBroadcast(intent);

        }

        if (SampleGattAttributes.SMART_BANDAGE_STATE.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.BANDAGE_STATE_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseState(characteristic.getValue()));
            sendBroadcast(intent);

        }

        if(SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Battery Charge"){
            intent.setAction(CustomActions.BANDAGE_BATT_CHRG_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseBattery(characteristic.getValue()));
            sendBroadcast(intent);

        }

        if (SampleGattAttributes.SMART_BANDAGE_EXTERNAL_POWER.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.EXT_POWER_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseExtPower(characteristic.getValue()));
            sendBroadcast(intent);

        }

        if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Moisture Map"){
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.setAction(CustomActions.MOISTURE_DATA_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseMoisture(characteristic.getValue()));
            sendBroadcast(intent);

        }

        if (SampleGattAttributes.SMART_BANDAGE_SYS_TIME.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.SYS_TIME_DATA_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseSysTime(characteristic.getValue()));
            sendBroadcast(intent);

        }


        if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Readings"){
            readingsAvailable = true;
            intent.setAction(CustomActions.SMART_BANDAGE_READINGS_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseReadings(characteristic.getValue()));
            broadcastQueue.add(intent);

        }

        if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Reading Size"){
            intent.setAction(CustomActions.SMART_BANDAGE_READING_SIZE_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseReadingSize(characteristic.getValue()));
            broadcastQueue.add(intent);

        }

        if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Reading Count"){
            intent.setAction(CustomActions.SMART_BANDAGE_READING_COUNT_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseReadingCount(characteristic.getValue()));
            broadcastQueue.add(intent);
        }

        if (SampleGattAttributes.SMART_BANDAGE_GREFT_TIME.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.SMART_BANDAGE_GREFT_TIME_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseSysTime(characteristic.getValue()));
            broadcastQueue.add(intent);

        }

        if (SampleGattAttributes.SMART_BANDAGE_DATA_OFFSETS.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.SMART_BANDAGE_DATA_OFFSETS_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseSysTime(characteristic.getValue()));
            broadcastQueue.add(intent);

        }
        /*
        while (broadcastQueue.size() != 0) {
            intent = broadcastQueue.remove();
            sendBroadcast(intent);
        }*/
    }
}