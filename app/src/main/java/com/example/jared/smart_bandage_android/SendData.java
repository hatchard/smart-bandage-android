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

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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

    private boolean requestInProgress = false;
    private class SendBulkReadingsAsyncTask extends AsyncTask<Void, Void, HttpResponse> {
        private List<HistoricalReading> Readings;
        public SendBulkReadingsAsyncTask(List<HistoricalReading> readings) {
            Readings = readings;
        }

        @Override
        protected HttpResponse doInBackground(Void... params) {
            Gson gson = new Gson();

            DefaultHttpClient httpClient = new DefaultHttpClient();

            HttpPost post = new HttpPost("http://www.jaredcuglietta.ca/bulkUpload.php");

            try {
                post.setEntity(new StringEntity(gson.toJson(Readings)));
            } catch (UnsupportedEncodingException e) {
                Log.i("BulkUploadError", e.getMessage());
                requestInProgress = false;
                return null;
            }

            post.setHeader("Accept", "application/json");
            post.setHeader("Content-Type", "application/json");

            HttpResponse response = null;
            try {
                requestInProgress = true;
                response = httpClient.execute(post);
//                if (response.getStatusLine().getStatusCode() != 200) {
                Log.i("BulkUploadResponse", response.getStatusLine().toString());
                Log.i("BulkUploadResponse", EntityUtils.toString(response.getEntity()));
//                }

            } catch (IOException e) {
                Log.i("BulkUploadError", e.getMessage());
            }

            requestInProgress = false;

            sendNext();
            return response;
        }
    }

    public boolean requestInProgress() {
        return requestInProgress;
    }

    public void bulkInsertToDatabase(List<HistoricalReading> readings) {
        new SendBulkReadingsAsyncTask(readings).execute();
    }

    private Queue<HistoricalReading> sendQueue = new LinkedList<>();

    public void queueData(List<HistoricalReading> readings) {
        sendQueue.addAll(readings);
        sendNext();
    }

    private void sendNext() {
        if (sendQueue.size() == 0 || requestInProgress()) {
            return;
        }

        requestInProgress = true;
        List<HistoricalReading> sendData = new ArrayList<>(Arrays.asList(sendQueue.toArray(new HistoricalReading[sendQueue.size()])));
        sendQueue.removeAll(sendData);
        bulkInsertToDatabase(sendData);
    }
}