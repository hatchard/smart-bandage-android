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
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SmartBandageConnService extends Service {
    private static final String TAG = SmartBandageConnService.class.getSimpleName();
    private static final String EXTRA_DATA = "EXTRA_DATA";
    HashMap<String,SmartBandage> rememberedBandages;
    BluetoothAdapter bluetoothAdapter;
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
                .setContentTitle("Foreground Service")
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
            BluetoothDevice device =  bluetoothAdapter.getRemoteDevice(key);
            device.connectGatt(this, true, mGattCallback);
        }
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
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        gatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services Discovered YAAY");

                BluetoothGattService service = gatt.getService(UUID.fromString(SampleGattAttributes.SMART_BANDAGE_SERVICE));
                List<BluetoothGattCharacteristic> charas = service.getCharacteristics();
                for ( BluetoothGattCharacteristic chara : charas){
                    Log.i(TAG, SampleGattAttributes.lookup(chara.getUuid().toString(), "UNKNOWN"));
                    gatt.setCharacteristicNotification(chara, true);
                    List<BluetoothGattDescriptor> descriptors = chara.getDescriptors();
                    for (BluetoothGattDescriptor descriptor : descriptors) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Read Characteristic");
                broadcastUpdate(CustomActions.DATA_AVAILABLE,characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i(TAG,"Characteristic Changed");
            //Here is where broadcasts will be sent containing information when characteristic is updated
            broadcastUpdate(CustomActions.DATA_AVAILABLE,characteristic);
        }
    };

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent();

        if (SampleGattAttributes.SMART_BANDAGE_TEMP.equals(characteristic.getUuid())) {
            intent.setAction(CustomActions.BANDAGE_TEMP_AVAILABLE);
            intent.putExtra("EXTRA_DATA",SmartBandage.parseTemp(characteristic));

        } else if (SampleGattAttributes.SMART_BANDAGE_HUMIDITY.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.BANDAGE_HUMIDITY_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseHumidity(characteristic));
        } else if (SampleGattAttributes.SMART_BANDAGE_ID.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.BANDAGE_ID_AVAILABLE);

        } else if (SampleGattAttributes.SMART_BANDAGE_STATE.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.BANDAGE_STATE_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseState(characteristic));

        } else if (SampleGattAttributes.SMART_BANDAGE_BATTERY_CHRG.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.BANDAGE_BATT_CHRG_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseBattery(characteristic));

        } else if (SampleGattAttributes.SMART_BANDAGE_EXTERNAL_POWER.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.EXT_POWER_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseExtPower(characteristic));

        } else if (SampleGattAttributes.SMART_BANDAGE_MOISTURE_MAP.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.MOISTURE_DATA_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseMoisture(characteristic));

        } else if (SampleGattAttributes.SMART_BANDAGE_SYS_TIME.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.SYS_TIME_DATA_AVAILABLE);
            intent.putExtra("EXTRA_DATA", SmartBandage.parseSysTime(characteristic));

        } else {

        }
        sendBroadcast(intent);
    }
}
