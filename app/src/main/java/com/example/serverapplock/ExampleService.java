package com.example.serverapplock;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.example.serverapplock.App.CHANNEL_ID;


public class ExampleService extends Service {

    TimeActionService tas;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");

        tas = new TimeActionService();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent action1Intent = new Intent(this, NotificationActionService.class);
        PendingIntent action1PendingIntent = PendingIntent.getService(this, 0, action1Intent, 0);

        Intent timeIntent = new Intent(this, tas.getClass());
        PendingIntent timePendingIntent = PendingIntent.getService(this, 0, timeIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Example Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .addAction(0, "Sync timer with server", action1PendingIntent)
                .addAction(0, "Unblock Toggle On/Off", timePendingIntent)
                .build();

        startForeground(1, notification);

        //do heavy work on a background thread
        //stopSelf();
        new Thread(new Runnable() {
            public void run() {
                doWork();
            }
        }).start();

        return START_NOT_STICKY;
    }

    private void doWork() {
        final SharedPreferences account = getSharedPreferences("ACCOUNT", MODE_PRIVATE);
        final SharedPreferences.Editor aEditor = account.edit();

        final SharedPreferences prefs = getSharedPreferences("BLOCKLIST", MODE_PRIVATE);
        Intent startBlockScreen = new Intent(this, BlockScreen.class);
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        while (true) {
            if(account.getLong("time", 0) < 0){
                aEditor.putLong("time", 0);
                aEditor.apply();
                tas.expend = false;
                tas.updateServer();
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Example Service")
                        .setContentText("Time's Up")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .build();
                nm.notify(2,notification);
            }
            else if(account.getLong("time", 0) > 29000 && account.getLong("time", 0) <= 30000){
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Example Service")
                        .setContentText(" About 30000 ms left")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .build();
                nm.notify(2,notification);
            }

            if (getTaskTopAppPackageName(this) != null && prefs.contains(getTaskTopAppPackageName(this)) && !tas.expend) {
                startActivity(startBlockScreen);
            }

            try {
                Thread.sleep(1000);
                if(tas.expend) {
                    aEditor.putLong("time", account.getLong("time", 0)-1000);
                    aEditor.apply();
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static String getTaskTopAppPackageName(Context context) {
        String packageName = "";
        // if the sdk >= 21. It can only use getRunningAppProcesses to get task top package name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usage = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> stats = usage.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);
            if (stats != null) {
                SortedMap<Long, UsageStats> runningTask = new TreeMap<Long, UsageStats>();
                for (UsageStats usageStats : stats) {
                    runningTask.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (runningTask.isEmpty()) {
                    return null;
                }
                packageName = runningTask.get(runningTask.lastKey()).getPackageName();
            }
        }
        return packageName;
    }

    public static class NotificationActionService extends IntentService {
        public NotificationActionService() {
            super(NotificationActionService.class.getSimpleName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            final SharedPreferences account = getSharedPreferences("ACCOUNT", MODE_PRIVATE);
            final SharedPreferences.Editor aEditor = account.edit();
            // Response received from the server
            Response.Listener<String> responseListener = new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        System.out.println(response);
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");
                        long JSONTime = jsonResponse.getLong("time");

                        if (success) {
                            aEditor.putLong("time", JSONTime);
                            aEditor.apply();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(NotificationActionService.this);
                            builder.setMessage("Sync with server failed")
                                    .setNegativeButton("Retry", null)
                                    .create()
                                    .show();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };

            TimeRequest timeRequest = new TimeRequest(account.getString("username", null),
                    account.getString("password", null),
                    responseListener,
                    account.getLong("time", 0),
                    "read");
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(timeRequest);
        }
    }

    public class TimeActionService extends IntentService {
        public boolean expend = false;
        public TimeActionService() {
            super(NotificationActionService.class.getSimpleName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            expend = !expend;
            if(expend){
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Example Service")
                        .setContentText("Timer Started")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .build();
                nm.notify(2,notification);
            }
            else{
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Example Service")
                        .setContentText("Timer Stopped")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .build();
                nm.notify(2,notification);
                updateServer();
            }
        }

        public void updateServer(){
            final SharedPreferences account = getSharedPreferences("ACCOUNT", MODE_PRIVATE);
            // Response received from the server
            Response.Listener<String> responseListener = new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");

                        if(!success) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(TimeActionService.this);
                            builder.setMessage("Sync with server failed")
                                    .setNegativeButton("Retry", null)
                                    .create()
                                    .show();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };

            TimeRequest timeRequest = new TimeRequest(account.getString("username", null),
                    account.getString("password", null),
                    responseListener,
                    account.getLong("time", 0),
                    "write");
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(timeRequest);
        }
    }
}