package com.example.screenshothooktest;

import android.app.Application;

/**
 * @author dwj  2017/11/10 17:17
 */

public class OurApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ScreenshotMonitor.getInstance(this).startWatching();
    }
}
