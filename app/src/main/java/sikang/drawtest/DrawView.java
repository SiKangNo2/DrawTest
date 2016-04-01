package sikang.drawtest;

import android.animation.Animator;
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
public class DrawView extends View implements Animator.AnimatorListener {
    private final String TAG = "DrawTest";
    private Canvas mCanvas;
    private Bitmap mDrawBitmap;
    private List<LinePoint> linePoints;
    private List<Path> pathList;
    private Path mPath;
    private Paint mPaint;
    private float mLeft, mTop, mRight, mBottom;
    private boolean rectUpdated, isEnlarging;

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
        isEnlarged = false;
        isEnlarging = false;
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
        mPaint.setStrokeCap(Paint.Cap.ROUND);// 圆形笔触

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

    private float lastTouchX, lastTouchY, startRawX, startRawY;
    private boolean isDoubleTouch, isDoubleClick, isEnlarged;
    private long lastClickTime;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                //是否双击（双击放大画板3倍，进行细节绘制）
                checkDoubleClick(event);
                //记录按下时相关参数
                touchDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                //双击后继续拖动手指进行可自由缩放
                if (isDoubleClick) {
                    if (isEnlarged)
                        scaleView(event);
                } else {
                    //单指移动进行路径绘制
                    if (event.getPointerCount() == 1) {
                        if (!isDoubleTouch)
                            touchMove(event);
                    } else if (event.getPointerCount() == 2) {
                        //双指移动时，如果当前为放大状态，则拖动画板（区域绘制）
                        isDoubleTouch = true;
                        if (isEnlarged)
                            moveView(event.getRawX(), event.getRawY());
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isDoubleClick) {
                } else if (!isDoubleTouch) {
                    //单指抬起，进行绘制结束的完善工作
                    touchUp(event);
                } else
                    isDoubleTouch = false;

                break;
        }
        invalidate();
        return true;
    }

    /**
     * 是否双击
     *
     * @param event 触摸点
     */
    private void checkDoubleClick(MotionEvent event) {
        long time = System.currentTimeMillis();
        isDoubleClick = false;
        if (!isEnlarging && time - lastClickTime <= 500) {
            if (Math.abs(event.getRawX() - startRawX) <= 20 && Math.abs(event.getRawY() - startRawY) <= 20) {
                isEnlarged = !isEnlarged;
                if (isEnlarged) {
                    scaleViewAnimator(3f, -(event.getX() - getWidth() / 2), -(event.getY() - getHeight() / 2));
                } else {
                    scaleViewAnimator(1f, 0f, 0f);
                }
                isDoubleClick = true;
                lastClickTime = 0;
            }
        }
        startRawX = event.getRawX();
        startRawY = event.getRawY();
        mTranX = getTranslationX();
        mTranY = getTranslationY();
        lastClickTime = time;
    }

    /**
     * 记录按下时的相关数据
     *
     * @param event 触摸点
     */
    private void touchDown(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        getParent().requestDisallowInterceptTouchEvent(true);
        lastTouchX = x;
        lastTouchY = y;
        mPath.moveTo(x, y);
        updateRect(x, y);
        linePoints.add(new LinePoint(x, y, true));

    }

    /**
     * 拖动画板
     *
     * @param rawX 当前触摸点的x坐标
     * @param rawY 当前触摸点的y坐标
     */
    private void moveView(float rawX, float rawY) {
        setTranslationX(mTranX + (rawX - startRawX));
        setTranslationY(mTranY + (rawY - startRawY));
    }

    private float mScale, mTranX, mTranY;

    /**
     * 缩放画板（双击放大后不松开继续移动手指时自由缩放）
     *
     * @param event 触摸点
     */
    private void scaleView(MotionEvent event) {
        float scale;
        if (Math.abs(event.getRawX() - startRawX) > Math.abs(event.getRawY() - startRawY)) {
            scale = mScale + ((event.getRawX() - startRawX) / getWidth()) * 10;
        } else {
            scale = mScale - ((event.getRawY() - startRawY) / getHeight()) * 10;
        }
        Log.d(TAG, "width: " + scale);
        if (scale < 1f) {
            scale = 1f;
            mScale = 1f;
            startRawX = event.getRawX();
            startRawY = event.getRawY();
        }
        setScaleX(scale);
        setScaleY(scale);
    }

    /**
     * 绘制path
     *
     * @param event 触摸点
     */
    private void touchMove(MotionEvent event) {
        if ((Math.abs(event.getRawX() - startRawX) < 20 && Math.abs(event.getRawY() - startRawY) < 20))
            return;
        int historySize = event.getHistorySize();
        for (int i = 0; i < historySize; i++) {
            float historyX = event.getHistoricalX(i);
            float historyY = event.getHistoricalY(i);
            addLine(historyX, historyY);
            updateRect(historyX, historyY);
            linePoints.add(new LinePoint(historyX, historyY, false));
        }

    }

    /**
     * 更新路径
     *
     * @param x 触摸点的x坐标
     * @param y 触摸点的y坐标
     */
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
        float viewWidth = (mRight - mLeft) + 20;
        float viewHeight = (mBottom - mTop) + 20;
        initCanvas((int) viewWidth, (int) viewHeight);
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
        float viewCenterX = (float) getLeft() + getWidth() / 2;
        float viewCenterY = (float) getTop() + getHeight() / 2;
        Log.d(TAG, "x: " + 540 / viewCenterX + "  y: " + 400 / viewCenterY);
        scaleMoveView(500 / viewWidth, 500 / viewHeight, 540 - viewCenterX, 400 - viewCenterY);
    }


    public void scaleMoveView(float scaleX, float scaleY, float tranX, float tranY) {
        ObjectAnimator viewScaleX = ObjectAnimator.ofFloat(this, "scaleX", 1f, scaleX);
        ObjectAnimator viewScaleY = ObjectAnimator.ofFloat(this, "scaleY", 1f, scaleY);
        ObjectAnimator viewTranX = ObjectAnimator.ofFloat(this, "translationX", 0f, tranX);
        ObjectAnimator viewTranY = ObjectAnimator.ofFloat(this, "translationY", 0f, tranY);
        AnimatorSet animSet = new AnimatorSet();
        animSet.play(viewScaleX).with(viewScaleY).with(viewTranX).with(viewTranY);
        animSet.setDuration(600);
        animSet.start();
    }

    /**
     * 缩放和位移动画（区域放大和还原）
     *
     * @param scale 要缩放的比例
     * @param tranX x轴位移的距离
     * @param tranY y轴位移的距离
     */
    public void scaleViewAnimator(float scale, float tranX, float tranY) {
        AnimatorSet animSet = new AnimatorSet();
        ObjectAnimator viewScaleX = ObjectAnimator.ofFloat(this, "scaleX", getScaleX(), scale);
        ObjectAnimator viewScaleY = ObjectAnimator.ofFloat(this, "scaleY", getScaleX(), scale);
        ObjectAnimator viewTranX = ObjectAnimator.ofFloat(this, "translationX", getTranslationX(), tranX);
        ObjectAnimator viewTranY = ObjectAnimator.ofFloat(this, "translationY", getTranslationY(), tranY);
        animSet.addListener(this);
        animSet.play(viewScaleX).with(viewScaleY).with(viewTranX).with(viewTranY);
        animSet.setDuration(300);
        animSet.start();
    }


    /**
     * 抬起手指时数据保存
     *
     * @param event 触摸点
     */
    private void touchUp(MotionEvent event) {
        if (Math.abs(event.getRawX() - startRawX) <= 20 && Math.abs(event.getRawY() - startRawY) <= 20) {
            return;
        }
        float x = event.getX();
        float y = event.getY();
        getParent().requestDisallowInterceptTouchEvent(true);
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
        updateRect(x, y);
        linePoints.add(new LinePoint(x, y, false));
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

    @Override
    public void onAnimationStart(Animator animation) {
        isEnlarging = true;
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        isEnlarging = false;
        mScale = getScaleX();
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

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
