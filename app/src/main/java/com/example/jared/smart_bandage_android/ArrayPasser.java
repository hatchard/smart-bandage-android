package com.example.jared.smart_bandage_android;

import android.text.TextUtils;

/**
 * Created by Me on 2016-03-24.
 */

//http://stackoverflow.com/questions/4166897/how-do-you-pass-a-float-array-between-activities-in-android
    //Falmarri, Nov 12, 2010
public class ArrayPasser {

    public static String pack(float[] data) {
        StringBuilder sb = new StringBuilder();
        final int length = data.length;
        for (int i = 0; i < length; i++) {
            sb.append(data[i]);
            if (i < (length - 1)) {
                sb.append(':');
            }
        }
        return sb.toString();
    }

    public static float[] unpack(String str) {
        if (TextUtils.isEmpty(str)) {
            return new float[0]; // or null depending on your needs
        } else {
            String[] srtData = TextUtils.split(str, ":");
            final int length = srtData.length;
            float[] result = new float[length];
            for (int i = 0; i < length; i++) {
                result[i] = Float.parseFloat(srtData[i]);
            }
            return result;
        }
    }
}