package com.example.jared.smart_bandage_android;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;

/**
 * Created by Me on 2016-03-19.
 */
public class SmartBandageStateMachine {
    //states
    private static final int CONNECT = 1;
    private static final int INITIALIZE = 2;
    private static final int READ = 3;
    private static final int DISCONNECT = 4;
    //public SB_State = new SB_State();

    //switch cases for the four states
    public void handleBandageReading(int state) {
        switch ( state) {
            case CONNECT:

                //might not need this particular one as it might be doing this automatically
                //could implement a check for when this is complete, and then proceed to next state
                break;
            case INITIALIZE:
                //will need to enable indications in here, as well as do the checking for services etc
                break;
            case READ:
                //will read through everything
                break;
            case DISCONNECT:
                //disable indications
                break;
        }
    }
}
