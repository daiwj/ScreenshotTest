package com.example.screenshothooktest;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * @author dwj 2016年5月4日上午9:08:12
 *         <p>
 *         抽象悬浮窗<br>
 *         需要根据不同的数据源，并复写相应方法定制不同的事件处理方案。</p>
 */
public abstract class DragFloatView<Data> implements View.OnTouchListener {

    public static final String TAG = DragFloatView.class.getSimpleName();

    private Activity mActivity;

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParam;
    private DisplayMetrics dm;

    private View mContentView;

    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    private int mWidth = 0, mHeight = 0;
    private int mTouchSlop = 0;
    private int mDefaultLocationY = 0;
    private int mBottomLimitHeight = 0;
    private long mKeepSideTimeMillis = 0;

    private int mClickX = 0;
    private int mClickY = 0;
    private boolean mIsMoved = false;

    private static final int DIRECTION_LEFT = 1;
    private static final int DIRECTION_RIGHT = 2;

    private boolean isKeepSide = false;
    private boolean isDraggable = true;

    private ValueAnimator mKeepSideAnimator;

    private boolean isAdded = false;

    public DragFloatView(Activity activity) {
        mActivity = activity;

        dm = activity.getResources().getDisplayMetrics();
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;

        final int[] size = generateWindowSize();
        if (size == null) {
            throw new IllegalArgumentException("Override method generateWindowSize and return the window size first!");
        }
        mWidth = size[0];
        mHeight = size[1];

        mDefaultLocationY = getDefaultLocationY();
        mKeepSideTimeMillis = getKeepSideTimeMillis();
        mBottomLimitHeight = getBottomLimitHeight();
        isKeepSide = isKeepSide();
        isDraggable = isDraggable();

        mTouchSlop = dp2px(4);

        mContentView = onCreateView();
        if (mContentView == null) {
            throw new IllegalArgumentException("No content view was found!");
        }

        mContentView.setOnTouchListener(this);

    }

    protected abstract View onCreateView();

    public abstract void applyData(Data data);

    protected abstract int[] generateWindowSize();

    public View getContentView() {
        return mContentView;
    }

    public Activity getActivity() {
        return mActivity;
    }

    public int getDefaultLocationY() {
        return mScreenHeight / 2 - mHeight;
    }

    public int getBottomLimitHeight() {
        return mScreenHeight - mHeight * 2;
    }

    public long getKeepSideTimeMillis() {
        return 500l;
    }

    public boolean isKeepSide() {
        return false;
    }

    public void setKeepSide(boolean isKeepSide) {
        this.isKeepSide = isKeepSide;
    }

    public void setDraggable(boolean isDraggable) {
        this.isDraggable = isDraggable;
    }

    public boolean isDraggable() {
        return true;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getScreenWidth() {
        return mScreenWidth;
    }

    public int getScreenHeight() {
        return mScreenHeight;
    }

    protected WindowManager.LayoutParams generateWindowLayoutParam() {
        return getDefaultWindowLayoutParam();
    }

    private WindowManager.LayoutParams getDefaultWindowLayoutParam() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.width = mWidth;
        lp.height = mHeight;
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION;
        lp.format = PixelFormat.RGBA_8888;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.gravity = Gravity.LEFT | Gravity.TOP;
        lp.x = mScreenHeight - mWidth;
        lp.y = mDefaultLocationY;
        return lp;
    }

    public void create() {
        if (!isAdded) {
            Log.w(TAG, "create: " + getClass().getSimpleName());
            WindowManager wm = mActivity.getWindowManager();
            WindowManager.LayoutParams lp = generateWindowLayoutParam();
            if (lp == null) {
                lp = getDefaultWindowLayoutParam();
            }
            mWindowManager = wm;
            mLayoutParam = lp;
            mWindowManager.addView(mContentView, mLayoutParam);
            isAdded = true;
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (!isDraggable) {
            return false;
        }

        boolean handled = false;
        final int motionX = (int) event.getRawX();
        final int motionY = (int) event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mClickX = motionX;
                mClickY = motionY;
                if (mKeepSideAnimator != null && mKeepSideAnimator.isRunning()) {
                    mKeepSideAnimator.cancel();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mIsMoved) {
                    if (Math.abs(motionX - mClickX) >= mTouchSlop || Math.abs(motionY - mClickY) >= mTouchSlop) {
                        mIsMoved = true;
                    }
                }
                if (mIsMoved) {
                    mLayoutParam.x = motionX - mWidth * 3 / 4;
                    mLayoutParam.y = motionY - mHeight * 3 / 4;
                    mWindowManager.updateViewLayout(mContentView, mLayoutParam);
                }
                handled = true;
                break;
            case MotionEvent.ACTION_UP:
                if (mIsMoved) {
                    if (isKeepSide) {
                        keepSideIfNeed(mLayoutParam.x, mLayoutParam.y);
                    }
                    mIsMoved = false;
                    handled = true;
                }
        }
        return handled;
    }

    private void keepSideIfNeed(final int currentX, final int currentY) {
        final int direction = currentX < mScreenWidth / 2 ? DIRECTION_LEFT : DIRECTION_RIGHT;
        final int endX = direction == DIRECTION_LEFT ? 0 : mScreenWidth - mWidth;
        final boolean isOutOfBounds = currentY > mBottomLimitHeight;
        final int endY = isOutOfBounds ? mDefaultLocationY : currentY;

        mKeepSideAnimator = ValueAnimator.ofFloat(0f, 1f);
        mKeepSideAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float offsetValue = ((Float) animation.getAnimatedValue()).floatValue();
                mLayoutParam.x = (int) (currentX + (endX - currentX) * offsetValue);
                mLayoutParam.y = (int) (currentY + (endY - currentY) * offsetValue);
                mWindowManager.updateViewLayout(mContentView, mLayoutParam);
            }
        });
        mKeepSideAnimator.setDuration(mKeepSideTimeMillis);
        mKeepSideAnimator.start();
    }

    public <T extends View> T findViewById(int id) {
        return (T) mContentView.findViewById(id);
    }

    public void setOnClickListener(View.OnClickListener listener) {
        mContentView.setOnClickListener(listener);
    }

    public void toggle(boolean toggle) {
        mContentView.setVisibility(toggle ? View.VISIBLE : View.INVISIBLE);
    }

    public void destroy() {
        if (isAdded) {
            mWindowManager.removeView(mContentView);
            mWindowManager = null;
            mLayoutParam = null;
            mActivity = null;
            isAdded = false;
        }
    }

    public boolean isAdded() {
        return isAdded;
    }

    public int dp2px(float dp) {
        return (int) (dm.density * dp + 0.5f);
    }
}
