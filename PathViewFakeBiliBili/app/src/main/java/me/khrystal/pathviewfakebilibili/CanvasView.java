package me.khrystal.pathviewfakebilibili;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * usage:
 * author: kHRYSTAL
 * create time: 19/4/2
 * update time:
 * email: 723526676@qq.com
 */
public class CanvasView extends View {

    private Path mPath = new Path();
    private Paint mPaint = new Paint();
    private Bitmap mBitmap;
    private Canvas mCanvas;

    {
        mPaint.setAntiAlias(true);
        mPaint.setColor(getResources().getColor(R.color.colorAccent));
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(7);
    }

    public CanvasView(Context context) {
        super(context);
    }

    public CanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CanvasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPath.moveTo(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                mPath.lineTo(event.getX(), event.getY());
                break;
        }
        mCanvas.drawPath(mPath, mPaint);
        invalidate();
        return true;
    }

    public Path[] getPaths() {
        List<Path> paths = new ArrayList<>();
        PathMeasure pathMeasure = new PathMeasure();
        pathMeasure.setPath(mPath, false);
        Path path;
        do {
            path = new Path();
            path.rLineTo(0, 0);
            pathMeasure.getSegment(0, pathMeasure.getLength(), path, true);
            if (!path.isEmpty()) {
                paths.add(path);
            }
        } while (pathMeasure.nextContour());

        return paths.toArray(new Path[]{});
    }

    public void clear() {
        setPath(new Path());
    }

    public void setPath(Path path) {
        mPath = path;
        mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_4444);
        mCanvas = new Canvas(mBitmap);
        mCanvas.drawPath(mPath, mPaint);
        invalidate();
    }

    public void setLineWidth(int width) {
        mPaint.setStrokeWidth(width);
        invalidate();
    }
}
