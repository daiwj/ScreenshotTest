package com.example.screenshothooktest;

import android.content.Context;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;

/**
 * @author dwj  2017/11/10 16:31
 * @deprecated
 */
public class ScreenshotMonitor {

    public static final String TAG = ScreenshotMonitor.class.getSimpleName();
    public static final String EXTRA_FILE_PATH = "screenshot_file_path";

    private final Handler H = new Handler();

    private final ArrayList<ScreenshotObserver> observerList = new ArrayList<>(); // sdcard 截屏目录直接监听

    private ScreenshotMonitor(Context context) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                final String ROOT = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
                observerList.add(new ScreenshotObserver(ROOT + Environment.DIRECTORY_PICTURES + "/Screenshots"));
                observerList.add(new ScreenshotObserver(ROOT + Environment.DIRECTORY_PICTURES + "/screenshots"));
                observerList.add(new ScreenshotObserver(ROOT + Environment.DIRECTORY_DCIM + "/Screenshots")); // 三星 s8
                observerList.add(new ScreenshotObserver(ROOT + Environment.DIRECTORY_DCIM + "/screenshots"));
                observerList.add(new ScreenshotObserver(ROOT + new String("截屏".getBytes(), "UTF-8"))); // VIVO x9;
            } catch (Exception e) {

            }
        }
    }

    private static ScreenshotMonitor sMonitor;
    private Watcher mWatcher;

    public static ScreenshotMonitor get(Context context) {
        if (sMonitor == null) {
            synchronized (ScreenshotMonitor.class) {
                if (sMonitor == null) {
                    sMonitor = new ScreenshotMonitor(context);
                }
            }
        }
        return sMonitor;
    }

    public void register(Watcher watcher) {
        mWatcher = watcher;
        Log.i(TAG, "register watcher: " + watcher.getClass().getSimpleName());
    }

    public void startWatching() {
        for (ScreenshotObserver observer : observerList) {
            observer.startWatching();
        }
    }

    private void stopWatching() {
        for (ScreenshotObserver observer : observerList) {
            observer.stopWatching();
        }
    }

    /**
     * 释放强引用， 避免内存泄露
     */
    public void free() {
        stopWatching();

        H.removeCallbacksAndMessages(null);

        mWatcher = null;
        sMonitor = null;
    }

    /**
     * SDCard文件直接监听
     */
    private final class ScreenshotObserver extends FileObserver {

        private String dir;

        public ScreenshotObserver(String path) {
            super(path, CLOSE_WRITE);

            // 部分设备需要监听CLOSE_NOWRITE才能监听到截屏，但是这个event会引起OOM，好纠结
//            super(path, CLOSE_WRITE | CLOSE_NOWRITE);

            dir = path;
            Log.i(TAG, "Observer on: " + dir);
        }

        @Override
        public void onEvent(final int event, @Nullable final String path) {
            final String imagePath = dir + "/" + path;
            if (event == CLOSE_WRITE || event == CLOSE_NOWRITE) {
                Log.i(TAG, "ScreenshotMonitor CREATE: " + imagePath);
                onWatch(imagePath);
            } else if (event == DELETE) {
                Log.i(TAG, "ScreenshotMonitor DELETE: " + imagePath);
                onWatch(imagePath);
            }
        }

        /**
         * run in ui-thread
         *
         * @param path
         */
        private void onWatch(@Nullable final String path) {
            H.post(new Runnable() {
                @Override
                public void run() {
                    if (mWatcher != null) {
                        mWatcher.onWatch(path);
                    } else {
                        Log.i(TAG, "ScreenshotMonitor Watcher is null");
                    }
                }
            });
        }
    }
}
