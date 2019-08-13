package com.example.serverapplock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AppList extends AppCompatActivity {

    final List<String> ListElementsArrayList = new ArrayList<String>();

    @Override
    public void onBackPressed() { //to prevent going back to login screen because already logged in at this point
        return;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        final SharedPreferences prefs = getSharedPreferences("BLOCKLIST", MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();

        final SharedPreferences account = getSharedPreferences("ACCOUNT", MODE_PRIVATE);
        final SharedPreferences.Editor aEditor = account.edit();

        ListView list = findViewById(R.id.ListView1);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(AppList.this, android.R.layout.simple_list_item_1, ListElementsArrayList){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = super.getView(position, convertView, parent);

                if(prefs.contains(getItem(position).substring(ListElementsArrayList.get(position).indexOf('(')+1, ListElementsArrayList.get(position).indexOf(')'))))
                    row.setBackgroundColor (Color.RED);
                else
                    row.setBackgroundColor (Color.WHITE);
                return row;
            }
        };
        list.setAdapter(adapter);

        // Get list of installed apps
        final PackageManager pm = getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> appList = pm.queryIntentActivities(mainIntent, 0);
        Collections.sort(appList, new ResolveInfo.DisplayNameComparator(pm));

        for (ResolveInfo temp : appList) {
            if(!temp.activityInfo.packageName.equals("com.example.serverapplock"))
                ListElementsArrayList.add(temp.activityInfo.applicationInfo.loadLabel(pm).toString() + " (" + temp.activityInfo.packageName + ")");
        }
        adapter.notifyDataSetChanged();

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(prefs.contains(getAppPackageName(position))){
                    editor.remove(getAppPackageName(position));
                    view.setBackgroundColor(Color.WHITE);
                }
                else {
                    editor.putString(getAppPackageName(position), "");
                    view.setBackgroundColor(Color.RED);
                }
                editor.apply();
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aEditor.remove("username");
                aEditor.remove("password");
                aEditor.apply();
                Intent serviceIntent = new Intent(AppList.this, ExampleService.class);
                stopService(serviceIntent);
                finish();
            }
        });
    }

    private String getAppPackageName(int position){
        return ListElementsArrayList.get(position).substring(ListElementsArrayList.get(position).indexOf('(')+1, ListElementsArrayList.get(position).indexOf(')'));
    }
}
