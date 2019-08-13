package com.example.serverapplock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        final SharedPreferences account = getSharedPreferences("ACCOUNT", MODE_PRIVATE);
        final SharedPreferences.Editor aEditor = account.edit();
        if(account.contains("username") && account.contains("password")){ //check if already logged in
            Intent startAppList = new Intent(MainActivity.this, AppList.class);
            startActivity(startAppList);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText etUsername = findViewById(R.id.usernameField);
        final EditText etPassword = findViewById(R.id.passwordField);

        Button confirm = findViewById(R.id.Confirm);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = etUsername.getText().toString();
                final String password = etPassword.getText().toString();

                // Response received from the server
                Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            String JSONUsername = jsonResponse.getString("username");
                            String JSONPassword = jsonResponse.getString("password");

                            if (success) {
                                aEditor.putString("username", JSONUsername);
                                aEditor.putString("password", JSONPassword);
                                aEditor.apply();
                                Intent startAppList = new Intent(MainActivity.this, AppList.class);
                                startActivity(startAppList);
                                startService();
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setMessage("Login Failed")
                                        .setNegativeButton("Retry", null)
                                        .create()
                                        .show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };

                LoginRequest loginRequest = new LoginRequest(username, password, responseListener);
                RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                queue.add(loginRequest);
            }
        });
        Button register = findViewById(R.id.Register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startRegistration = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(startRegistration);
            }
        });
    }
    public void startService() {

        //Don't forget to enable Usage Access in settings
        Intent serviceIntent = new Intent(this, ExampleService.class);
        serviceIntent.putExtra("inputExtra", "Description goes here");

        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, ExampleService.class);
        stopService(serviceIntent);
    }
}