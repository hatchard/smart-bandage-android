package com.example.jared.smart_bandage_android;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothSocket;

import java.io.DataInputStream;
import java.io.InputStream;

/**
 * Created by Me on 2016-03-19.
 */
public class SmartBandageStateMachine {
    //states
    private static final int READ_CURRENT = 1;
    private static final int READ_HISTORICAL = 2;
    private static final int DISCONNECT = 3;
    //public SB_State = new SB_State();

    //switch cases for the four states
    public void handleBandageReading(int state) {
        switch ( state) {
            case READ_CURRENT:
                // Read through the current characteristicss, wait till one is returned till read the next

                break;
            case READ_HISTORICAL:
                //will need to enable indications in here, as well as do the checking for services etc

                break;
            case DISCONNECT:
                //disable indications
                break;
        }
    }
}
