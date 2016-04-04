package com.example.jared.smart_bandage_android;


/**
 * Created by Me on 2016-03-12.
 */
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SendData {
    public void insertToDatabase(final String record_type, final String bID, final String sID, final String time, final String value){
        class SendPostReqAsyncTask extends AsyncTask<String, Void, String> {
            @Override
            protected String doInBackground(String... params) {
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("record_type", record_type));
                nameValuePairs.add(new BasicNameValuePair("bandage_id", bID));
                nameValuePairs.add(new BasicNameValuePair("sensor_id", sID));
                nameValuePairs.add(new BasicNameValuePair("creation_time", time));
                nameValuePairs.add(new BasicNameValuePair("value", value));

                try {
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(
                            "http://www.jaredcuglietta.ca/uploader.php");
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpResponse response = httpClient.execute(httpPost);
                    if (response.getStatusLine().getStatusCode() != 200) {
                        Log.i("help", response.getStatusLine().toString());
                        HttpEntity entity = response.getEntity();
                        Log.i("help", entity.toString());
                    }
                } catch (ClientProtocolException e) {
                    Log.i("help", e.getMessage());

                } catch (IOException e) {
                    Log.i("help1", e.getMessage());

                }
                return "success";
            }
        }
        SendPostReqAsyncTask sendPostReqAsyncTask = new SendPostReqAsyncTask();
        sendPostReqAsyncTask.execute(record_type, bID, sID,time,value);
    }
}