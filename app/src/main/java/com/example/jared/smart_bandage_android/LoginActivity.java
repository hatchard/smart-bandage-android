package com.example.jared.smart_bandage_android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.View;

import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends Activity  {
    Button loginButton;
    EditText userName, password, bandageID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginButton=(Button)findViewById(R.id.login_button);
        userName=(EditText)findViewById(R.id.enterName);
        password=(EditText)findViewById(R.id.enterPassword);
        bandageID=(EditText)findViewById(R.id.bandageID);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        // I think this will have the start up page be the scanning page, unless there is already
        // a connected device, in which case it will default to the bandage reading page

        /*
        if(findViewById(R.id.connectionStatus) != null) {
            Intent intent = new Intent(this, DisplayBandageReadingsActivity.class);
            startActivity(intent);
        }
        */



    }

    private void attemptLogin() {
        userName.setError(null);
        String usernameString = userName.getText().toString();
        password.setError(null);
        String passwordString = password.getText().toString();
        bandageID.setError(null);
        String bandageString = bandageID.getText().toString();

        if (TextUtils.isEmpty(usernameString)) {
            userName.setError(getString(R.string.error_field_required));
        }

        else if (TextUtils.isEmpty(passwordString)) {
            password.setError(getString(R.string.error_field_required));
        }

        else if (TextUtils.isEmpty(bandageString)) {
            bandageID.setError(getString(R.string.error_field_required));
        }

        else {
            // TODO Get user info from internet
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

}