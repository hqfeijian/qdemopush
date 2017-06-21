package com.bihu.qdemopush.push;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bihu.qdemopush.utils.AppUtils;
import com.bihu.qdemopush.utils.ToastUtil;

/**
 * Created by huqiang2 on 2017/5/27.
 *
 */

public class QpushService extends Service {

    final String SOCKET_HOST = "214.socket.behuh.com";
    final int SOCKET_PORT = 9988;
    //数据推送，不显示到通知栏
    final static int PUSH_TYPE_DATA = 1;
    final static int PUSH_TYPE_PROTO_DATA = 2;
    Handler mHandler;
    String TAG = "QpushService";
    QpushClient mClient;

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        initHandler();
        mClient = QpushClient.getInstance(mHandler);
        mClient.init(SOCKET_HOST, SOCKET_PORT, AppUtils.getPesudoUniqueID());
        super.onCreate();
    }

    /**
     * 初始化handler，用于接收推送的消息
     */
    private void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case PUSH_TYPE_DATA:
                        Log.e(TAG, "PUSH_TYPE_DATA");
                        String data = (String) msg.obj;
                        ToastUtil.showShort(QpushService.this.getApplicationContext(), data);
                    case PUSH_TYPE_PROTO_DATA:
                        Driver.ClientMessage clientMessage = (Driver.ClientMessage) msg.obj;
                        Log.e(TAG, "PUSH_TYPE_PROTO_DATA");
                        Intent intentCancelRighr = new Intent();
                        intentCancelRighr.setAction(PushReceiverAction.PUSH_ACTION);
                        intentCancelRighr.putExtra("data", clientMessage);
                        getApplicationContext().sendBroadcast(intentCancelRighr);
                        break;
                }
            }
        };
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        mClient.onDestory();
    }
}
