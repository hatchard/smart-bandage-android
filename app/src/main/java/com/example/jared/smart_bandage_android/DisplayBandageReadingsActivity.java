package com.example.jared.smart_bandage_android;

import android.widget.ListView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
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
        updateActivity();
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

            if (CustomActions.ACTION_GATT_CONNECTED.equals(action)) {
                Toast toast = Toast.makeText(context, "Smart Bandage Connected", Toast.LENGTH_SHORT);
                toast.show();
            } else if (CustomActions.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast toast = Toast.makeText(context, "Smart Bandage Disconnected", Toast.LENGTH_SHORT);
                toast.show();
            }

            updateActivity();
        }
    };

    public void updateActivity(){
        BandageReadingAdapter adapter = new BandageReadingAdapter(this, generateData());
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);

        TextView statusView = (TextView) findViewById(R.id.statusTextView);

        listView.setAdapter(adapter);// if extending Activity
        if (null != bandage) {
            statusView.setText("Status: " + (bandage.getBandageConnectionStatus() ? "Connected" : "Disconnected"));
        } else {
            statusView.setText("Status: Disconnected");
        }
    }

    private ArrayList<DisplayModel> generateData() {
        ArrayList<DisplayModel> models = new ArrayList<DisplayModel>();

        updateDisplayValues();

        models.add(displayModelTemperature);
        models.add(displayModelHumidity);
        models.add(displayModelMoisture);

        return models;
    }

    private void updateDisplayValues() {
        if (null != bandage) {
            displayModelTemperature.setBandageData(getDisplayNumber(bandage.GetTemperatures(), "\u00b0C"));
            displayModelHumidity.setBandageData(getDisplayNumber(bandage.GetHumidities(), "% RH"));
            displayModelMoisture.setBandageData(getDisplayNumber(bandage.GetMoistures(), "%"));
        }
    }

    private String getDisplayNumber(ReadingList list, String units) {
        Double value = list.average();
        if (value.isNaN()) {
            return "";
        }

        return format.format(value) + units;
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