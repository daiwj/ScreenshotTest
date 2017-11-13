package com.example.screenshothooktest;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;

/**
 * @author dwj  2017/11/10 16:31
 */

public class ScreenshotMonitor {

    public static final String TAG = ScreenshotMonitor.class.getSimpleName();
    public static final String EXTRA_FILE_PATH = "screenshot_file_path";

    private Context mContext;

    private final Handler H = new Handler();

    private final ArrayList<ScreenshotObserver> observerList = new ArrayList<>(); // sdcard 截屏目录直接监听
    private ContentObserver mInternalObserver, mExternalObserver; // 媒体文件全局监听

    {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            final String ROOT = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
            observerList.add(new ScreenshotObserver(ROOT + Environment.DIRECTORY_PICTURES + "/Screenshots"));
            observerList.add(new ScreenshotObserver(ROOT + Environment.DIRECTORY_PICTURES + "/screenshots"));
            observerList.add(new ScreenshotObserver(ROOT + Environment.DIRECTORY_DCIM + "/Screenshots")); // 三星 s8
            observerList.add(new ScreenshotObserver(ROOT + Environment.DIRECTORY_DCIM + "/screenshots"));
            try {
                observerList.add(new ScreenshotObserver(ROOT + new String("截屏".getBytes(), "UTF-8"))); // VIVO x9;
            } catch (Exception e) {

            }

            mInternalObserver = new MediaContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, H);
            mExternalObserver = new MediaContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, H);
        }
    }


    private BitmapFactory.Options OPTIONS = new BitmapFactory.Options();

    {
        OPTIONS.inJustDecodeBounds = true;
    }

    public interface Watcher {
        /**
         * @param event {@link FileObserver#ALL_EVENTS}
         * @param path  file path
         */
        void onWatch(int event, String path);
    }

    private ScreenshotMonitor(Context context) {
        mContext = context;
    }

    private static ScreenshotMonitor sMonitor;
    private Watcher mWatcher;

    public static ScreenshotMonitor getInstance(Context context) {
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

        if (mInternalObserver == null) {
            // 添加监听 内部储存
            mContext.getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                    false,
                    mInternalObserver);
        }
        if (mExternalObserver != null) {
            //外部储存
            mContext.getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    false,
                    mExternalObserver);
        }

    }

    public void stopWatching() {
        for (ScreenshotObserver observer : observerList) {
            observer.stopWatching();
        }
        if (mInternalObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mInternalObserver);
        }
        if (mExternalObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mExternalObserver);
        }

        mWatcher = null;
        sMonitor = null;
    }

    /**
     * SDCard文件直接监听
     */
    private final class ScreenshotObserver extends FileObserver {

        private String dir;

        public ScreenshotObserver(String path) {
            super(path, ALL_EVENTS);
            dir = path;
            Log.i(TAG, "Observer on: " + dir);
        }

        @Override
        public void onEvent(final int event, @Nullable final String path) {
            if (event == CLOSE_WRITE) {
                Log.i(TAG, "ScreenshotMonitor CREATE: " + dir + "/" + path);
                onWatch(FileObserver.CLOSE_WRITE, path);
            } else if (event == DELETE) {
                Log.i(TAG, "ScreenshotMonitor DELETE: " + dir + "/" + path);
                onWatch(FileObserver.DELETE, path);
            }
        }

        private void onWatch(final int event, @Nullable final String path) {
            H.post(new Runnable() {
                @Override
                public void run() {
                    if (mWatcher != null) {
                        mWatcher.onWatch(event, dir + "/" + path);
                    }
                }
            });
        }
    }

    /**
     * 媒体内容观察者(观察媒体数据库的改变)
     */
    private final class MediaContentObserver extends ContentObserver {

        private final String[] MEDIA_PROJECTIONS = {
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
        };

        private Uri mContentUri;

        public MediaContentObserver(Uri contentUri, Handler handler) {
            super(handler);
            mContentUri = contentUri;
        }

        @Override
        public void onChange(boolean selfChange) {
            handleMediaContentChange(mContentUri);
        }

        private void handleMediaContentChange(Uri contentUri) {
            Cursor cursor = null;
            try {
                // 数据改变时查询数据库中最后加入的一条数据
                cursor = mContext.getContentResolver().query(
                        contentUri,
                        MEDIA_PROJECTIONS,
                        null,
                        null,
                        MediaStore.Images.ImageColumns.DATE_ADDED + " desc limit 1"
                );

                if (cursor == null) {
                    return;
                }
                if (!cursor.moveToFirst()) {
                    return;
                }

                // 获取各列的索引
                int dataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);

                // 获取行数据
                final String data = cursor.getString(dataIndex);

                Bitmap bitmap = BitmapFactory.decodeFile(data, OPTIONS);
                if (bitmap != null) {
                    Log.i(TAG, "ContentObserver: CREATE" + data);
                    H.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mWatcher != null) {
                                mWatcher.onWatch(FileObserver.CLOSE_WRITE, data);
                            }
                        }
                    });
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
}
