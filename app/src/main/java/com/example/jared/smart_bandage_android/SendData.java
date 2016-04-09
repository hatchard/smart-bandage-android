package com.example.jared.smart_bandage_android;

/**
 * Created by Me on 2016-03-12.
 */

import android.os.AsyncTask;
import android.util.Log;
import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class SendData {

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
                Log.i("BulkUploadResponse", response.getStatusLine().toString());
                Log.i("BulkUploadResponse", EntityUtils.toString(response.getEntity()));
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