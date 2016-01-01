package com.github.ashkansarlak.recmodplay.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.github.ashkansarlak.recmodplay.R;
import com.github.ashkansarlak.recmodplay.utility.Convert;
import com.github.ashkansarlak.recmodplay.utility.Res;

/**
 * Created by Ashkan on 12/29/2015.
 */
public class VolumeMeter extends View {

    private static final float MAX_WIDTH = Convert.dpToPx(12/*dp*/);
    private static final float MAX_HEIGHT = Convert.dpToPx(144/*dp*/);
    private static final int DEFAULT_METER_COLOR = Res.getColor(R.color.blue);
    private static final int FRAME_COLOR = Color.BLACK;
    private static final float FRAME_WIDTH = Convert.dpToPx(1/*dp*/);

    private int meterColor = DEFAULT_METER_COLOR;
    private int max;
    private double current;

    public VolumeMeter(Context context) {
        super(context);
    }

    public VolumeMeter(Context context, AttributeSet attrs) {
        super(context, attrs);
        fetchAttrs(context, attrs);
    }

    public VolumeMeter(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        fetchAttrs(context, attrs);
    }

    private void fetchAttrs(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.VolumeMeter, 0, 0);
        try {
            this.meterColor = ta.getColor(R.styleable.VolumeMeter_meterColor, DEFAULT_METER_COLOR);
        } finally {
            ta.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = (int) Math.min(MAX_WIDTH, widthSize);
        } else {
            //Be whatever you want
            width = (int) MAX_WIDTH;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = (int) Math.min(MAX_HEIGHT, heightSize);
        } else {
            //Be whatever you want
            height = (int) MAX_HEIGHT;
        }

        //MUST CALL THIS
        setMeasuredDimension(width, height);
    }

    private Paint paint = new Paint();

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        float width = Math.min(getWidth(), MAX_WIDTH);
        float height = Math.min(getHeight(), MAX_HEIGHT);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        float barWidth = width - 2 * FRAME_WIDTH;
        float barHeight = height - 2 * FRAME_WIDTH;

        paint.setColor(meterColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(centerX - barWidth/2, (float) (centerY - (barHeight * (2*current/max - 1)) /2),
                        centerX + barWidth/2, centerY + barHeight/2, paint);

        paint.setColor(FRAME_COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(FRAME_WIDTH);
        canvas.drawRect(0, 0, width, height, paint);
    }

    public void setMax(int max) {
        this.max = max;
    }

    public void setCurrent(double current) {
        this.current = current;
        postInvalidate();
    }
}
