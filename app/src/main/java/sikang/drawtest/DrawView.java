package sikang.drawtest;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SiKang on 2016/3/28.
 */
public class DrawView extends View {
    private final String TAG = "DrawTest";
    private Canvas mCanvas;
    private Bitmap mDrawBitmap;
    private List<LinePoint> linePoints;
    private List<Path> pathList;
    private Path mPath;
    private Paint mPaint;
    private float mLeft, mTop, mRight, mBottom;
    private boolean rectUpdated;

    public DrawView(Context context) {
        super(context);
        init();
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        rectUpdated = false;
        linePoints = new ArrayList<>();
        pathList = new ArrayList<Path>();
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);// 消除锯齿
        mPaint.setDither(true); // 图像抖动
        mPaint.setStrokeWidth(20);
        mPaint.setAlpha(110);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);// 圆心笔触

    }

    public void initCanvas(int width, int height) {
        if (mCanvas != null) {
            mCanvas = null;
        }
        if (mDrawBitmap != null) {
            mDrawBitmap.recycle();
            mDrawBitmap = null;
        }
        mDrawBitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mDrawBitmap);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = measureWidth(widthMeasureSpec, 200);
        int measuredHeight = measureWidth(heightMeasureSpec, 200);
        Log.d(TAG, measuredWidth + "   " + measuredHeight);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private int measureWidth(int measureSpec, int defaultSize) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int result = defaultSize;
        if (specMode == MeasureSpec.AT_MOST) {
            result = specSize;
        } else if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        }

        return result;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mLeft = right - left;
        mRight = 0;
        mTop = bottom - top;
        mBottom = 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDrawBitmap != null)
            canvas.drawBitmap(mDrawBitmap, 0, 0, mPaint);
        canvas.drawPath(mPath, mPaint);

    }

    private float lastTouchX, lastTouchY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float eventX = event.getX();
        float eventY = event.getY();
        //阻止父级拦截
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                lastTouchX = eventX;
                lastTouchY = eventY;
                mPath.moveTo(eventX, eventY);
                updateRect(eventX, eventY);
                linePoints.add(new LinePoint(eventX, eventY, true));
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(event);
                updateRect(eventX, eventY);
                linePoints.add(new LinePoint(eventX, eventY, false));
                break;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(true);
                touchUp(eventX, eventY);
                updateRect(eventX, eventY);
                linePoints.add(new LinePoint(eventX, eventY, false));
                break;
        }
        invalidate();
        return true;
    }

    private void touchMove(MotionEvent event) {
        int historySize = event.getHistorySize();
        for (int i = 0; i < historySize; i++) {
            addLine(event.getHistoricalX(i), event.getHistoricalY(i));
        }
    }

    private void addLine(float x, float y) {
        float cx1 = lastTouchX + (x - lastTouchX) / 4;
        float cy1 = lastTouchY + (y - lastTouchY) / 4;
        float cx2 = lastTouchX + (x - lastTouchX) / 4 * 3;
        float cy2 = lastTouchY + (y - lastTouchY) / 4 * 3;
        mPath.cubicTo(cx1, cy1, cx2, cy2, x, y);
        lastTouchX = x;
        lastTouchY = y;
    }

    private void updateRect(float x, float y) {
        mLeft = Math.min(mLeft, x);
        mTop = Math.min(mTop, y);
        mRight = Math.max(mRight, x);
        mBottom = Math.max(mBottom, y);
        //取正方形
        if ((mRight - mLeft) > (mBottom - mTop)) {
            mBottom = mTop + (mRight - mLeft);
        } else {
            mRight = mLeft + (mBottom - mTop);
        }

        if (mLeft == x || mTop == y || mRight == x || mBottom == y)
            rectUpdated = true;
    }

    public void changeLayout() {
        if (!rectUpdated)
            return;
        rectUpdated = false;
        mPath.reset();
        float viewWidth =(mRight - mLeft) + 20;
        float viewHeight = (mBottom - mTop) + 20;
        initCanvas((int)viewWidth, (int)viewHeight);
        for (LinePoint point : linePoints) {
            float newX = point.x - mLeft + 10;
            float newY = point.y - mTop + 10;
            Log.d(TAG, point.isStart + "   /   " + newX + "   /   " + newY);
            if (point.isStart) {
                mPath.moveTo(newX, newY);
                lastTouchX = newX;
                lastTouchY = newY;
            } else {
                addLine(newX, newY);
            }
        }
        mCanvas.drawPath(mPath, mPaint);
        mPath.reset();
        layout(getLeft() + (int) mLeft, getTop() + (int) mTop, getLeft() + (int) mRight + 20, getTop() + (int) mBottom + 20);
        invalidate();
        float viewCenterX=(float)getLeft()+getWidth()/2;
        float viewCenterY=(float)getTop()+getHeight()/2;
        Log.d(TAG,"x: "+ 540/viewCenterX+"  y: "+400/viewCenterY);
        scaleView(500 / viewWidth, 500 / viewHeight, 540 - viewCenterX, 400-viewCenterY);
    }


    public void scaleView(float scaleX, float scaleY, float tranX, float tranY) {
        ObjectAnimator viewScaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, scaleX);
        ObjectAnimator viewScaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, scaleY);
        ObjectAnimator viewTranX = ObjectAnimator.ofFloat(this, "translationX", 0f, tranX);
        ObjectAnimator viewTranY = ObjectAnimator.ofFloat(this, "translationY", 0f, tranY);
        AnimatorSet animSet = new AnimatorSet();
        animSet.play(viewScaleX).with(viewScaleY).with(viewTranX).with(viewTranY);
        animSet.setDuration(600);
        animSet.start();

    }

    private void touchUp(float x, float y) {
        float cx1 = lastTouchX + (x - lastTouchX) / 4;
        float cy1 = lastTouchY + (y - lastTouchY) / 4;
        float cx2 = lastTouchX + (x - lastTouchX) / 4 * 3;
        float cy2 = lastTouchY + (y - lastTouchY) / 4 * 3;
        mPath.cubicTo(cx1, cy1, cx2, cy2, x, y);
        if (mCanvas == null) {
            initCanvas(getWidth(), getHeight());
        }
//        pathList.add(new Path(mPath));
//        for (Path path : pathList) {
//            mCanvas.drawPath(path, mPaint);
//        }
        mCanvas.drawPath(mPath, mPaint);
        mPath.reset();
    }

    public void onDestory() {
        if (mCanvas != null) {
            mCanvas = null;
        }
        if (mDrawBitmap != null) {
            mDrawBitmap.recycle();
            mDrawBitmap = null;
        }
        if (pathList != null) {
            pathList = null;
        }

    }

    class LinePoint {
        float x;
        float y;
        boolean isStart;

        LinePoint(float x, float y, boolean isStart) {
            this.x = x;
            this.y = y;
            this.isStart = isStart;
        }

    }
}
