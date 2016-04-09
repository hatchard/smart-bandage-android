package com.example.jared.smart_bandage_android;

/**
 * Created by Me on 2016-03-09.
 */
public class DisplayModel {
    private int icon;
    private String title;
    private String bandageData;

    public DisplayModel(int icon, String title, String bandageData) {
        super();
        this.icon = icon;
        this.title = title;
        this.bandageData = bandageData;
    }

    public int getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public String getBandageData() {
        return bandageData;
    }

    public void setBandageData(String bandageData) {
        this.bandageData = bandageData;
    }
}
