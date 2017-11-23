package com.example.screenshothooktest;

/**
 * @author dwj  2017/11/22 16:34
 */

public interface Watcher {
    /**
     * @param path  file path
     */
    void onWatch(String path);
}
