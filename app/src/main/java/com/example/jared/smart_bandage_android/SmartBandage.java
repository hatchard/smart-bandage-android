package com.example.jared.smart_bandage_android;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanRecord;
import android.os.ParcelUuid;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;


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
    private boolean isActive;
    private boolean bandageConnectionStatus = false;

    public SmartBandage() {

    }

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
}
