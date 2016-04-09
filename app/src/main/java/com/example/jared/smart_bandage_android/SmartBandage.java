package com.example.jared.smart_bandage_android;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanRecord;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import android.content.Context;

/**
 * Created by Jared on 2/20/2016.
 */

public class SmartBandage implements Serializable{
    private static final String TAG = SmartBandage.class.getSimpleName();

    public static final ParcelUuid BANDAGE_SERVICE = ParcelUuid.fromString("0000f0f0-0000-1000-8000-00805f9b34fb");

    Context context;
    private String bandageName;
    private BluetoothGatt mBluetoothGatt;
    public Queue<BluetoothGattCharacteristic> bleReadQueue = new LinkedList<>();
    private String bandageAddress;
    private HistoricalReading currentReadings = new HistoricalReading(0, 0);

    private SendData sendData = new SendData();
    private Integer BandageId;
    private SmartBandage selfRef;

    Queue<Intent> broadcastQueue = new LinkedList<>();
    Queue<HistoricalReading> sendQueue = new LinkedList<>();

    private boolean bandageConnectionStatus = false;

    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";

    public SmartBandage(ScanRecord record, String bandageAddress) {
        this.bandageAddress = bandageAddress;
        this.bandageName = record.getDeviceName();
    }

    public SmartBandage(Context context, BluetoothDevice bleDevice) {
        this.bandageAddress = bleDevice.getAddress();
        this.bandageName = bleDevice.getName();

        if (bandageAddress.equals("F1:F1:F1:F1:F1:F1")) {
            Log.i("Smart Bandage", "Set device id 14, " + bandageAddress);
            BandageId = 14;
        } else if (bandageAddress.equals("24:71:89:17:6A:21")) {
            Log.i("Smart Bandage", "Set device id 88, " + bandageAddress);
            BandageId = 88;
        } else {
            Log.i("Smart Bandage", "MAC Unknown. Set device id 88, " + bandageAddress);
            BandageId = 88;
        }

        if (null == this.bandageName) {
            bandageName = "Smart Bandage";
        }

        this.context = context;
        this.selfRef = this;
        SetBLEParams(context, bleDevice);
    }

    public void SetBLEParams(Context context, BluetoothDevice bleDevice) {
        if (null != mBluetoothGatt) {
            mBluetoothGatt.close();
        }
        mBluetoothGatt = bleDevice.connectGatt(context, true, this.mGattCallback);
    }

    public String getBandageName() {
        return bandageName;
    }


    public String getBandageAddress() {
        return bandageAddress;
    }

    public boolean getBandageConnectionStatus() {
        return this.bandageConnectionStatus;
    }

    private Double[] parseTemp(byte[] data){
        getCurrentReadings().Temperatures.clear();
        currentReadings.parseTemperatureArray(data, 0);

        return currentReadings.Temperatures.toArray(new Double[]{});
    }

    private Double[] parseHumidity(byte[] data){
        getCurrentReadings().Humidities.clear();
        currentReadings.parseHumidityArray(data, 0);

        return currentReadings.Humidities.toArray(new Double[]{});
    }
    private int parseID(byte[] data){
    return ReadingList.parse16BitLittleEndian(data, 0);
    }

    private int parseState(byte[] data){
        return ReadingList.parse16BitLittleEndian(data, 0);
    }

    private double parseBattery(byte[] data){
    int count = 0;
    double sum = 0;
    double readingValue;
    for (int i = 0; i < data.length/2; ++i) {
        count++;
        sum += (((0x0FF & data[2 * i + 1]) << 8 | (0x0FF & data[2 * i]))) / 16.;
    }
    readingValue = sum/count;
    return readingValue;
    }

    private int parseExtPower(byte[] data){
    return (0x0FF & data[0]);
    }

    private Double[] parseMoisture(byte[] data){
        getCurrentReadings().Moistures.clear();
        currentReadings.parseMoistureArray(data, 0);

        return currentReadings.Moistures.toArray(new Double[] {});
    }

    private HistoricalReading getCurrentReadings() {
        if (null == currentReadings) {
            currentReadings = new HistoricalReading(0, 0);
        }

        return currentReadings;
    }

    public ReadingList GetMoistures() {
        return getCurrentReadings().Moistures;
    }

    public ReadingList GetTemperatures() {
        return getCurrentReadings().Temperatures;
    }

    public ReadingList GetHumidities() {
        return getCurrentReadings().Humidities;
    }

    long parseSysTime(byte[] data){
        return ReadingList.parse32BitLittleEndian(data, 0);
    }

    private String parseReadingSize(byte[] data) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Reading Size: \n");
        for (int i = 0; i < data.length/2; ++i) {
            stringBuilder.append((((0x0FF & data[2 * i + 1]) << 8 | (0x0FF & data[2 * i]))));
            stringBuilder.append("bytes\n");
        }
        return stringBuilder.toString();
    }

    private String parseReadingCount(byte[] data) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Number of Available Readings: \n");
        int value = 0;
        for (int i = 0; i < data.length; ++i) {
            value |= (0x0FF & data[i]) << (8 * i);
        }
        stringBuilder.append(value);
        stringBuilder.append("bytes\n");
        return stringBuilder.toString();
    }

    private ArrayList<HistoricalReading> parseReadings(Integer bandageId, byte[] data) {
        ArrayList<HistoricalReading> returnList = new ArrayList<>();
        long referenceTime = ReadingList.parse32BitLittleEndian(data, 0);

        for (int i = 0; i < (data.length - HistoricalReading.HistoricalReadingDataOffsets.RefTimeSize)/22; ++i) {
            HistoricalReading reading = HistoricalReading.FromRawData(referenceTime, data, i * 22 + HistoricalReading.HistoricalReadingDataOffsets.RefTimeSize);

            if (null != reading) {
                reading.BandageId = bandageId;
                returnList.add(reading);
            }
        }

        return returnList;
    }

    public HistoricalReading.HistoricalReadingDataOffsets parseDataOffsets(final byte[] data) {
        final int offsetSize = 2;

        HistoricalReading.Offsets = new HistoricalReading.HistoricalReadingDataOffsets() {{
            TemperatureOffset = data[0];
            HumidityOffset = data[1];
            MoistureOffset = data[2];
            TimeDiffOffset = data[3];

            TemperatureCount = (HumidityOffset - TemperatureOffset) / offsetSize;
            HumidityCount = (MoistureOffset - HumidityOffset) / offsetSize;
            MoistureCount = (TimeDiffOffset - MoistureOffset) / offsetSize;
        }};

        return HistoricalReading.Offsets;
    }

     BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
         private Object lock = new Object();

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (null == bandageName || bandageName.length() == 0) {
                    bandageName = gatt.getDevice().getName();
                    if (null == bandageName) {
                        bandageName = "Smart Bandage";
                    }
                }

                bandageConnectionStatus = true;
                Log.i(TAG, "Connected to GATT server in service.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        gatt.discoverServices());
                broadcastUpdate(CustomActions.ACTION_GATT_CONNECTED);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bandageConnectionStatus = false;
                Log.i(TAG, "Disconnected from GATT server in service.");
                // Disable notificiations upon disconnect

                if (gatt != null) {
                    BluetoothGattService service = gatt.getService(SmartBandageGatt.UUID_SMART_BANDAGE_SERVICE);

                    if (service != null) {
                        SetEnableCharacteristicNotifications(service.getCharacteristic(SmartBandageGatt.UUID_READINGS), false);
                    }
                }

                broadcastUpdate(CustomActions.ACTION_GATT_DISCONNECTED);
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
                try {
                    synchronized (lock) {
                        lock.wait(200);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IllegalMonitorStateException e) {
                    e.printStackTrace();
                }

                Log.w("SmartBandage", "Retry service discovery");
                gatt.discoverServices();
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("Application MTU size updated from service: " + Integer.toString(mtu));

                BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(SampleGattAttributes.SMART_BANDAGE_SERVICE));
                bleReadQueue.clear();
                bleReadQueue.add(service.getCharacteristic(SmartBandageGatt.UUID_READING_DATA_OFFSETS));
                for ( BluetoothGattCharacteristic chara : service.getCharacteristics() ){
                    if (chara.getUuid().equals(SmartBandageGatt.UUID_READINGS) || chara.getUuid().equals(SmartBandageGatt.UUID_READING_DATA_OFFSETS)) {
                        continue;
                    }

                    bleReadQueue.add(chara);
                }
                readingCharacteristic(bleReadQueue);

            } else {
                System.err.println("Application MTU size update failed. Current MTU: " + Integer.toString(mtu));
                try {
                    synchronized (lock) {
                        lock.wait(200);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IllegalMonitorStateException e) {
                    e.printStackTrace();
                }

                gatt.requestMtu(256);
                System.out.println("Trying again to update mtu");
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
                } else {
                    //if there is no more to read then enable notifications to read historical data
                    //after you have read all of the current characteristics, can enable notifications
                    System.out.println("Turning on notifications ");
                    SetEnableCharacteristicNotifications(
                            gatt.getService(SmartBandageGatt.UUID_SMART_BANDAGE_SERVICE)
                                    .getCharacteristic(SmartBandageGatt.UUID_READINGS), true);
                    //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    //added in for reading historical data

                }

            } else {
                System.err.println("BLE read failed: " + characteristic.getUuid().toString() + ", status: " + Integer.toString(status));
                try {
                    synchronized (lock) {
                        lock.wait(200);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IllegalMonitorStateException e) {
                    e.printStackTrace();
                }

                gatt.readCharacteristic(characteristic);
                System.out.println("Trying again to read characteristic");
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
                try {
                    synchronized (lock) {
                        lock.wait(200);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IllegalMonitorStateException e) {
                    e.printStackTrace();
                }

                gatt.writeCharacteristic(characteristic);
                System.out.println("Trying again to read characteristic");
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
                try {
                    synchronized (lock) {
                        lock.wait(200);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IllegalMonitorStateException e) {
                    e.printStackTrace();
                }

                gatt.writeDescriptor(descriptor);
                System.out.println("Trying again to write descriptor");
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("Bluetooth descriptor read success: " + descriptor.getUuid().toString());
            } else {
                System.err.println("Bluetooth descriptor read failed: " + descriptor.getUuid().toString());
                try {
                    synchronized (lock) {
                        lock.wait(200);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IllegalMonitorStateException e) {
                    e.printStackTrace();
                }

                gatt.readDescriptor(descriptor);
                System.out.println("Trying again to read descriptor");
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
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        intent.putExtra(CustomActions.CURRENT_BANDAGE, getBandageAddress());
        context.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        intent.putExtra(CustomActions.CURRENT_BANDAGE, getBandageAddress());

        Log.i(TAG, "SAMPLE GATT ATTRIBUTE UUID: " + (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null)));
        if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Temperature Value") {
            final byte[] data = characteristic.getValue();
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.setAction(CustomActions.BANDAGE_TEMP_AVAILABLE);
            parseTemp(data);
            context.sendBroadcast(intent);
        }

        if  (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Humidity Value"){
            final byte[] data = characteristic.getValue();
            Log.i(TAG, "HUMIDITY: " + characteristic.getValue().toString());
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.setAction(CustomActions.BANDAGE_HUMIDITY_AVAILABLE);
            parseHumidity(data);
            context.sendBroadcast(intent);
        }

        if (SampleGattAttributes.SMART_BANDAGE_ID.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.BANDAGE_ID_AVAILABLE);
            intent.putExtra("EXTRA_DATA", BandageId = parseID(characteristic.getValue()));
            context.sendBroadcast(intent);
        }

        if (SampleGattAttributes.SMART_BANDAGE_STATE.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.BANDAGE_STATE_AVAILABLE);
            intent.putExtra("EXTRA_DATA", parseState(characteristic.getValue()));
            context.sendBroadcast(intent);
        }

        if(SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Battery Charge"){
            intent.setAction(CustomActions.BANDAGE_BATT_CHRG_AVAILABLE);
            intent.putExtra("EXTRA_DATA", parseBattery(characteristic.getValue()));
            context.sendBroadcast(intent);
        }

        if (SampleGattAttributes.SMART_BANDAGE_EXTERNAL_POWER.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.EXT_POWER_AVAILABLE);
            intent.putExtra("EXTRA_DATA", parseExtPower(characteristic.getValue()));
            context.sendBroadcast(intent);
        }

        if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Moisture Map"){
            final byte[] data = characteristic.getValue();
            Log.i(TAG, "HUMIDITY: " + characteristic.getValue().toString());
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.setAction(CustomActions.MOISTURE_DATA_AVAILABLE);
            parseMoisture(data);
            context.sendBroadcast(intent);
        }

        if (SampleGattAttributes.SMART_BANDAGE_SYS_TIME.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.SYS_TIME_DATA_AVAILABLE);
            intent.putExtra("EXTRA_DATA", parseSysTime(characteristic.getValue()));
            context.sendBroadcast(intent);
        }


        if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Readings"){
            intent.setAction(CustomActions.SMART_BANDAGE_READINGS_AVAILABLE);
            ArrayList<HistoricalReading> readings = parseReadings(BandageId, characteristic.getValue());
            intent.putExtra("EXTRA_DATA", readings);
            context.sendBroadcast(intent);

            sendData.queueData(readings);
        }

        if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Reading Size"){
            intent.setAction(CustomActions.SMART_BANDAGE_READING_SIZE_AVAILABLE);
            intent.putExtra("EXTRA_DATA", parseReadingSize(characteristic.getValue()));
            broadcastQueue.add(intent);
        }

        if (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Reading Count"){
            intent.setAction(CustomActions.SMART_BANDAGE_READING_COUNT_AVAILABLE);
            intent.putExtra("EXTRA_DATA", parseReadingCount(characteristic.getValue()));
            broadcastQueue.add(intent);
        }

        if (SmartBandageGatt.UUID_READING_DATA_OFFSETS.equals(characteristic.getUuid())){
            intent.setAction(CustomActions.SMART_BANDAGE_DATA_OFFSETS_AVAILABLE);
            intent.putExtra("EXTRA_DATA", parseDataOffsets(characteristic.getValue()));
            broadcastQueue.add(intent);
        }
    }

    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG);
    private boolean SetEnableCharacteristicNotifications(BluetoothGattCharacteristic characteristic, boolean enable ) {

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
