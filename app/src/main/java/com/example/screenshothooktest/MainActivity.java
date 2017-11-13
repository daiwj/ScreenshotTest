package com.example.screenshothooktest;

import android.app.Activity;
import android.os.Bundle;
import android.os.FileObserver;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity implements ScreenshotMonitor.Watcher {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ScreenshotMonitor.getInstance(this).register(this);
    }

    @Override
    public void onWatch(int event, String path) {
        if (event == FileObserver.CLOSE_WRITE) {
            final DragFloatView view = new ScreenshotFloatView(this);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "click drag float view", Toast.LENGTH_SHORT).show();
                    view.destroy();
                }
            });
            view.applyData(path);
            view.create();
        }
    }
}
