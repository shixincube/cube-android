package com.shixincube.app.widget.optionitemview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.shixincube.app.R;

/**
 * 自定义的顶部标题
 */
public class OptionItemView extends View {

    /** 控件的宽 */
    private int width;
    /** 控件的高 */
    private int height;

    private Context context;

    /** 开始位 Bitmap */
    private Bitmap startImage;

    /** 结束位 Bitmap */
    private Bitmap endImage;

    private boolean isShowStartImg = true;
    private boolean isShowStartText = true;
    private boolean isShowEndImg = true;
    private boolean isShowEndText = true;

    // 拆分模式(默认是false，也就是一个整体)
    private boolean splitMode = false;

    /**
     * 判断按下开始的位置是否在左
     */
    private boolean startBeginTouchDown = false;

    /**
     * 判断按下开始的位置是否在中间
     */
    private boolean centerBeginTouchDown = false;

    /**
     * 判断按下开始的位置是否在右
     */
    private boolean endBeginTouchDown = false;

    /**
     * 标题
     */
    private String title = "";

    /**
     * 标题字体大小
     */
    private float titleTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
            16, getResources().getDisplayMetrics());
    /**
     * 标题颜色
     */
    private int titleTextColor = Color.BLACK;

    /**
     * 开始位文字
     */
    private String startText = "";
    /**
     * 开始位文字大小
     */
    private float startTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
            16, getResources().getDisplayMetrics());
    /**
     * 开始位文字左边距
     */
    private int startTextMarginLeft = -1;
    /**
     * 开始位图左边距
     */
    private int startImageMarginLeft = -1;
    /**
     * 开始位图右边距
     */
    private int startImageMarginRight = -1;
    /**
     * 开始位文字颜色
     */
    private int startTextColor = Color.BLACK;

    /**
     * 结束位文字
     */
    private String endText = "";
    /**
     * 结束位文字大小
     */
    private float endTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
            16, getResources().getDisplayMetrics());
    /**
     * 结束位文字颜色
     */
    private int endTextColor = Color.BLACK;
    /**
     * 结束位右边距
     */
    private int endTextMarginRight = -1;
    /**
     * 结束位图左边距
     */
    private int endImageMarginLeft = -1;
    /**
     * 结束位图右边距
     */
    private int endImageMarginRight = -1;

    private Paint paint;
    /**
     * 对文本的约束
     */
    private Rect textBound;
    /**
     * 控制整体布局
     */
    private Rect rect;

    public OptionItemView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.OptionItemView);

        for (int i = 0; i < typedArray.getIndexCount(); i++) {
            int attr = typedArray.getIndex(i);
            if (attr == R.styleable.OptionItemView_start_src) {
                startImage = BitmapFactory.decodeResource(getResources(), typedArray.getResourceId(attr, 0));
            } else if (attr == R.styleable.OptionItemView_end_src) {
                endImage = BitmapFactory.decodeResource(getResources(), typedArray.getResourceId(attr, 0));
            } else if (attr == R.styleable.OptionItemView_title_size) {
                titleTextSize = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                        16, getResources().getDisplayMetrics()));
            } else if (attr == R.styleable.OptionItemView_title_color) {
                titleTextColor = typedArray.getColor(attr, Color.BLACK);
            } else if (attr == R.styleable.OptionItemView_title) {
                title = typedArray.getString(attr);
            } else if (attr == R.styleable.OptionItemView_start_text) {
                startText = typedArray.getString(attr);
            } else if (attr == R.styleable.OptionItemView_start_text_size) {
                startTextSize = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                        16, getResources().getDisplayMetrics()));
            } else if (attr == R.styleable.OptionItemView_start_text_margin_left) {
                startTextMarginLeft = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        -1, getResources().getDisplayMetrics()));
            } else if (attr == R.styleable.OptionItemView_start_image_margin_left) {
                startImageMarginLeft = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        -1, getResources().getDisplayMetrics()));
            } else if (attr == R.styleable.OptionItemView_start_image_margin_right) {
                startImageMarginRight = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        -1, getResources().getDisplayMetrics()));
            } else if (attr == R.styleable.OptionItemView_start_text_color) {
                startTextColor = typedArray.getColor(attr, Color.BLACK);
            } else if (attr == R.styleable.OptionItemView_end_text) {
                endText = typedArray.getString(attr);
            } else if (attr == R.styleable.OptionItemView_end_text_size) {
                endTextSize = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                        16, getResources().getDisplayMetrics()));
            } else if (attr == R.styleable.OptionItemView_end_text_margin_right) {
                endTextMarginRight = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        -1, getResources().getDisplayMetrics()));
            } else if (attr == R.styleable.OptionItemView_end_image_margin_left) {
                endImageMarginLeft = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        -1, getResources().getDisplayMetrics()));
            } else if (attr == R.styleable.OptionItemView_end_image_margin_right) {
                endImageMarginRight = typedArray.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        -1, getResources().getDisplayMetrics()));
            } else if (attr == R.styleable.OptionItemView_end_text_color) {
                endTextColor = typedArray.getColor(attr, Color.BLACK);
            } else if (attr == R.styleable.OptionItemView_split_mode) {
                splitMode = typedArray.getBoolean(attr, false);
            }
        }
        typedArray.recycle();    // 回收 TypeArray

        rect = new Rect();
        paint = new Paint();
        textBound = new Rect();
        // 计算了描绘字体需要的范围
        paint.getTextBounds(title, 0, title.length(), textBound);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        width = getWidth();
        height = getHeight();

        // 抗锯齿处理
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));

        rect.left = getPaddingLeft();
        rect.right = width - getPaddingRight();
        rect.top = getPaddingTop();
        rect.bottom = height - getPaddingBottom();

        // 抗锯齿
        paint.setAntiAlias(true);
        paint.setTextSize(titleTextSize > startTextSize ? titleTextSize > endTextSize ? titleTextSize : endTextSize : startTextSize > endTextSize ? startTextSize : endTextSize);
//        mPaint.setTextSize(titleTextSize);
        paint.setStyle(Paint.Style.FILL);
        // 文字水平居中
        paint.setTextAlign(Paint.Align.CENTER);

        // 计算垂直居中baseline
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        int baseLine = (int) ((rect.bottom + rect.top - fontMetrics.bottom - fontMetrics.top) / 2);

        if (!title.trim().equals("")) {
            // 正常情况，将字体居中
            paint.setColor(titleTextColor);
            canvas.drawText(title, rect.centerX(), baseLine, paint);
            // 取消使用掉的快
            rect.bottom -= textBound.height();
        }


        if (startImage != null && isShowStartImg) {
            // 计算左图范围
            rect.left = startImageMarginLeft >= 0 ? startImageMarginLeft : width / 32;
            rect.right = rect.left + height * 1 / 2;
            rect.top = height / 4;
            rect.bottom = height * 3 / 4;
            canvas.drawBitmap(startImage, null, rect, paint);
        }
        if (endImage != null && isShowEndImg) {
            // 计算右图范围
            rect.right = width - (endImageMarginRight >= 0 ? endImageMarginRight : width / 32);
            rect.left = rect.right - height * 1 / 2;
            rect.top = height / 4;
            rect.bottom = height * 3 / 4;
            canvas.drawBitmap(endImage, null, rect, paint);
        }
        if (startText != null && !startText.equals("") && isShowStartText) {
            paint.setTextSize(startTextSize);
            paint.setColor(startTextColor);
            int w = 0;
            if (startImage != null) {
                w += startImageMarginLeft >= 0 ? startImageMarginLeft : (height / 8);//增加左图左间距
                w += height * 1 / 2;//图宽
                w += startImageMarginRight >= 0 ? startImageMarginRight : (width / 32);// 增加左图右间距
                w += startTextMarginLeft > 0 ? startTextMarginLeft : 0;//增加左字左间距
            } else {
                w += startTextMarginLeft >= 0 ? startTextMarginLeft : (width / 32);//增加左字左间距
            }

            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(startText, w, baseLine, paint);
        }
        if (endText != null && !endText.equals("") && isShowEndText) {
            paint.setTextSize(endTextSize);
            paint.setColor(endTextColor);

            int w = width;
            if (endImage != null) {
                w -= endImageMarginRight >= 0 ? endImageMarginRight : (height / 8);//增加右图右间距
                w -= height * 1 / 2;//增加图宽
                w -= endImageMarginLeft >= 0 ? endImageMarginLeft : (width / 32);//增加右图左间距
                w -= endTextMarginRight > 0 ? endTextMarginRight : 0;//增加右字右间距
            } else {
                w -= endTextMarginRight >= 0 ? endTextMarginRight : (width / 32);//增加右字右间距
            }

            // 计算了描绘字体需要的范围
            paint.getTextBounds(endText, 0, endText.length(), textBound);
            canvas.drawText(endText, w - textBound.width(), baseLine, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //是一个整体，则不拆分各区域的点击
        if (!splitMode)
            return super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int _x = (int) event.getX();
                if (_x < width / 8) {
                    startBeginTouchDown = true;
                } else if (_x > width * 7 / 8) {
                    endBeginTouchDown = true;
                } else {
                    centerBeginTouchDown = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                int x = (int) event.getX();
                if (startBeginTouchDown && x < width / 8 && listener != null) {
                    listener.leftOnClick();
                } else if (endBeginTouchDown && x > width * 7 / 8 && listener != null) {
                    listener.rightOnClick();
                } else if (centerBeginTouchDown && listener != null) {
                    listener.centerOnClick();
                }
                startBeginTouchDown = false;
                centerBeginTouchDown = false;
                endBeginTouchDown = false;
                break;
        }
        return true;
    }

    public void setTitleText(String text) {
        title = text;
        invalidate();
    }

    public void setTitleText(int stringId) {
        title = context.getString(stringId);
        invalidate();
    }

    public void setTitleColor(int color) {
        titleTextColor = color;
        invalidate();
    }

    public void setTitleSize(int sp) {
        titleTextSize = sp2px(context, sp);
        invalidate();
    }

    public void setStartText(String text) {
        startText = text;
        invalidate();
    }

    public void setLeftText(int stringId) {
        startText = context.getString(stringId);
        invalidate();
    }

    public void setStartTextColor(int color) {
        startTextColor = color;
        invalidate();
    }

    public void setStartImageMarginRight(int dp) {
        startImageMarginRight = dp2px(context, dp);
        invalidate();
    }

    public void setStartImageMarginLeft(int dp) {
        this.startImageMarginLeft = dp2px(context, dp);
        invalidate();
    }

    public void setStartTextMarginLeft(int dp) {
        this.startTextMarginLeft = dp2px(context, dp);
        invalidate();
    }

    public void setStartImage(Bitmap bitmap) {
        startImage = bitmap;
        invalidate();
    }

    public void setEndImage(Bitmap bitmap) {
        endImage = bitmap;
        invalidate();
    }

    public void setStartTextSize(int sp) {
        startTextSize = sp2px(context, sp);
        invalidate();
    }

    public void setEndText(String text) {
        endText = text;
        invalidate();
    }

    public void setRightText(int stringId) {
        endText = context.getString(stringId);
        invalidate();
    }

    public void setEndTextColor(int color) {
        endTextColor = color;
        invalidate();
    }

    public void setEndTextSize(int sp) {
        startTextSize = sp2px(context, sp);
        invalidate();
    }

    public void setEndImageMarginLeft(int dp) {
        endImageMarginLeft = dp2px(context, dp);
        invalidate();
    }

    public void setEndImageMarginRight(int dp) {
        this.endImageMarginRight = dp2px(context, dp);
        invalidate();
    }

    public void setEndTextMarginRight(int dp) {
        this.endTextMarginRight = dp2px(context, dp);
        invalidate();
    }

    public void showLeftImg(boolean flag) {
        isShowStartImg = flag;
        invalidate();
    }

    public void showLeftText(boolean flag) {
        isShowStartText = flag;
        invalidate();
    }

    public void showRightImg(boolean flag) {
        isShowEndImg = flag;
        invalidate();
    }

    public void showRightText(boolean flag) {
        isShowEndText = flag;
        invalidate();
    }

    public void setSpliteMode(boolean spliteMode) {
        splitMode = spliteMode;
    }

    public boolean getSpliteMode() {
        return splitMode;
    }

    private OnOptionItemClickListener listener;

    public interface OnOptionItemClickListener {
        void leftOnClick();

        void centerOnClick();

        void rightOnClick();
    }

    public void setOnOptionItemClickListener(OnOptionItemClickListener listener) {
        this.listener = listener;
    }

    private int sp2px(Context context, float spVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                spVal, context.getResources().getDisplayMetrics());
    }

    private int dp2px(Context context, float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, context.getResources().getDisplayMetrics());
    }
}
