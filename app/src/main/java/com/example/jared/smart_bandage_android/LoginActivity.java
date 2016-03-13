package com.example.jared.smart_bandage_android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;

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


        /*loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(userName.getText().toString().equals("admin") &&

                        password.getText().toString().equals("admin")) {
                    Toast.makeText(getApplicationContext(), "Redirecting...",Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Wrong Credentials",Toast.LENGTH_SHORT).show();
                }
            }
        });
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