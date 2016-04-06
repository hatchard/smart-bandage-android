package com.example.jared.smart_bandage_android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanRecord;
import android.content.Intent;
import android.os.ParcelUuid;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import android.content.Context;


/**
 * Created by Jared on 2/20/2016.
 */
public class SmartBandage implements Serializable{
    private static final String TAG = SmartBandage.class.getSimpleName();
    public static final ParcelUuid BANDAGE_SERVICE = ParcelUuid.fromString("0000f0f0-0000-1000-8000-00805f9b34fb");
    public final static boolean CONNECTED = true;
    public final static boolean DISCONNECTED = false;
    Context context;
    BluetoothAdapter bluetoothAdapter;
    private String bandageName;
    BluetoothGatt mBluetoothGatt;
    public Queue<BluetoothGattCharacteristic> bleReadQueue = new LinkedList<>();
    private String bandageAddress;


    private SendData sendData;
    private Integer BandageId;
    private boolean isActive;
    Queue<Intent> broadcastQueue = new LinkedList<>();
    Queue<HistoricalReading> sendQueue = new LinkedList<>();
    private boolean bandageConnectionStatus = false;
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";

    public SmartBandage(ScanRecord record,String bandageAddress) {
        this.bandageAddress = bandageAddress;
        this.bandageName = record.getDeviceName();
    }

    public SmartBandage(BluetoothGatt gatt,boolean connStatus) {
        this.bandageAddress = gatt.getDevice().getAddress();
        this.bandageName = gatt.getDevice().getName();
        this.bandageConnectionStatus = connStatus;
    }

    public String getBandageName() {
        return bandageName;
    }


    public String getBandageAddress() {
        return bandageAddress;
    }


    public void setBandageConnectionStatus(boolean status) {
        this.bandageConnectionStatus = status;
    }

    public boolean getBandageConnectionStatus() {
        return this.bandageConnectionStatus;
    }


     float[] parseTemp(byte[] data){

        //int count = 0;
        float[] tempArray = new float[data.length/2];
        double temp;
        //double readingValue;
        for (int i = 0; i < data.length/2; ++i) {
           // count++;
            temp = (((0x0FF & data[2*i+1]) << 8 | (0x0FF & data[2*i])))/16.;
            tempArray[i] = (float) temp;

        }
        //readingValue = sum/count;
        return tempArray;
    }


      float[] parseHumidity(byte[] data){
        double temp;
        float[] tempArray = new float[data.length/2];
        for (int i = 0; i < data.length/2; ++i) {
            temp = (((0x0FF & data[2*i+1]) << 8 | (0x0FF & data[2*i])))/16.;
            tempArray[i] = (float) temp;
        }
        return tempArray;
    }
    int parseID(byte[] data){
        return ReadingList.parse16BitLittleEndian(data, 0);
    }
    int parseState(byte[] data){
        return ReadingList.parse16BitLittleEndian(data, 0);
    }
     double parseBattery(byte[] data){
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
     int parseExtPower(byte[] data){
        return (0x0FF & data[0]);
    }
     float[] parseMoisture(byte[] data){
        double temp;
        float[] tempArray = new float[data.length/2];
        for (int i = 0; i < data.length/2; ++i) {

            temp = (((0x0FF & data[2*i+1]) << 8 | (0x0FF & data[2*i])))/16.;
            tempArray[i] = (float) temp;
        }

        return tempArray;

    }


    long parseSysTime(byte[] data){
        return ReadingList.parse32BitLittleEndian(data, 0);
    }

    String parseReadingSize(byte[] data) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Reading Size: \n");
        for (int i = 0; i < data.length/2; ++i) {
            stringBuilder.append((((0x0FF & data[2 * i + 1]) << 8 | (0x0FF & data[2 * i]))));
            stringBuilder.append("bytes\n");
        }
        return stringBuilder.toString();
    }

    String parseReadingCount(byte[] data) {
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

    ArrayList<HistoricalReading> parseReadings(Integer bandageId, byte[] data) {
        ArrayList<HistoricalReading> returnList = new ArrayList<>();
        long referenceTime = ReadingList.parse32BitLittleEndian(data, 0);

        for (int i = 0; i < (data.length - HistoricalReading.HistoricalReadingDataOffsets.RefTimeSize)/22; ++i) {
            HistoricalReading reading = HistoricalReading.FromRawData(referenceTime, data, i * 22 + HistoricalReading.HistoricalReadingDataOffsets.RefTimeSize);

            if (null != reading) {
                // TODO: Use the actual bandage id
                reading.BandageId = bandageId;
                reading.BandageId = 14;
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

                if (gatt != null) {
                    BluetoothGattService service = gatt.getService(SmartBandageGatt.UUID_SMART_BANDAGE_SERVICE);

                    if (service != null) {
                        SetEnableCharacteristicNotifications(service.getCharacteristic(SmartBandageGatt.UUID_READINGS), false);
                    }
                }

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
                bleReadQueue.clear();
                for ( BluetoothGattCharacteristic chara : service.getCharacteristics() ){
                    if (chara.getUuid().equals(SmartBandageGatt.UUID_READINGS)) {
                        continue;
                    }

                    bleReadQueue.add(chara);
                }
                readingCharacteristic(bleReadQueue);

            } else {
                System.err.println("Application MTU size update failed. Current MTU: " + Integer.toString(mtu));
                try {
                    wait(200);
                } catch (InterruptedException e) {
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
                    wait(200);
                } catch (InterruptedException e) {
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
                    wait(200);
                } catch (InterruptedException e) {
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
                    wait(200);
                } catch (InterruptedException e) {
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
                    wait(200);
                } catch (InterruptedException e) {
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
        context.sendBroadcast(intent);
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
            intent.putExtra("DATA_ARRAY", ArrayPasser.pack(parseTemp(characteristic.getValue())));
            context.sendBroadcast(intent);
        }

        if  (SampleGattAttributes.lookup(characteristic.getUuid().toString(), null) == "Humidity Value"){
            final byte[] data = characteristic.getValue();
            Log.i(TAG, "HUMIDITY: " + characteristic.getValue().toString());
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.setAction(CustomActions.BANDAGE_HUMIDITY_AVAILABLE);
            intent.putExtra("DATA_ARRAY",ArrayPasser.pack(parseHumidity(characteristic.getValue())));
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
            intent.putExtra("DATA_ARRAY", ArrayPasser.pack(parseMoisture(characteristic.getValue())));
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

            if (null != readings && readings.size() > 0) {
                for (HistoricalReading reading: readings) {
                    sendQueue.add(reading);
                }
            }

            sendReadings();
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



    private void sendReadings() {
        if (sendQueue.size() == 0) {
            return;
        }

        if (null == sendData) {
            sendData = new SendData();
        }

        List<HistoricalReading> readings = new ArrayList<>(Arrays.asList(sendQueue.toArray(new HistoricalReading[sendQueue.size()])));
        sendQueue.removeAll(readings);
        sendData.bulkInsertToDatabase(readings);
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

}
