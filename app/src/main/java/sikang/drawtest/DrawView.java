package sikang.drawtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

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

    public DrawView(Context context) {
        super(context);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePoints = new ArrayList<>();
        pathList = new ArrayList<Path>();
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);// 消除锯齿
        mPaint.setDither(true); // 图像抖动
        mPaint.setStrokeWidth(20);
//        mPaint.setAlpha(98);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);// 圆心笔触
    }

    public void initCanvas() {
        mDrawBitmap = Bitmap.createBitmap(getWidth(), getHeight(),
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDrawBitmap != null)
            canvas.drawBitmap(mDrawBitmap, 0, 0, mPaint);
        canvas.drawPath(mPath, mPaint);

    }

    private float lastX, lastY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float eventX = event.getX();
        float eventY = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastX = eventX;
                lastY = eventY;
                mPath.moveTo(eventX, eventY);
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(event);
                break;
            case MotionEvent.ACTION_UP:
                touchUp(eventX, eventY);
                break;
        }
        invalidate();
        return true;
    }

    private void touchMove(MotionEvent event) {
        int historySize = event.getHistorySize();
        for (int i = 0; i < historySize; i++) {
            float historicalX = event.getHistoricalX(i);
            float historicalY = event.getHistoricalY(i);
            float cx1 = lastX + (historicalX - lastX) / 4;
            float cy1 = lastY + (historicalY - lastY) / 4;
            float cx2 = lastX + (historicalX - lastX) / 4 * 3;
            float cy2 = lastY + (historicalY - lastY) / 4 * 3;
            mPath.cubicTo(cx1, cy1, cx2, cy2, historicalX, historicalY);
            lastX = historicalX;
            lastY = historicalY;
        }
    }

    private void touchUp(float x, float y) {
        float cx1 = lastX + (x - lastX) / 4;
        float cy1 = lastY + (y - lastY) / 4;
        float cx2 = lastX + (x - lastX) / 4 * 3;
        float cy2 = lastY + (y - lastY) / 4 * 3;
        mPath.cubicTo(cx1, cy1, cx2, cy2, x, y);
        if (mCanvas == null) {
            initCanvas();
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

        LinePoint(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }
    }
}
