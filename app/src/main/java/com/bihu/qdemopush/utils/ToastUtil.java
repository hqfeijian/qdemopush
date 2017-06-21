package com.bihu.qdemopush.utils;

import android.content.Context;
import android.widget.Toast;


/**
 *
 * Created by hoobo_Q on 2016/5/11.
 */
public class ToastUtil {
    /**
     * toast
     * @param context     最好是:getApplicationContext（）
     */
    public static void showShort(Context context, String toastString) {
        try {
            Toast.makeText(context, toastString, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 为了不导致内存泄露，使用该方法的时候请使用Application
     */
    public static void showLong(Context context, String toastString) {
        try {
            Toast.makeText(context, toastString, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
