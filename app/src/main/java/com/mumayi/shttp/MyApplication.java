package com.mumayi.shttp;

import android.app.Application;

/**
 * Created by Administrator on 2018/3/16.
 */

public class MyApplication extends Application {
    public static MyApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }
    public static MyApplication getInstance() {
        return mInstance;
    }
}
