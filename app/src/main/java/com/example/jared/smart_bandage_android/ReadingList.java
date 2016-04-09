package com.example.jared.smart_bandage_android;

import java.util.ArrayList;

/**
 * Created by michaelblouin on 3/30/2016.
 */

public class ReadingList extends ArrayList<Double> {
    public static int parse16BitLittleEndian(byte[] data, int offset) {
        return (((0x0FF & data[1 + offset]) << 8 | (0x0FF & data[offset])));
    }

    public static long parse32BitLittleEndian(byte[] data, int offset) {
        long value = 0;
        for (int i = 0; i < 4; ++i) {
            value |= (0x0FF & data[i + offset]) << (8*i);
        }
        return value;
    }

    public Double average() {
        if (size() == 0) {
            return Double.NaN;
        }
        Double sum = 0.;
        for (Double d: this) {
            sum += d;
        }
        return sum/size();
    }

}
