package com.bihu.qdemopush;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.bihu.qdemopush.push.QpushService;

/**
 * Created by huqiang2 on 2017/6/21.
 */

public class AppApplication extends Application {
    String TAG="AppApplication";
    @Override
    public void onCreate() {
        super.onCreate();
        initPush();
    }
    /**
     * 初始化推送服务
     */
    private void initPush(){
        Log.e(TAG,"initPush");
        Intent intentService = new Intent();
        intentService.setClass(this, QpushService.class);
        this.startService(intentService);


    }
}
