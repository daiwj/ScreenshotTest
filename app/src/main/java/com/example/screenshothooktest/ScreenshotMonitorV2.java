package com.example.screenshothooktest;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dwj  2017/11/10 16:31
 */

public class ScreenshotMonitorV2 {

    public static final String TAG = ScreenshotMonitorV2.class.getSimpleName();
    public static final String EXTRA_FILE_PATH = "screenshot_file_path";

    private final Handler H = new Handler();

    private static ScreenshotMonitorV2 sMonitor;
    private ContentResolver mContentResolver;
    private ContentObserver mExternalObserver;
    private Watcher mWatcher;

    private final List<String> FILTERS = new ArrayList<>();

    {
        try {
            FILTERS.add("screenshots");
            FILTERS.add(new String("截屏".getBytes(), "UTF-8"));
        } catch (Exception e) {
        }
    }

    private final Uri EXTERNAL_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    private ScreenshotMonitorV2(Context context) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

            mContentResolver = context.getContentResolver();

            HandlerThread thread = new HandlerThread("ScreenshotObserver");
            thread.start();
            Handler handler = new Handler(thread.getLooper());
            mExternalObserver = new MediaContentObserver(handler);

        }
    }

    public static ScreenshotMonitorV2 get(Context context) {
        if (sMonitor == null) {
            synchronized (ScreenshotMonitorV2.class) {
                if (sMonitor == null) {
                    sMonitor = new ScreenshotMonitorV2(context);
                }
            }
        }
        return sMonitor;
    }

    public void startWatching() {
        if (mContentResolver != null && mExternalObserver != null) {
            Log.i(TAG, "start watching on: " + EXTERNAL_CONTENT_URI.getPath());
            mContentResolver.registerContentObserver(EXTERNAL_CONTENT_URI, true, mExternalObserver);
        }
    }

    private void stopWatching() {
        if (mContentResolver != null && mExternalObserver != null) {
            mContentResolver.unregisterContentObserver(mExternalObserver);
        }
    }

    /**
     * 释放强引用， 避免内存泄露
     */
    public void free() {
        stopWatching();

        H.removeCallbacksAndMessages(null);

        mContentResolver = null;
        mExternalObserver = null;
        mWatcher = null;
        sMonitor = null;
    }

    public void register(Watcher watcher) {
        mWatcher = watcher;
        Log.i(TAG, "register watcher: " + watcher.getClass().getSimpleName());
    }

    /**
     * 媒体内容观察者(观察媒体数据库的改变)
     */
    private final class MediaContentObserver extends ContentObserver {

        private final String[] PROJECTION = {
                MediaStore.Images.Media.DATA, // image path
                MediaStore.Images.Media.DATE_ADDED // image create time
        };

        private final String ORDER = MediaStore.Images.ImageColumns.DATE_ADDED + " desc limit 1";

        public MediaContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.i(TAG, "ContentObserver onChange nothing (sdk < 16)");
            handleContentChanged(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (isSingleImageFile(uri)) {
                Log.i(TAG, "ContentObserver onChange " + uri.getPath());
                handleContentChanged(uri);
            }
        }

        private boolean isSingleImageFile(Uri uri) {
            //return uri != null && uri.toString().matches(EXTERNAL_CONTENT_URI.toString() + "/[0-9]+");//在小米有的手机上测试不通过
            return uri != null && uri.toString().matches(EXTERNAL_CONTENT_URI.toString() + "/*|/[0-9]+");
        }

        private void handleContentChanged(Uri uri) {
            Cursor cursor = null;
            try {
                if (uri == null) { // sdk < 16
                    cursor = mContentResolver.query(EXTERNAL_CONTENT_URI, PROJECTION, null, null, ORDER);
                } else {
                    cursor = mContentResolver.query(uri, PROJECTION, null, null, ORDER);
                }
                if (cursor != null && cursor.moveToFirst()) {
                    final String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
                    Log.i(TAG, "ContentObserver imagePath: " + imagePath);
                    final long createTime = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_ADDED));
                    final long currentTime = System.currentTimeMillis() / 1000;
                    if (Math.abs(currentTime - createTime) <= 3) {
                        if (isScreenshotPath(imagePath)) {
                            H.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (mWatcher != null) {
                                        mWatcher.onWatch(imagePath);
                                    }
                                }
                            }, uri == null ? 500 : 0);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }
        }

        private boolean isScreenshotPath(String path) {
            boolean filtered = false;
            for (String filter : FILTERS) {
                filtered = path.toLowerCase().contains(filter);
                if (filtered) {
                    return true;
                }
            }
            return filtered;
        }
    }
}
