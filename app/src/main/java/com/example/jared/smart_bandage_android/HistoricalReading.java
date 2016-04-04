package com.example.jared.smart_bandage_android;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by michaelblouin on 3/30/2016.
 */
public class HistoricalReading implements Serializable {
    public static HistoricalReadingDataOffsets Offsets = null;
    public static class HistoricalReadingDataOffsets implements Serializable {
        public static final int TemperatureSize = 2;
        public static final int HumiditySize = 2;
        public static final int MoistureSize = 2;
        public static final int TimeDiffSize = 2;
        public static final int RefTimeSize = 4;

        public int TemperatureOffset;
        public int TemperatureCount;
        public int HumidityOffset;
        public int HumidityCount;
        public int MoistureOffset;
        public int MoistureCount;
        public int TimeDiffOffset;
    }

    public ReadingList Temperatures = new ReadingList();
    public ReadingList Humidities = new ReadingList();
    public ReadingList Moistures = new ReadingList();
    public Date ReadingTime;

    public static HistoricalReading FromRawData(long referenceTime, byte[] data, int offset) {
        if (null == Offsets) {
            return null;
        }

        int timeDiff = ReadingList.parse16BitLittleEndian(data, offset + Offsets.TimeDiffOffset);
        if (0 == timeDiff) {
            return null;
        }

        HistoricalReading reading = new HistoricalReading(referenceTime, timeDiff);

        for (int i = 0; i < Offsets.TemperatureCount; ++i) {
            int baseOffset = HistoricalReadingDataOffsets.TemperatureSize * i + Offsets.TemperatureOffset + offset;
            reading.Temperatures.add(ReadingList.parse16BitLittleEndian(data, baseOffset) / 16.);
        }

        for (int i = 0; i < Offsets.HumidityCount; ++i) {
            int baseOffset = HistoricalReadingDataOffsets.HumiditySize * i + Offsets.HumidityOffset + offset;
            reading.Humidities.add(ReadingList.parse16BitLittleEndian(data, baseOffset) / 16.);
        }

        for (int i = 0; i < Offsets.MoistureCount; ++i) {
            int baseOffset = HistoricalReadingDataOffsets.MoistureSize * i + Offsets.MoistureOffset + offset;
            reading.Moistures.add(ReadingList.parse16BitLittleEndian(data, baseOffset) / 16.);
        }

        return reading;
    }

    public HistoricalReading(long referenceTimestamp, long timeDiff) {
        ReadingTime = new Date();
        ReadingTime.setTime((referenceTimestamp + timeDiff) * 1000);
    }
}
