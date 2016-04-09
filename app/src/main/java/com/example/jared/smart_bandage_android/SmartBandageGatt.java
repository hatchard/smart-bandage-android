package com.example.jared.smart_bandage_android;

import java.util.UUID;
import static java.util.UUID.fromString;

/**
 * Created by michaelblouin on 3/15/2016.
 */
public class SmartBandageGatt {
    public final static UUID

        UUID_SMART_BANDAGE_SERVICE  = fromString("0000f0f0-0000-1000-8000-00805f9b34fb"),
        UUID_SYSTIME                = fromString("0000f0f8-0000-1000-8000-00805f9b34fb"),
        UUID_READINGS               = fromString("0000f0f9-0000-1000-8000-00805f9b34fb"),
        UUID_READING_DATA_OFFSETS   = fromString("0000f0fc-0000-1000-8000-00805f9b34fb");

}