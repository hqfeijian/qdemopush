package com.bihu.qdemopush.push;

/**
 * Created by huqiang2 on 2017/6/7.
 */

public class PushReceiverAction {
    //领取到红包的推送广播
    public static final String GET_REDPACKET_ACTION = "GET_REDPACKET_ACTION";
    //未领取到红包的推送广播
    public static final String DONT_GET_REDPACKET_ACTION = "DONT_GET_REDPACKET_ACTION";
    // 被取消抢红包资格推送广播
    public static final String CANCEL_REDPACKET_RIGHT = "CANCEL_REDPACKET_RIGHT";

    public static final String PUSH_ACTION = "com.bihu.driver.driverclient.push";
}
