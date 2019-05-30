package me.khrystal.widget.marqueeview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Scroller;

import java.lang.ref.WeakReference;

/**
 * usage:
 * author: kHRYSTAL
 * create time: 19/5/27
 * update time:
 * email: 723526676@qq.com
 */
public class MarqueeView extends ViewGroup {

    public static final int ORIENTATION_UP = 1;
    public static final int ORIENTATION_DOWN = 2;
    public static final int ORIENTATION_LEFT = 3;
    public static final int ORIENTATION_RIGHT = 4;

    private int mOrientation;
    private int mItemCount;
    private int mCurrentPosition;
    private int mScrollDistance;
    private int mSwitchTime;
    private int mScrollTime;

    private Scroller mScroller;

    /**
     * 判断是否手动调用start()的标志位, 用于在onWindowFocusChanged辨识是否进行暂停恢复的操作｀
     */
    private boolean mIsStart;

    // 是否开启滚动时子View的缩放和透明度动画
    private boolean mEnableAlphaAnim;
    private boolean mEnableScaleAnim;

    private MarqueeViewAdapter mAdapter;

    private MarqueeObserver mMarqueeObserver = new MarqueeObserver();

    private boolean mVisible;


    public MarqueeView(Context context) {
        super(context);
        init(context, null);
    }

    public MarqueeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, null);
    }

    public MarqueeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, null);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MarqueeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MarqueeView);
            mSwitchTime = ta.getInt(R.styleable.MarqueeView_switchTime, 2500);
            mScrollTime = ta.getInt(R.styleable.MarqueeView_scrollTime, 1000);
            mOrientation = ta.getInt(R.styleable.MarqueeView_orientation, ORIENTATION_UP);
            mEnableAlphaAnim = ta.getBoolean(R.styleable.MarqueeView_enableAlphaAnim, false);
            mEnableScaleAnim = ta.getBoolean(R.styleable.MarqueeView_enableScaleAnim, false);
            ta.recycle();
        }
        mScroller = new Scroller(context, new AccelerateDecelerateInterpolator());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 声明临时变量存储父容器的期望值
        int parentDesireHeigth = 0;
        int parentDesireWidth = 0;

        int tmpWidth = 0;
        int tmpHeight = 0;

        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                final View child = getChildAt(i);
                // 获取子元素的布局参数
                final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                // 测量子元素并考虑外边距
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                // 计算父容器的期望值
                parentDesireWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                // 取子控件的最大宽度
                tmpWidth = Math.max(tmpWidth, parentDesireWidth);
                parentDesireHeigth = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
                // 取子控件的最大高度
                tmpHeight = Math.max(tmpHeight, parentDesireHeigth);
            }
            parentDesireWidth = tmpWidth;
            parentDesireHeigth = tmpHeight;
            // 考虑父容器内边距
            parentDesireWidth += getPaddingLeft() + getPaddingRight();
            parentDesireHeigth += getPaddingTop() + getPaddingBottom();
            // 尝试比较建议最小值和期望值的大小 并取大值
            parentDesireWidth = Math.max(parentDesireWidth, getSuggestedMinimumWidth());
            parentDesireHeigth = Math.max(parentDesireHeigth, getSuggestedMinimumHeight());
        }
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 设置最终测量值
        setMeasuredDimension(resolveSize(parentDesireWidth, widthMeasureSpec),
                resolveSize(parentDesireHeigth, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();

        if (mOrientation == ORIENTATION_DOWN || mOrientation == ORIENTATION_UP) {
            final int height = getMeasuredHeight();
            mScrollDistance = height;
            int multiHeight = 0;
            // 垂直方向跑马灯
            for (int i = 0; i < getChildCount(); i++) {
                // 遍历子元素并对其进行定位布局
                final View child = getChildAt(i);
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                if (i == 0 && multiHeight == 0 && mOrientation == ORIENTATION_DOWN) {
                    multiHeight = -height;
                    mCurrentPosition = 1;
                }
                // 垂直方向的话 因为布局高度定死为子控件最大的高度, 所以子控件一律位置垂直居中 paddingTop 和marginTop均失效
                child.layout(paddingLeft + lp.leftMargin,
                        (height - child.getMeasuredHeight()) / 2 + multiHeight,
                        child.getMeasuredWidth() + paddingLeft + lp.leftMargin,
                        (height - child.getMeasuredHeight()) / 2 + child.getMeasuredHeight() + multiHeight);
                multiHeight += height;
            }
        } else {
            final int width = getMeasuredWidth();
            mScrollDistance = width;
            int multiWidth = 0;
            // 水平方向跑马灯
            for (int i = 0; i < getChildCount(); i++) {
                final View child = getChildAt(i);
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

                if (i == 0 && multiWidth == 0 && mOrientation == ORIENTATION_RIGHT) {
                    multiWidth = -width;
                    mCurrentPosition = 1;
                }
                // 水平方向 因为布局宽度定死为子控件最大的宽度
                child.layout((width - child.getMeasuredWidth()) / 2 + multiWidth,
                        paddingTop + lp.topMargin,
                        (width - child.getMeasuredWidth()) / 2 + child.getMeasuredWidth() + multiWidth,
                        child.getMeasuredHeight() + paddingTop + lp.topMargin);
                multiWidth += width;
            }
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mVisible = visibility == VISIBLE;
        if (visibility == VISIBLE) {
            carryOn();
        } else {
            pause();
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        mVisible = visibility == VISIBLE;
        if (visibility == VISIBLE) {
            carryOn();
        } else {
            pause();
        }
    }

    @Override
    public void onScreenStateChanged(int screenState) {
        super.onScreenStateChanged(screenState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mVisible = screenState == View.SCREEN_STATE_ON;
        } else {
            mVisible = screenState == 1;
        }
        if (screenState == View.SCREEN_STATE_OFF) {
            pause();
        } else {
            carryOn();
        }
    }

    @Override
    public void computeScroll() {
        if (mItemCount == 0)
            return;
        if (mScroller.computeScrollOffset()) {
            if (mOrientation == ORIENTATION_DOWN || mOrientation == ORIENTATION_UP) {
                scrollTo(0, mScroller.getCurrY());
                handleScrollAnim();
            } else {
                scrollTo(mScroller.getCurrX(), 0);
                handleScrollAnim();
            }
            invalidate();
        } else if (!mScroller.computeScrollOffset()) {
            switch (mOrientation) {
                case ORIENTATION_UP:
                    if (mCurrentPosition >= mItemCount - 1) {
                        // 滚动到最后一个时 迅速回到第一个 造成轮播的家乡
                        fastScroll(-mCurrentPosition * mScrollDistance);
                        mCurrentPosition = 0;
                    }
                    break;
                case ORIENTATION_DOWN:
                    if (mCurrentPosition <= 0) {
                        fastScroll((mItemCount - 1) * mScrollDistance);
                        mCurrentPosition = mItemCount - 1;
                    }
                    break;
                case ORIENTATION_LEFT:
                    if (mCurrentPosition >= mItemCount - 1) {
                        fastScroll(-mCurrentPosition * mScrollDistance);
                        mCurrentPosition = 0;
                    }
                    break;
                case ORIENTATION_RIGHT:
                    if (mCurrentPosition <= 0) {
                        fastScroll((mItemCount - 1) * mScrollDistance);
                        mCurrentPosition = mItemCount - 1;
                    }
                    break;
            }
            invalidate();
        }
    }

    private void smoothScroll(int distance) {
        if (mOrientation == ORIENTATION_DOWN || mOrientation == ORIENTATION_UP) {
            mScroller.startScroll(0, mScroller.getFinalY(), 0, distance, mScrollTime);
        } else {
            mScroller.startScroll(mScroller.getFinalX(), 0, distance, 0, mScrollTime);
        }
    }

    private void fastScroll(int distance) {
        if (mOrientation == ORIENTATION_DOWN || mOrientation == ORIENTATION_UP) {
            mScroller.startScroll(0, mScroller.getFinalY(), 0, distance, 0);
        } else {
            mScroller.startScroll(mScroller.getFinalX(), 0, distance, 0);
        }
    }

    private void handleScrollAnim() {
        if (!mEnableAlphaAnim && !mEnableScaleAnim) {
            return;
        }

        float rate = 0;
        boolean notReachBorder = false;
        int relativeChildPosition = 0;
        switch (mOrientation) {
            case ORIENTATION_UP:
                rate = (mScroller.getCurrY() - mScroller.getStartY()) * 1.0f / (mScroller.getFinalY() - mScroller.getStartY()) / 2.0f + 0.5f;
                notReachBorder = mCurrentPosition != 0;
                relativeChildPosition = mCurrentPosition - 1;
                break;
            case ORIENTATION_DOWN:
                rate = (mScroller.getCurrY() - mScroller.getStartY()) * 1.0f / (mScroller.getFinalY() - mScroller.getStartY()) / 2.0f + 0.5f;
                notReachBorder = mCurrentPosition != mItemCount - 1;
                relativeChildPosition = mCurrentPosition + 1;
                break;
            case ORIENTATION_LEFT:
                rate = (mScroller.getCurrX() - mScroller.getStartX()) * 1.0f / (mScroller.getFinalX() - mScroller.getStartX()) / 2.0f + 0.5f;
                notReachBorder = mCurrentPosition != 0;
                relativeChildPosition = mCurrentPosition - 1;
                break;
            case ORIENTATION_RIGHT:
                rate = (mScroller.getCurrX() - mScroller.getStartX()) * 1.0f / (mScroller.getFinalX() - mScroller.getStartX()) / 2.0f + 0.5f;
                notReachBorder = mCurrentPosition != mItemCount - 1;
                relativeChildPosition = mCurrentPosition + 1;
                break;
        }

        if (notReachBorder) {
            playAnim(getChildAt(mCurrentPosition), mEnableAlphaAnim, mEnableScaleAnim, rate);
            playAnim(getChildAt(relativeChildPosition), mEnableScaleAnim, mEnableScaleAnim, 1.5f - rate);
        } else {
            playAnim(getChildAt(mCurrentPosition), mEnableAlphaAnim, mEnableScaleAnim, 1);
        }
    }

    private void playAnim(View view, boolean enableAlphaAnim, boolean enableScaleAnim, float rate) {
        if (view == null) {
            return;
        }
        if (enableAlphaAnim) {
            ViewCompat.setAlpha(view, rate);
        }
        if (enableScaleAnim) {
            ViewCompat.setScaleX(view, rate);
            ViewCompat.setScaleY(view, rate);
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }

    public void start() {
        if (getChildCount() <= 1 || mHandler != null) {
            return;
        }
        mIsStart = true;
        mHandler = new MarqueeViewHandler(this);
        mHandler.sendEmptyMessageDelayed(100, mSwitchTime);
    }

    public void stop() {
        if (mHandler == null)
            return;
        mIsStart = false;
        mHandler.removeMessages(100);
    }

    private void carryOn() {
        if (mIsStart && mHandler != null) {
            mHandler.removeMessages(100);
            mHandler.sendEmptyMessageDelayed(100, mSwitchTime);
        }
    }

    private void pause() {
        if (mIsStart && mHandler != null) {
            mHandler.removeMessages(100);
        }
    }

    private class MarqueeObserver extends DataSetObserver {

        @Override
        public void onChanged() {
            super.onChanged();
            addChildView(mAdapter);
        }
    }

    public void setAdapter(MarqueeViewAdapter adapter) {
        adapter.registerDataSetObserver(mMarqueeObserver);
        addChildView(adapter);
    }

    private void addChildView(MarqueeViewAdapter adapter) {
        mAdapter = adapter;
        removeAllViews();
        for (int i = 0; i < adapter.getCount(); i++) {
            final View child = adapter.getView(i, null, this);
            addView(child);
        }

        if (adapter.getCount() > 1) {
            switch (getOrientation()) {
                case ORIENTATION_UP:
                case ORIENTATION_LEFT:
                    // 首添加到尾
                    addView(adapter.getView(0, null, this));
                    break;
                case ORIENTATION_DOWN:
                case ORIENTATION_RIGHT:
                    // 尾添加到首
                    addView(adapter.getView(getChildCount() - 1, null, this), 0);
                    break;
            }
            mItemCount = adapter.getCount() + 1;
        } else {
            mItemCount = adapter.getCount();
        }

        if (mItemCount <= 1) {
            mScroller.forceFinished(true);
            scrollTo(0, 0);
        }
    }

    public int getItemCount() {
        return mItemCount;
    }

    public int getCurrentPosition() {
        return mCurrentPosition;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public int getSwitchTime() {
        return mSwitchTime;
    }

    public void setSwitchTime(int switchTime) {
        mSwitchTime = switchTime;
    }

    public int getScrollTime() {
        return mScrollTime;
    }

    public void setScrollTime(int scrollTime) {
        mScrollTime = scrollTime;
    }

    public boolean isEnableAlphaAnim() {
        return mEnableAlphaAnim;
    }

    public void setEnableAlphaAnim(boolean enableAlphaAnim) {
        mEnableAlphaAnim = enableAlphaAnim;
    }

    public boolean isEnableScaleAnim() {
        return mEnableScaleAnim;
    }

    public void setEnableScaleAnim(boolean enableScaleAnim) {
        mEnableScaleAnim = enableScaleAnim;
    }

    private MarqueeViewHandler mHandler;


    private static class MarqueeViewHandler extends Handler {

        private WeakReference<MarqueeView> mReference;

        public MarqueeViewHandler(MarqueeView marqueeView) {
            mReference = new WeakReference<>(marqueeView);
        }

        @Override
        public void handleMessage(Message msg) {
            MarqueeView marqueeView = mReference.get();
            if (marqueeView != null && marqueeView.mVisible) {
                if (msg.what == 100) {
                    switch (marqueeView.mOrientation) {
                        case ORIENTATION_UP:
                            marqueeView.mCurrentPosition++;
                            marqueeView.smoothScroll(marqueeView.mScrollDistance);
                            break;
                        case ORIENTATION_DOWN:
                            marqueeView.mCurrentPosition--;
                            marqueeView.smoothScroll(-marqueeView.mScrollDistance);
                            break;
                        case ORIENTATION_LEFT:
                            marqueeView.mCurrentPosition++;
                            marqueeView.smoothScroll(marqueeView.mScrollDistance);
                            break;
                        case ORIENTATION_RIGHT:
                            marqueeView.mCurrentPosition--;
                            marqueeView.smoothScroll(-marqueeView.mScrollDistance);
                            break;
                    }
                    marqueeView.postInvalidate();
                    sendEmptyMessageDelayed(100, marqueeView.mSwitchTime);
                }
            }
        }
    }
}
