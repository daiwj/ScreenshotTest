package com.example.screenshothooktest;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
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

    private Context mContext;

    private final Handler H = new Handler();

    private final List<String> FILTERS = new ArrayList<>();

    {
        try {
            FILTERS.add("screenshots");
            FILTERS.add(new String("截屏".getBytes(), "UTF-8"));
        } catch (Exception e) {
        }
    }

    private final String FILE_NAME_PREFIX = "screenshot";
    private final String PATH_SCREENSHOT = "screenshots/";

    private final Uri EXTERNAL_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    private ContentObserver mExternalObserver; // 媒体文件全局监听

    private ScreenshotMonitorV2(Context context) {
        mContext = context;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            HandlerThread thread = new HandlerThread("Screenshot_Observer");
            thread.start();
            Handler handler = new Handler(thread.getLooper());
            mExternalObserver = new MediaContentObserver(handler);
        }
    }

    private static ScreenshotMonitorV2 sMonitor;
    private Watcher mWatcher;

    public static ScreenshotMonitorV2 getInstance(Context context) {
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
        if (mExternalObserver != null) {
            Log.i(TAG, "start watching on: " + EXTERNAL_CONTENT_URI.getPath());
            mContext.getContentResolver().registerContentObserver(EXTERNAL_CONTENT_URI, true, mExternalObserver);
        }
    }

    public void stopWatching() {
        if (mExternalObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mExternalObserver);
        }

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
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA
        };

        private final String ORDER = MediaStore.Images.ImageColumns.DATE_ADDED + " desc limit 1";

        public MediaContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.i(TAG, "onChange " + uri.getPath());
            if (isSingleImageFile(uri)) {
                handleMediaContentChange(uri);
            }
        }

        private boolean isSingleImageFile(Uri uri) {
            return uri.toString().matches(EXTERNAL_CONTENT_URI.toString() + "/[0-9]+");
        }

        private void handleMediaContentChange(Uri contentUri) {
            Cursor cursor = null;
            try {
                cursor = mContext.getContentResolver().query(contentUri, PROJECTION, null, null, ORDER);
                if (cursor != null && cursor.moveToFirst()) {
                    final int dataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    final String imagePath = cursor.getString(dataIndex);
                    Log.i(TAG, "imagePath: " + imagePath);
                    if (isPathScreenshot(imagePath)) {
                        H.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mWatcher != null) {
                                    mWatcher.onWatch(FileObserver.CLOSE_WRITE, imagePath);
                                }
                            }
                        });
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
    }

    private boolean isFileScreenshot(String fileName) {
        return fileName.toLowerCase().startsWith(FILE_NAME_PREFIX);
    }

    private boolean isPathScreenshot(String path) {
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
