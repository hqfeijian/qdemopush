package com.bihu.qdemopush;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.bihu.qdemopush.push.QpushService;

public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG,"onDestroy");
        try {
            Intent serviceService = new Intent(this,QpushService.class);
            this.stopService(serviceService);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
