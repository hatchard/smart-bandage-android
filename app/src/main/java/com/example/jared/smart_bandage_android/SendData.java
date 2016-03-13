package com.example.jared.smart_bandage_android;


/**
 * Created by Me on 2016-03-12.
 */


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

public class SendData extends AsyncTask<JSONObject, JSONObject, JSONObject> {

    String url = "http://www.jaredcuglietta.ca/login.php"; //TODO no host name known right now

    @Override
    protected JSONObject doInBackground(JSONObject... data) {
        JSONObject json = data[0]; //only takes the first object in the array right now
        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 100000);

        JSONObject jsonResponse = null;
        HttpPost post = new HttpPost(url);
        try {
            StringEntity se = new StringEntity("json="+json.toString());
            post.addHeader("content-type", "application/x-www-form-urlencoded");
            post.setEntity(se);

            HttpResponse response;
            response = client.execute(post);
            String resFromServer = org.apache.http.util.EntityUtils.toString(response.getEntity());

            jsonResponse=new JSONObject(resFromServer);
            Log.i("Response from server", jsonResponse.getString("msg"));
        } catch (Exception e) { e.printStackTrace();}

        return jsonResponse;
    }

}