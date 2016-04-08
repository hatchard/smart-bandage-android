package com.example.jared.smart_bandage_android;

/**
 * Created by Me on 2016-03-09.
 */

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class BandageReadingAdapter extends ArrayAdapter<DisplayModel> {

    private final Context context;
    private final ArrayList<DisplayModel> arrayList;

    public BandageReadingAdapter(Context context, ArrayList<DisplayModel> arrayList){
        super(context, R.layout.data, arrayList);
        this.context=context;
        this.arrayList=arrayList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // 1. Create inflater
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // 2. Get rowView from inflater

        View rowView;

        rowView = inflater.inflate(R.layout.data, parent, false);

        // 3. Get icon,title & counter views from the rowView
        ImageView imgView = (ImageView) rowView.findViewById(R.id.item_icon);
        TextView titleView = (TextView) rowView.findViewById(R.id.item_title);
        TextView counterView = (TextView) rowView.findViewById(R.id.item_data);

        // 4. Set the text for textView
        imgView.setImageResource(arrayList.get(position).getIcon());
        titleView.setText(arrayList.get(position).getTitle());
        counterView.setText(arrayList.get(position).getBandageData());


        // 5. retrn rowView
        return rowView;
    }
}