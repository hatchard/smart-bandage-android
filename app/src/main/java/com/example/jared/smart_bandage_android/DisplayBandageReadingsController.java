package com.example.jared.smart_bandage_android;

import android.content.Intent;

/**
 * Created by Me on 2016-03-11.
 */
public class DisplayBandageReadingsController {
    private DisplayBandageReadingsActivity activity;

    public DisplayBandageReadingsController(DisplayBandageReadingsActivity activity) {
        this.activity = activity;
    }

    //setBandageReadingsViewItemOnClick(){

    //}

    public void viewNewConnectionOnClick() {
        Intent intent = new Intent(activity, MainActivity.class);
        activity.startActivity(intent);
    }

    public void viewAdvancedViewOnClick(){
        Intent intent = new Intent(activity, ConnectedDevicesActivity.class);
        activity.startActivity(intent);
    }
}
