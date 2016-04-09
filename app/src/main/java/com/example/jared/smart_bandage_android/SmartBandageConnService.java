package com.example.jared.smart_bandage_android;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class SmartBandageConnService extends Service {
    private static final String TAG = SmartBandageConnService.class.getSimpleName();
    private static HashMap<String,SmartBandage> rememberedBandages;

    private BluetoothAdapter bluetoothAdapter;
    private static SmartBandageConnService service;

    public SmartBandageConnService() {
        rememberedBandages = new HashMap<>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        service = this;
        super.onCreate();
        FileIO f = new FileIO();
        String json = f.readFile(getFilesDir() + FileIO.SAVE);

        HashMap<String, SmartBandage> deserializer = f.gsonSmartBandageHashMapDeserializer(json);

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

        for (String key: deserializer.keySet()) {
            if (!rememberedBandages.containsKey(key)) {
                rememberedBandages.put(key, null);
            }
        }

        for (String key : rememberedBandages.keySet()){
            Log.i(TAG,"Device " + key );
            SmartBandage smartBandage = rememberedBandages.get(key);
            if (null == smartBandage) {
                smartBandage = new SmartBandage(this, bluetoothAdapter.getRemoteDevice(key));
                rememberedBandages.put(key, smartBandage);
            }
        }
        broadcastUpdate(CustomActions.SERVICE_STARTED);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
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

    public static Map<String, SmartBandage> getBandages() {
        if (null == rememberedBandages) {
            rememberedBandages = new HashMap<>();
        }

        return rememberedBandages;
    }

    public static SmartBandage addDevice(String key) {
        if (getBandages().containsKey(key)) {
            return rememberedBandages.get(key);
        }

        if (null == service) {
            rememberedBandages.put(key, null);
            return null;
        }

        return service.addDeviceToInstance(key);
    }

    private SmartBandage addDeviceToInstance(String key) {
        if (null == bluetoothAdapter) {
            return null;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(key);
        if (null == device) {
            return null;
        }

        SmartBandage bandage = new SmartBandage(this, device);
        rememberedBandages.put(key, bandage);

        return bandage;
    }
}