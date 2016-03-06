package com.example.jared.smart_bandage_android;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanRecord;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Jared on 2/20/2016.
 */
public class SmartBandage implements Serializable{
    private static final String TAG = SmartBandage.class.getSimpleName();
    public static final ParcelUuid BANDAGE_SERVICE = ParcelUuid.fromString("0000f0f0-0000-1000-8000-00805f9b34fb");
    public final static boolean CONNECTED = true;
    public final static boolean DISCONNECTED = false;
    private String bandageName;
    private String bandageAddress;
    private boolean bandageConnectionStatus = false;

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

    public void setBandageName(String bandageName) {
        this.bandageName = bandageName;
    }

    public String getBandageAddress() {
        return bandageAddress;
    }

    public void setBandageAddress(String bandageAddress) {
        this.bandageAddress = bandageAddress;
    }

    public void setBandageConnectionStatus(boolean status) {
        this.bandageConnectionStatus = status;
    }

    public boolean getBandageConnectionStatus() {
        return this.bandageConnectionStatus;
    }

    public static float parseTemp(BluetoothGattCharacteristic characteristic){

        return 1;
    }
    public static float parseHumidity(BluetoothGattCharacteristic characteristic){

        return 1;
    }
    public static int parseID(BluetoothGattCharacteristic characteristic){

        return 1;
    }
    public static int parseState(BluetoothGattCharacteristic characteristic){

        return 1;
    }
    public static float parseBattery(BluetoothGattCharacteristic characteristic){

        return 1;
    }
    public static int parseExtPower(BluetoothGattCharacteristic characteristic){

        return 1;
    }
    public static float parseMoisture(BluetoothGattCharacteristic characteristic){

        return 1;
    }
    public static int parseSysTime(BluetoothGattCharacteristic characteristic){

        return 1;
    }
}
