package com.example.screenshothooktest;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity implements Watcher {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ScreenshotMonitorV2.get(this).register(this);
    }

    @Override
    public void onWatch(String path) {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ScreenshotMonitorV2.get(this).free();
    }
}
