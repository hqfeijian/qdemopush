package com.bihu.qdemopush.push;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by huqiang2 on 2017/5/27.
 *
 * @author huq
 */
public class QpushClient implements Runnable {
    protected static QpushClient mInstance;
    protected Handler mHandler;
    protected InetSocketAddress mAddress;
    String mIMEI;
    protected String TAG = "QpushClient";
    //socket连接的超时时间
    private final int CONNECT_TIME_OUT = 5 * 1000;
    //巡检周期
    private final int CHECK_PERIOD = 2 * 1000;
    //连接尝试间隔时间
    private final int CONNECT_PERIOD = 30 * 1000;
    private final int HEARTBEART_PERIOD = 5 * 1000;
    //若连接失败或响应失败，则尝试次数为9，若仍无效，则不再尝试
    private final int CONNECT_TRY_TIMES = 9;

    private final int SEND_MSG_TYPE_HEARTBEAT = 1; //心跳包
    private final int SEND_MSG_TYPE_SOCKET_LOGIN = 2; //发送socket登录包
    //连接尝试次数
    private int mConnectCount;

    Socket mClientSocket;
    String mHost;
    int mPort;
    //设置是否去读取数据
    boolean isStartRecieveMsg = false;
    //开启心跳检测
    boolean isKeepHeartBeat = false;

    BufferedReader mReader;
    ScheduledExecutorService executor;//定位定时器
    HeartBeatTask mHeartBeatTask;
    Thread mThread;

    private QpushClient(Handler handler) {
        mHandler = handler;
    }

    public static QpushClient getInstance(Handler handler) {
        if (mInstance == null) {
            mInstance = new QpushClient(handler);
        }
        return mInstance;
    }

    public void init(String host, int port,String imei) {
        mHost = host;
        mPort = port;
        mIMEI = imei;
        mThread = new Thread(this);
        mThread.start();
        isStartRecieveMsg = true;
        isKeepHeartBeat = true;
    }

    @Override
    public void run() {
        mAddress = new InetSocketAddress(getIP(mHost), mPort);

        //尝试连接，若未连接，则设置尝试次数
        while (mConnectCount < CONNECT_TRY_TIMES) {
            connect();
            if (!mClientSocket.isConnected()) {
                mConnectCount++;
                sleep(CONNECT_PERIOD);
            } else {
                mConnectCount = 0;//连接上，则恢复置0
                break;
            }
        }
        if (mClientSocket.isConnected()) {
            keepHeartBeat();
            recvProtobufMsg();
//            recvStringMsg();
        }
    }

    private void connect() {
        try {
            if (mClientSocket == null) {
                mClientSocket = new Socket();
            }
            mClientSocket.connect(mAddress, CONNECT_TIME_OUT);
            Driver.HeartBeat heartbeat = Driver.HeartBeat.newBuilder().setImei(mIMEI).build();
            Driver.ClientMessage socketLogin = Driver.ClientMessage.newBuilder().setType(1).setHeartBeat(heartbeat).build();
            sendMsg(socketLogin,SEND_MSG_TYPE_SOCKET_LOGIN);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "连接失败 mClientSocket.connect fail ,ip=" + mAddress.getHostName() + ";port=" + mAddress.getPort() + ";detail:" + e.getMessage());
        }
    }

    /**
     * 心跳维护
     */
    private void keepHeartBeat() {
        //设置心跳频率，启动心跳
        if (isKeepHeartBeat) {
            if (mHeartBeatTask == null) {
                mHeartBeatTask = new HeartBeatTask();
            }
            try {
                if (executor != null) {
                    executor.shutdownNow();
                    executor = null;
                }
                executor = Executors.newScheduledThreadPool(1);
                executor.scheduleAtFixedRate(
                        mHeartBeatTask,
                        1000,  //initDelay
                        HEARTBEART_PERIOD,  //period
                        TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * @param message
     * @param type    1=login；2=心跳；
     */
    public void sendMsg(String message, int type) {
        PrintWriter writer;
        try {
            writer = new PrintWriter(new OutputStreamWriter(mClientSocket.getOutputStream(), "UTF-8"), true);
            writer.println(message);
            Log.e(TAG, "sendMsg  Socket.isClosed()=" + mClientSocket.isClosed() + ";connect=" + mClientSocket.isConnected());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            switch (type) {
                case SEND_MSG_TYPE_HEARTBEAT:
                    mHandler.obtainMessage(QpushService.PUSH_TYPE_PROTO_DATA, "发送心跳异常").sendToTarget();
                    break;
            }

        }
    }

    /**
     * @param message
     * @param type    1=login；2=心跳；
     */
    public void sendMsg(Driver.ClientMessage message, int type) {
        try {
            message.writeTo(mClientSocket.getOutputStream());
            Log.e(TAG, "sendMsg success");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            if (type == SEND_MSG_TYPE_HEARTBEAT) {
                //心跳失败
                Log.e(TAG, "心跳失败");
                if (mClientSocket.isClosed()) {
                    connect();
                }
            } else {
                Log.e(TAG, "发送数据失败");
            }
            e.printStackTrace();
        }
    }

    /**
     * 不断的检测是否有服务器推送的数据过来
     */
    public void recvStringMsg() {
        while (mClientSocket != null && mClientSocket.isConnected() && !mClientSocket.isClosed()) {
            try {
                mReader = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream(), "UTF-8"));
                String data = mReader.readLine();
                Log.e(TAG, "recvStringMsg data=" + data);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            sleep(2000);
        }
        sleep(CHECK_PERIOD);
    }
    /**
     * 不断的检测是否有服务器推送的数据过来
     */
    public void recvProtobufMsg() {
        while (isStartRecieveMsg) {
            try {
                byte[] resultByte = recvByteMsg(mClientSocket.getInputStream());
                if (resultByte != null) {
                    Driver.ClientMessage retMsg = Driver.ClientMessage.parseFrom(resultByte);
                    mHandler.obtainMessage(QpushService.PUSH_TYPE_PROTO_DATA, retMsg).sendToTarget();
                } else {
                    Log.e(TAG, "resultByte is null");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            sleep(5 * 1000);
        }
    }
    /**
     * 接收server的信息
     *
     * @return
     */
    public byte[] recvByteMsg(InputStream inpustream) {
        try {
            byte len[] = new byte[1024];
            int count = inpustream.read(len);
            byte[] temp = new byte[count];
            for (int i = 0; i < count; i++) {
                temp[i] = len[i];
            }
            return temp;
        } catch (Exception localException) {
            localException.printStackTrace();
        }
        return null;
    }

    class HeartBeatTask implements Runnable {
        @Override
        public void run() {
            //执行发送心跳
            try {
                mClientSocket.sendUrgentData(65);
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    Log.e(TAG, "socket心跳异常，尝试断开，重连");
                    mClientSocket.close();
                    mClientSocket = null;
                    //然后尝试重连
                    connect();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            Log.e(TAG, "发送心跳，Socket.isClosed()=" + mClientSocket.isClosed() + ";connect" + mClientSocket.isConnected());
        }
    }

    /**
     * 通过域名获取IP
     *
     * @param domain
     * @return
     */
    public String getIP(String domain) {
        String IPAddress = "";
        InetAddress ReturnStr1 = null;
        try {
            ReturnStr1 = InetAddress.getByName(domain);
            IPAddress = ReturnStr1.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e(TAG, "获取IP失败" + e.getMessage());
        }
        return IPAddress;
//        return "192.168.3.121";
    }

    private void sleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 销毁socket
     */
    public void onDestory() {
        if (mClientSocket != null) {
            try {
                isStartRecieveMsg = false;
                mClientSocket.close();
                if (executor != null) {
                    executor.shutdownNow();
                    executor = null;
                }
                if(mThread != null){
                    mThread.interrupt();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }catch (Exception ex){
                ex.printStackTrace();
            }
            mClientSocket = null;
        }
    }

    /*
     * Ready for use.
     */
    public void close() {
        try {
            if (mClientSocket != null && !mClientSocket.isClosed())
                mClientSocket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
