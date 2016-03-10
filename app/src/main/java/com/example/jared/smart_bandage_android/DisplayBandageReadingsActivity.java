package com.example.jared.smart_bandage_android;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;


public class DisplayBandageReadingsActivity extends AppCompatActivity {
    //probably not the best way to do this
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_bandage_readings);
        context = this;


        // 1. pass context and data to the custom adapter
        BandageReadingAdapter adapter = new BandageReadingAdapter(this, generateData());

        // if extending Activity 2. Get ListView from activity_main.xml
        ListView listView = (ListView) findViewById(R.id.listView);

        // 3. setListAdapter
        listView.setAdapter(adapter);// if extending Activity
       // setListAdapter(adapter);

        }

        private ArrayList<DisplayModel> generateData(){
            ArrayList<DisplayModel> models = new ArrayList<DisplayModel>();
            models.add(new DisplayModel(R.drawable.thermometer,"Temperature: ","1")); //change to icon matching
            models.add(new DisplayModel(R.drawable.cloud,"Humidity: ","2"));
            models.add(new DisplayModel(R.drawable.raindrop,"Moisture: ","12"));

            return models;
        }
}
