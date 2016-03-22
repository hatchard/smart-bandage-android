/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jared.smart_bandage_android;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String SMART_BANDAGE_SERVICE = "0000f0f0-0000-1000-8000-00805f9b34fb";
    public static String SMART_BANDAGE_TEMP = "0000f0f1-0000-1000-8000-00805f9b34fb";
    public static String SMART_BANDAGE_HUMIDITY = "0000f0f2-0000-1000-8000-00805f9b34fb";
    public static String SMART_BANDAGE_ID = "0000f0f3-0000-1000-8000-00805f9b34fb";
    public static String SMART_BANDAGE_STATE = "0000f0f4-0000-1000-8000-00805f9b34fb";
    public static String SMART_BANDAGE_BATTERY_CHRG = "0000f0f5-0000-1000-8000-00805f9b34fb";
    public static String SMART_BANDAGE_EXTERNAL_POWER = "0000f0f6-0000-1000-8000-00805f9b34fb";
    public static String SMART_BANDAGE_MOISTURE_MAP = "0000f0f7-0000-1000-8000-00805f9b34fb";
    public static String SMART_BANDAGE_SYS_TIME = "0000f0f8-0000-1000-8000-00805f9b34fb";
    public static String SMART_BANDAGE_READINGS = "0000f0f9-0000-1000-8000-00805f9b34fb";
    public static String SMART_BANDAGE_READING_SIZE = "0000f0fa-0000-1000-8000-00805f9b34fb";
    public static String SMART_BANDAGE_READING_COUNT = "0000f0fb-0000-1000-8000-00805f9b34fb";
    public static String SMART_BANDAGE_GREFT_TIME = "0000f0fc-0000-1000-8000-00805f9b34fb";
    public static String SMART_BANDAGE_DATA_OFFSETS = "0000f0fd-0000-1000-8000-00805f9b34fb";


    static {
        // Define the UUIDs of the services that your bluetooth device exposes

        // Added the Smart Bandage Characteristic UUIDs
        attributes.put("0000f0f0-0000-1000-8000-00805f9b34fb", "Smart Bandage Service");
        attributes.put("0000f0f1-0000-1000-8000-00805f9b34fb", "Temperature Value");
        attributes.put("0000f0f2-0000-1000-8000-00805f9b34fb", "Humidity Value");
        attributes.put("0000f0f3-0000-1000-8000-00805f9b34fb", "Bandage ID");
        attributes.put("0000f0f4-0000-1000-8000-00805f9b34fb", "Bandage State");
        attributes.put("0000f0f5-0000-1000-8000-00805f9b34fb", "Battery Charge");
        attributes.put("0000f0f6-0000-1000-8000-00805f9b34fb", "External Power");
        attributes.put("0000f0f7-0000-1000-8000-00805f9b34fb", "Moisture Map");
        attributes.put("0000f0f8-0000-1000-8000-00805f9b34fb", "System Time");

        //TODO need to confirm correct Bandage Characteristic UUIDs for the below,
        attributes.put("0000f0f9-0000-1000-8000-00805f9b34fb", "Readings");
        attributes.put("0000f0fa-0000-1000-8000-00805f9b34fb", "Reading Size");
        attributes.put("0000f0fb-0000-1000-8000-00805f9b34fb", "Reading Count");
        attributes.put("0000f0fc-0000-1000-8000-00805f9b34fb", "Greft Time");
        attributes.put("0000f0fd-0000-1000-8000-00805f9b34fb", "Data Offsets");

        // Define the UUIDs of the characteristics for your bluetooth device
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
