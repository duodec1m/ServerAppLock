package com.example.serverapplock;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class TimeRequest extends StringRequest {
    private static final String Time_REQUEST_URL = "https://wandle-balances.000webhostapp.com/Time.php";
    private Map<String, String> params;

    public TimeRequest(String username, String password, Response.Listener<String> listener, long time, String type) {
        super(Method.POST, Time_REQUEST_URL, listener, null);
        params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
        params.put("time", String.valueOf(time));
        params.put("type", type);
    }

    @Override
    public Map<String, String> getParams() {
        return params;
    }
}