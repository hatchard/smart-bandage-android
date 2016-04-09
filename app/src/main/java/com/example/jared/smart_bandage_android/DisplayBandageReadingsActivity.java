package com.example.jared.smart_bandage_android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;


public class DisplayBandageReadingsActivity extends AppCompatActivity {
    private static final String TAG = DisplayBandageReadingsActivity.class.getSimpleName();

    Context context;
    String bandageAddr;
    DisplayModel displayModelTemperature = DisplayModels.getInstance().getTemperatureDM();
    DisplayModel displayModelHumidity = DisplayModels.getInstance().getHumidityDM();
    DisplayModel displayModelMoisture = DisplayModels.getInstance().getMoistureDM();
    DecimalFormat format = new DecimalFormat("#.#");
    public static Map<String,SmartBandage> deviceList;
    private SmartBandage bandage;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_bandage_readings);
        context = this;
        deviceList = SmartBandageConnService.getBandages();
        bandageAddr = (String) getIntent().getSerializableExtra(CustomActions.CURRENT_BANDAGE);
        bandage = deviceList.get(bandageAddr);
        updateActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sb_menu, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_connection:
                viewNewConnection();
                return true;
            case R.id.advanced_view:
                viewAdvancedView();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            String intentBandage = (String) intent.getSerializableExtra(CustomActions.CURRENT_BANDAGE);
            if (null == intentBandage || !intentBandage.equals(bandageAddr)) {
                return;
            }

            if (null == bandage) {
                deviceList = SmartBandageConnService.getBandages();
                bandage = deviceList.get(bandageAddr);
            }

            if (ACTION_GATT_CONNECTED.equals(action)) {

            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {

            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothGatt.getServices());

            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                // Toast.makeText(DisplayBandageReadingsActivity.this, intent.getStringExtra(EXTRA_DATA), Toast.LENGTH_SHORT).show();
            }

            if (CustomActions.BANDAGE_TEMP_AVAILABLE.equals(action)) {
                displayModelTemperature.setBandageData(getDisplayNumber(bandage.GetTemperatures()) + "\u00b0C");
            }

            if (CustomActions.BANDAGE_HUMIDITY_AVAILABLE.equals(action)) {
                displayModelHumidity.setBandageData(getDisplayNumber(bandage.GetHumidities()) + "% RH");
            }

            if (CustomActions.MOISTURE_DATA_AVAILABLE.equals(action)) {
                displayModelMoisture.setBandageData(getDisplayNumber(bandage.GetMoistures()) + "%");
            }

            if(CustomActions.SMART_BANDAGE_READING_COUNT_AVAILABLE.equals(action)){
                String values = intent.getStringExtra("EXTRA_DATA");
                Log.i (TAG, "reading count" + values);
            }

            if (CustomActions.SMART_BANDAGE_READING_SIZE_AVAILABLE.equals(action)) {
                String values = intent.getStringExtra("EXTRA_DATA");
                Log.i (TAG, "reading size" + values);
            }

            updateActivity();
        }
    };

    public void updateActivity(){
        BandageReadingAdapter adapter = new BandageReadingAdapter(this, generateData());
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);// if extending Activity
    }

    private ArrayList<DisplayModel> generateData() {
        ArrayList<DisplayModel> models = new ArrayList<DisplayModel>();

        if (null != bandage) {
            displayModelTemperature.setBandageData(getDisplayNumber(bandage.GetTemperatures()) + "\u00b0C");
            displayModelHumidity.setBandageData(getDisplayNumber(bandage.GetHumidities()) + "% RH");
            displayModelMoisture.setBandageData(getDisplayNumber(bandage.GetMoistures()) + "%");
        }

        models.add(displayModelTemperature);
        models.add(displayModelHumidity);
        models.add(displayModelMoisture);

        return models;
    }

    private String getDisplayNumber(ReadingList list) {
        Double value = list.average();
        if (value.isNaN()) {
            return "";
        }

        return format.format(value);
    }

    public void viewNewConnection() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }


    public void viewAdvancedView(){
        Intent intent = new Intent(this, ConnectedDevicesAdvancedActivity.class);
        startActivity(intent);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        intentFilter.addAction(CustomActions.ACTION_GATT_CONNECTED);
        intentFilter.addAction(CustomActions.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(CustomActions.BANDAGE_HUMIDITY_AVAILABLE);
        intentFilter.addAction(CustomActions.BANDAGE_TEMP_AVAILABLE);
        intentFilter.addAction(CustomActions.MOISTURE_DATA_AVAILABLE);
        intentFilter.addAction(CustomActions.SMART_BANDAGE_READINGS_AVAILABLE);
        intentFilter.addAction(CustomActions.SMART_BANDAGE_READING_SIZE_AVAILABLE);
        intentFilter.addAction(CustomActions.SMART_BANDAGE_READING_COUNT_AVAILABLE);
        intentFilter.addAction(CustomActions.SMART_BANDAGE_DATA_OFFSETS_AVAILABLE);

        return intentFilter;
    }
}