package com.example.jared.smart_bandage_android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;


public class DisplayBandageReadingsActivity extends AppCompatActivity {
    //probably not the best way to do this
    Context context;
    SendData sendData;
    //EditText bandageID = (EditText) findViewById(R.id.bandageID);
    String bandageID = "1234";
    public static String DEVICE_LIST ="deviceList";
    public static HashMap<String,SmartBandage> deviceList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_bandage_readings);
        context = this;
        //android.os.Debug.waitForDebugger();
        deviceList = (HashMap<String,SmartBandage>) getIntent().getSerializableExtra(DEVICE_LIST);

        // 1. pass context and data to the custom adapter
        BandageReadingAdapter adapter = new BandageReadingAdapter(this, generateData());

        // if extending Activity 2. Get ListView from activity_main.xml
        ListView listView = (ListView) findViewById(R.id.listView);

        // 3. setListAdapter
        listView.setAdapter(adapter);// if extending Activity
       // setListAdapter(adapter);

        sendData = new SendData();
        sendData.insert();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sb_menu, menu);
        return true;
    }


    // Couldn't get this working in the onClick
    // TODO look into it
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
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

    // Get the data to display in the Activity.
    public String getTemperatureData() {

        return "hot";
    }

    public String getHumidityData() {

        return "humid";
    }

    public String getMoistureData() {
        return "wet";
    }

    private ArrayList<DisplayModel> generateData(){
        ArrayList<DisplayModel> models = new ArrayList<DisplayModel>();
        models.add(new DisplayModel(R.drawable.thermometer,"Temperature: ", getTemperatureData()));
        models.add(new DisplayModel(R.drawable.cloud,"Humidity: ", getHumidityData()));
        models.add(new DisplayModel(R.drawable.raindrop, "Moisture: ", getMoistureData()));

        return models;
    }

    public void viewNewConnection() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }


    public void viewAdvancedView(){
        Log.w("why", "about to go to DeviceServiceViewActivity");
        Intent intent = new Intent(this, ConnectedDevicesActivity.class);
        intent.putExtra(ConnectedDevicesActivity.DEVICE_LIST, deviceList);
       // Intent intent = new Intent(this, DeviceServiceViewActivity.class);
        startActivity(intent);
    }


}

