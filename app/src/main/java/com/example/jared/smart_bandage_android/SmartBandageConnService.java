package com.example.jared.smart_bandage_android;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class SmartBandageConnService extends Service {
    private static final String TAG = SmartBandageConnService.class.getSimpleName();
    HashMap<String,SmartBandage> rememberedBandages;
  //  public boolean readingsAvailable = false; //is set to true when there are historical readings available


   private SmartBandage smartBandage;

    BluetoothAdapter bluetoothAdapter;
    BluetoothGatt mBluetoothGatt;

    public SmartBandageConnService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FileIO f = new FileIO();
        String json = f.readFile(getFilesDir() +
                FileIO.SAVE);
        rememberedBandages = new HashMap<>();
        rememberedBandages = f.gsonSmartBandageHashMapDeserializer(json);
        Intent activityIntent = new Intent(this,MainActivity.class);
        activityIntent.setAction(CustomActions.MAIN_ACTION);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);
        Log.i(TAG, "Received Start Foreground Service Intent");
        Notification.Builder nBuilder = new Notification.Builder(this)
                .setContentTitle("Smart Bandage Service")
                .setContentText("Running..")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.black_circle)
                .setOngoing(true);
        Notification notification = nBuilder.build();
        startForeground(CustomActions.FOREGROUND_SERVICE_ID, notification);
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();


        for (String key : rememberedBandages.keySet()){
            Log.i(TAG,"Device " + key );
            smartBandage = rememberedBandages.get(key);
            mBluetoothGatt = bluetoothAdapter.getRemoteDevice(key).connectGatt(this, true, smartBandage.mGattCallback);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(CustomActions.STOP_FOREGROUND_SERVICE)){
            stopForeground(true);
            stopSelf();
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


}