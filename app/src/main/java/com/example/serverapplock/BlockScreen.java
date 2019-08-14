package com.example.serverapplock;

import android.app.Activity;
import android.os.Bundle;

public class BlockScreen extends Activity {

    @Override
    public void onBackPressed() { //to prevent going back to login screen because already logged in at this point
        return;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_screen);
        this.setFinishOnTouchOutside(false);
    }
}
