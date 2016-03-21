package com.example.jared.smart_bandage_android;

import java.util.UUID;
import static java.util.UUID.fromString;

/**
 * Created by michaelblouin on 3/15/2016.
 */
public class SmartBandageGatt {
    public final static UUID

            UUID_SMART_BANDAGE_SERVICE  = fromString("0000f0f0-0000-1000-8000-00805f9b34fb"),
            UUID_CURRENT_TEMPERATURE    = fromString("0000f0f1-0000-1000-8000-00805f9b34fb"),
            UUID_CURRENT_HUMIDITY       = fromString("0000f0f2-0000-1000-8000-00805f9b34fb"),
            UUID_BANDAGE_ID             = fromString("0000f0f3-0000-1000-8000-00805f9b34fb"),
            UUID_BANDAGE_STATE          = fromString("0000f0f4-0000-1000-8000-00805f9b34fb"),
            UUID_BATTERY_CHARGE         = fromString("0000f0f5-0000-1000-8000-00805f9b34fb"),
            UUID_EXT_PWR                = fromString("0000f0f6-0000-1000-8000-00805f9b34fb"),
            UUID_MOISTURE_MAP           = fromString("0000f0f7-0000-1000-8000-00805f9b34fb"),
            UUID_SYSTIME                = fromString("0000f0f8-0000-1000-8000-00805f9b34fb"),
            UUID_READINGS               = fromString("0000f0f9-0000-1000-8000-00805f9b34fb"),
            UUID_READING_SIZE           = fromString("0000f0fa-0000-1000-8000-00805f9b34fb"),
            UUID_READING_COUNT          = fromString("0000f0fb-0000-1000-8000-00805f9b34fb"),
            UUID_READING_REF_TIMESTAMP  = fromString("0000f0fc-0000-1000-8000-00805f9b34fb"),
            UUID_READING_DATA_OFFSETS   = fromString("0000f0fd-0000-1000-8000-00805f9b34fb");
}