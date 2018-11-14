package com.mgcoco.signatureview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class SignatureView extends View {

    public static final float DEFAULT_THINKNESS = 3.0f;

    private Bitmap mBitmap;

    private float mMaxPressure = 50;

    private float mPatchX, mPatchY, mPatchP;

    private float mTouchX = 0 , mTouchY = 0 , mTouchP = 0;

    private final ArrayList<Float> mStrokeX = new ArrayList<>();
    private final ArrayList<Float> mStrokeY = new ArrayList<>();
    private final ArrayList<Float> mStrokeP = new ArrayList<>();
    private final ArrayList<Integer> mStrokeC = new ArrayList<>();

    private float mThickness = 3.0f;

    private Paint mPenPaint;

    private Canvas mCanvas;

    private boolean mNeedPatch = false;

    private boolean mPressurePath = false;

    public SignatureView(Context context) {
        super(context);
        init(context, null);
    }

    public SignatureView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SignatureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs){

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SignatureView);

        mThickness = typedArray.getFloat(R.styleable.SignatureView_msv_pen_thinkness, DEFAULT_THINKNESS);
        int penColor = typedArray.getColor(R.styleable.SignatureView_msv_pen_color, getResources().getColor(android.R.color.black, null));
        mPressurePath = typedArray.getBoolean(R.styleable.SignatureView_msv_pen_pressure, false);

        mPenPaint = new Paint();
        mPenPaint.setAntiAlias(true);
        mPenPaint.setColor(penColor);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if(w > 0 && h > 0){
            mBitmap = Bitmap.createBitmap(w, h , Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mCanvas.drawBitmap(mBitmap, 0, 0, null);
        }
    }

    private void draw(){
        int numberOfCoordinate = mStrokeX.size();

        int index = numberOfCoordinate - 1;
        if(index >= 0){
            float x2 = mStrokeX.get(index);
            float y2 = mStrokeY.get(index);
            float p2 = mStrokeP.get(index);

            switch (index) {
                case 0:
                    drawLine(x2, y2, x2, y2, p2, x2, y2, p2);
                    break;

                case 1:
                    float x1 = mStrokeX.get(index - 1);
                    float y1 = mStrokeY.get(index - 1);
                    float p1 = mStrokeP.get(index - 1);
                    drawLine(x2, y2, x1, y1, p1, x1, y1, p1);
                    break;

                default:
                    float x = mStrokeX.get(index - 2);
                    float y = mStrokeY.get(index - 2);
                    float p = mStrokeP.get(index - 2);
                    x1 = mStrokeX.get(index - 1);
                    y1 = mStrokeY.get(index - 1);
                    p1 = mStrokeP.get(index - 1);
                    drawLine(x2, y2, x1, y1, p1, x, y, p);
                    break;
            }
        }
    }

    @SuppressLint("NewApi")
    public boolean onTouchEvent(MotionEvent event) {
        // Getting action state which is press down or pick up
        int action = event.getAction();

        InputDevice inDevice = event.getDevice();

        List<InputDevice.MotionRange> l = inDevice.getMotionRanges();

        mMaxPressure = event.getDevice().getMotionRanges().get(0).getMax();

        mTouchX = (int)event.getX();
        mTouchY = (int)event.getY();
        mTouchP = event.getPressure() * l.get(0).getRange();

        if(mPressurePath)
            mTouchP = mMaxPressure;

        mTouchP = mThickness + (mThickness * (mTouchP / mMaxPressure));

        addStrokePoint(mTouchX, mTouchY, mTouchP);
        draw();

        if(action == MotionEvent.ACTION_UP || (action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP) {
            clear();
            mNeedPatch = false;
        }
        invalidate();
        return true;
    }

    @SuppressLint({ "DrawAllocation", "DrawAllocation" })
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    @SuppressLint("FloatMath")
    private int distance(float x1, float y1, float x2, float y2){
        return (int)Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
    }

    private void drawLine(float x2, float y2, float x1, float y1, float p1, float x, float y, float p){

        float dis = distance(x1, y1, x, y);

        float tGap = 0.01f, tStart = 0.225f, tEnd = 0.775f;

        float gap = 100;

        if(dis > gap) {
            tGap = 0.005f;
        }

        float patchX = mPatchX, patchY = mPatchY, patchP = mPatchP;
        float firstX = getThreeDBezierValue(x, x1, x2, tStart), firstY = getThreeDBezierValue(y, y1, y2, tStart), firstP = mPatchP;

        float[] lastPoint = null;

        for(float t = tStart; t <= tEnd; t = t + tGap){
            drawCircle(x, y, p, x1, y1, p1, x2, y2, t);

            if(lastPoint != null && (Math.abs(lastPoint[0] - mPatchX) > 1 || Math.abs(lastPoint[1] - mPatchY) > 1)){
                for (float tt = 0.2f; tt <= 0.8; tt = tt + 0.2f) {
                    patchCircle(lastPoint[0], lastPoint[1], lastPoint[2], mPatchX, mPatchY, mPatchP, tt);
                }
            }

            lastPoint = new float[]{mPatchX, mPatchY, mPatchP};
        }

        if(mNeedPatch) {
            for (float t = 0; t <= 1; t = t + 0.02f) {
                patchCircle(patchX, patchY, patchP, firstX, firstY, firstP, t);
            }
        }
        mNeedPatch = true;
        return;
    }

    private void drawCircle(float x, float y, float p, float x1, float y1, float p1, float x2, float y2, float t){
        mPatchX = getThreeDBezierValue(x, x1, x2, t);
        mPatchY = getThreeDBezierValue(y, y1, y2, t);
        mPatchP = (p1 - p) * t + p;
        mCanvas.drawCircle(mPatchX, mPatchY, mPatchP, mPenPaint);
    }

    private void patchCircle(float x, float y, float p, float x1, float y1, float p1, float t){
        float patchX = getTwoBezierValue(x, x1, x1, t);
        float patchY = getTwoBezierValue(y, y1, y1, t);
        float patchP = (p1 - p) * t + p;
        mCanvas.drawCircle(patchX, patchY, patchP, mPenPaint);
    }

    private float getThreeDBezierValue(float x, float x1, float x2, float t){
        return x*(1-t)*(1-t)*(1-t)+3*x1*t*(1-t)*(1-t)+3*x1*t*t*(1-t)+x2*t*t*t;
    }

    private float getTwoBezierValue(float x, float x1, float x2, float t){
        return x*(1-t)*(1-t)*(1-t)+3*x1*t*(1-t)*(1-t)+3*x1*t*t*(1-t)+x2*t*t*t;
    }

    private void clear(){
        mStrokeX.clear();
        mStrokeY.clear();
        mStrokeP.clear();
        mStrokeC.clear();
        return;
    }

    private void addStrokePoint(float x0, float y0, float p0){
        mStrokeX.add(x0);
        mStrokeY.add(y0);
        mStrokeP.add(p0);
        mStrokeC.add(mPenPaint.getColor());
        return;
    }

    public void setThickness(int level) {
        mThickness = level;
    }

    public void setColor(int R, int G, int B) {
        mPenPaint.setARGB(255, R, G, B);
    }

    public void clearCanvas(){
        mStrokeX.removeAll(mStrokeX);
        mStrokeY.removeAll(mStrokeY);
        mStrokeP.removeAll(mStrokeP);
        mStrokeC.removeAll(mStrokeC);

        mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mCanvas.drawBitmap(mBitmap, 0, 0, null);
        invalidate();
    }

    public Bitmap getSignature(){
        return mBitmap;
    }
}
