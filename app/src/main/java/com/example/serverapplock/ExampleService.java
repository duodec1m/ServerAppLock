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
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.example.serverapplock.App.CHANNEL_ID;


public class ExampleService extends Service {

    private int time = 60000;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent action1Intent = new Intent(this, NotificationActionService.class);
        PendingIntent action1PendingIntent = PendingIntent.getService(this, 0, action1Intent, 0);

        Intent timeIntent = new Intent(this, TimeActionService.class);
        PendingIntent timePendingIntent = PendingIntent.getService(this, 0, timeIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Example Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .addAction(0, "Sync timer with server", action1PendingIntent)
                .addAction(0, "Expend time", timePendingIntent)
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
        final SharedPreferences prefs = getSharedPreferences("BLOCKLIST", MODE_PRIVATE);
        Intent startBlockScreen = new Intent(this, BlockScreen.class);
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        while (true) {
            if(time < 0){
                time = 0;
                TimeActionService.expend = false;
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Example Service")
                        .setContentText("Time's Up")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .build();
                nm.notify(2,notification);
            }
            else if(time > 29000 && time <= 30000){
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Example Service")
                        .setContentText(" About 30000 ms left")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .build();
                nm.notify(2,notification);
            }

            if (getTaskTopAppPackageName(this) != null && prefs.contains(getTaskTopAppPackageName(this))) {
                startActivity(startBlockScreen);
            }

            try {
                Thread.sleep(1000);
                if(TimeActionService.expend)
                    time=time-1000;
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
            //implement server sync here
        }
    }

    public static class TimeActionService extends IntentService {
        public static boolean expend = false;
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
            }
        }
    }
}