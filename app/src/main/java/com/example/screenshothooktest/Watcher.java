package com.example.screenshothooktest;

import android.os.FileObserver;

/**
 * @author dwj  2017/11/22 16:34
 */

public interface Watcher {
    /**
     * @param event {@link FileObserver#ALL_EVENTS}
     * @param path  file path
     */
    void onWatch(int event, String path);
}
