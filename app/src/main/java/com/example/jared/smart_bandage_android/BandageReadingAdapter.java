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

/*
Class used to create view for the display bandage readings activity

 */
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
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView;
        rowView = inflater.inflate(R.layout.data, parent, false);
        ImageView imgView = (ImageView) rowView.findViewById(R.id.item_icon);
        TextView titleView = (TextView) rowView.findViewById(R.id.item_title);
        TextView counterView = (TextView) rowView.findViewById(R.id.item_data);
        imgView.setImageResource(arrayList.get(position).getIcon());
        titleView.setText(arrayList.get(position).getTitle());
        counterView.setText(arrayList.get(position).getBandageData());
        return rowView;
    }
}