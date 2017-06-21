package com.bihu.qdemopush.push;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.bihu.qdemopush.MainActivity;
import com.bihu.qdemopush.R;
import com.bihu.qdemopush.utils.ToastUtil;

/**
 * Created by huqiang2 on 2017/6/8.
 *
 * @author hq
 */

public class PushReceiver extends BroadcastReceiver {
    Context mContext;
    String TAG = "PushReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        if (intent.getAction().equals(PushReceiverAction.PUSH_ACTION)) {
            //推送的通知消息
            Driver.ClientMessage clientMessage = (Driver.ClientMessage) intent.getSerializableExtra("data");
            if (clientMessage != null) {
                switch (clientMessage.getType()) {
                    case 1: //socket登录，不做处理
                        Log.e(TAG, "socket登录消息,sid=" + clientMessage.getHeartBeat().getSid());
                        //// TODO: 2017/6/21
                        break;
                    case 2://通知告知被迫取消领取红包资格
                        Log.e(TAG, "取消抢红包资格,title=" + clientMessage.getLogOut().getTitle() + ";message=" + clientMessage.getLogOut().getContent());
                        ToastUtil.showShort(mContext, "被迫取消抢红包资格");
                        Intent intentCancelRighr = new Intent();
                        intentCancelRighr.setAction(PushReceiverAction.CANCEL_REDPACKET_RIGHT);
                        intentCancelRighr.putExtra("data", clientMessage);
                        context.sendBroadcast(intentCancelRighr);
                        showNotification(clientMessage.getLogOut().getTitle(), clientMessage.getLogOut().getContent(), 1);
                        break;
                    case 3: //领取红包/未领到红包消息
                        Intent intentGo = new Intent();
                        intentGo.setAction(clientMessage.getRedPacket().getResult() == 1 ? PushReceiverAction.GET_REDPACKET_ACTION : PushReceiverAction.DONT_GET_REDPACKET_ACTION);
                        intentGo.putExtra("data", clientMessage);
                        context.sendBroadcast(intentGo);
                        Log.e(TAG, "红包消息，content=" + clientMessage.getRedPacket().getContent());
                        showNotification(clientMessage.getRedPacket().getTitle(), clientMessage.getRedPacket().getContent(), 2);
                        break;
                }
            } else {
                Log.e(TAG, "clientMessage is null");
            }
        }
    }

    /**
     * 在状态栏显示通知
     */
    @SuppressWarnings("deprecation")
    private void showNotification(String title, String content, int type) {
        // 创建一个NotificationManager的引用
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context
                .NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        builder.setContentTitle(title)
                .setContentText(content)
                .setTicker(title)
//                .setContentIntent(getDefalutIntent(Notification.FLAG_AUTO_CANCEL))//点击通知栏设置意图
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_HIGH)
//                .setAutoCancel(true)
                .setOngoing(false)//ture，设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)
                .setSmallIcon(R.mipmap.ic_launcher);
        Notification notification = builder.build();
        notification.sound = Uri.parse("android.resource://" + mContext.getPackageName() + "/" + R.raw.diaoluo_da);
        Intent notificationIntent;
        notificationIntent = new Intent(mContext, MainActivity.class);
        // 点击该通知后要跳转的Activity
        PendingIntent contentItent = PendingIntent.getActivity(mContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.contentIntent = contentItent;
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        // 把Notification传递给NotificationManager
        notificationManager.notify(type, notification);
    }
}
