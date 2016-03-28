package com.example.jared.smart_bandage_android;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Jared on 2/16/2016.
 */
public class FileIO {
    public static final String SAVE = "rememberedDevices.json";
    public static final String SAVE_HISTORICAL_DATA = "historicalData.json";
    private File getFile(String file_name) throws IOException {
        File f = new File(file_name);
        if (!f.exists()) {
            Log.d("File:","Not exists, creating");
            f.createNewFile();
        }
        return f;
    }

    public String readFile(String file_name) {
        try {
            File f = getFile(file_name);
            BufferedReader buf = new BufferedReader(new FileReader(f));
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();

            while ( (line = buf.readLine()) != null){
                stringBuilder.append(line);
            }
            buf.close();
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public String readHistoricalFile(String file_name) {
        try {
            File f = getFile(file_name);
            BufferedReader buf = new BufferedReader(new FileReader(f));
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();

            while ( (line = buf.readLine()) != null){
                stringBuilder.append(line);
            }
            buf.close();
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public boolean writeFile(String file_name,boolean append,String content){
        File f = null;
        try {
            f = getFile(file_name);
            int contentLength = content.length();
            BufferedWriter buf = new BufferedWriter(new FileWriter(f,append));
            buf.write(content, 0, contentLength);
            buf.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean writeHistoricalFile(String file_name,String content){
        File f = null;
        try {
            f = getFile(file_name);
            BufferedWriter buf = new BufferedWriter(new FileWriter(f,true));
            buf.write(content + "\n");
            buf.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String gsonSmartBandageHashMapSerializer (HashMap<String,SmartBandage> bandageHashMap) {
        Gson gson = new Gson();
        return gson.toJson(bandageHashMap);
    }

    //checks file system for the saved file, if there is no file or data in the file,
    //it returns a new hashmap
    public HashMap<String,SmartBandage> gsonSmartBandageHashMapDeserializer (String serializedHashMap) {
        Gson gson = new Gson();
        Type hashType = new TypeToken<Map<String,SmartBandage>>() {}.getType();
        if (serializedHashMap != null) {
            Map<String, SmartBandage> sm;
            sm = gson.fromJson(serializedHashMap, hashType);
            if (sm == null){
                return new HashMap<String,SmartBandage>();
            }
            else {
                Log.d("TAG: ", "sm is empty?: " + Boolean.toString(sm.isEmpty()));
                HashMap<String, SmartBandage> hm = new HashMap<String, SmartBandage>();
                hm.putAll(sm);
                return hm;
            }

        }
        else {
            return new HashMap<String,SmartBandage>();
        }

    }


}
