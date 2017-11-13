package com.example.screenshothooktest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * @author dwj  2017/11/10 14:20
 */

public class ScreenshotFloatView extends DragFloatView<String> {

    private ImageView imageView;

    public ScreenshotFloatView(Activity activity) {
        super(activity);
        setDraggable(true);
        setKeepSide(true);
    }

    @Override
    protected View onCreateView() {
        View contentView = LayoutInflater.from(getActivity()).inflate(R.layout.layout_float_view, null);
        imageView = contentView.findViewById(R.id.image);
        return contentView;
    }

    @Override
    public void applyData(String data) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        Bitmap bitmap = BitmapFactory.decodeFile(data);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }
    }

    @Override
    protected int[] generateWindowSize() {
        return new int[]{dp2px(81), dp2px(146)};
    }

    @Override
    protected WindowManager.LayoutParams generateWindowLayoutParam() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.width = getWidth();
        lp.height = getHeight();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION;
        lp.format = PixelFormat.RGBA_8888;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.gravity = Gravity.LEFT | Gravity.TOP;
        lp.x = getScreenWidth() - getWidth() - dp2px(10);
        lp.y = getScreenHeight() / 4;
        return lp;
    }
}
