package com.example.jared.smart_bandage_android;

/**
 * Created by Me on 2016-04-05.
 */
public class DisplayModels {

    private static DisplayModels instance = new DisplayModels();

    private DisplayModel temperatureDM = new DisplayModel(R.drawable.thermometer,"Temperature: ", "");
    private DisplayModel humidityDM = new DisplayModel(R.drawable.cloud,"Humidity: ", "");
    private DisplayModel moistureDM = new DisplayModel(R.drawable.raindrop, "Moisture: ", "");

    private DisplayModels() {}

    public static DisplayModels getInstance() {
        return instance;
    }

    public DisplayModel getTemperatureDM() {
        return temperatureDM;
    }

    public DisplayModel getHumidityDM() {
        return humidityDM;
    }

    public DisplayModel getMoistureDM() {
        return moistureDM;
    }


}
