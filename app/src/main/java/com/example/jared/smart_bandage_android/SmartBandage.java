package com.example.jared.smart_bandage_android;

import android.bluetooth.le.ScanRecord;
import android.os.ParcelUuid;

import java.io.Serializable;

/**
 * Created by Jared on 2/20/2016.
 */
public class SmartBandage implements Serializable{
    public static final ParcelUuid BANDAGE_SERVICE = ParcelUuid.fromString("0000f0f0-0000-1000-8000-00805f9b34fb");

    private String bandageName;
    private String bandageAddress;

    public SmartBandage(ScanRecord record,String bandageAddress) {
        this.bandageAddress = bandageAddress;
        this.bandageName = record.getDeviceName();
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

}
