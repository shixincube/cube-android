/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.shixincube.app.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;

import com.shixincube.app.R;

/**
 * 气泡形式的 Image View 。
 */
@SuppressLint("AppCompatCustomView")
public class BubbleImageView extends ImageView {

    private static final int LOCATION_NONE = 0;
    private static final int LOCATION_LEFT = -1;
    private static final int LOCATION_RIGHT = 1;

    private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.ARGB_8888;
    private static final int COLOR_DRAWABLE_DIMENSION = 1;

    private int mAngle = dp2px(10);
    private int mArrowTop = dp2px(40);
    private int mArrowWidth = dp2px(20);
    private int mArrowHeight = dp2px(20);
    private int mArrowOffset = 0;
    private int mArrowLocation = LOCATION_NONE;

    private int textSize = dp2px(12);

    private Rect mDrawableRect;
    private Bitmap mBitmap;
    private BitmapShader mBitmapShader;
    private Paint mBitmapPaint;
    private Matrix mShaderMatrix;
    private int mBitmapWidth;
    private int mBitmapHeight;

    private Paint mPaint;
    private int percent = 0;
    // 是否显示文字
    private boolean mShowText = true;
    // 是否显示阴影
    private boolean mShowShadow = true;

    public BubbleImageView(Context context) {
        this(context, null);
    }

    public BubbleImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(attrs);
        mPaint = new Paint();
    }

    /**
     * 是否显示阴影
     */
    public void showShadow(boolean showShadow) {
        this.mShowShadow = showShadow;
        postInvalidate();
    }

    /**
     * 设置进度的百分比
     */
    public void setPercent(int percent) {
        this.percent = percent;
        postInvalidate();
    }


    /**
     * 设置进度文字是否显示
     */
    public void setProgressVisible(boolean show) {
        this.mShowText = show;
        postInvalidate();
    }

    private void initView(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.BubbleImageView);
            mAngle = (int) a.getDimension(
                    R.styleable.BubbleImageView_bubble_angle, mAngle);
            mArrowHeight = (int) a.getDimension(
                    R.styleable.BubbleImageView_bubble_arrowHeight,
                    mArrowHeight);
            mArrowOffset = (int) a.getDimension(
                    R.styleable.BubbleImageView_bubble_arrowOffset,
                    mArrowOffset);
            mArrowTop = (int) a.getDimension(
                    R.styleable.BubbleImageView_bubble_arrowTop, mArrowTop);
            mArrowWidth = (int) a.getDimension(
                    R.styleable.BubbleImageView_bubble_arrowWidth, mAngle);
            mArrowLocation = a.getInt(
                    R.styleable.BubbleImageView_bubble_arrowLocation,
                    mArrowLocation);
            mShowText = a.getBoolean(R.styleable.BubbleImageView_bubble_showText, mShowText);
            mShowShadow = a.getBoolean(R.styleable.BubbleImageView_bubble_showShadow, mShowShadow);
            a.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() == null) {
            return;
        }

        RectF rect = new RectF(getPaddingStart(), getPaddingTop(), getRight()
                - getLeft() - getPaddingEnd(), getBottom() - getTop()
                - getPaddingBottom());

        Path path = new Path();

        if (mArrowLocation == LOCATION_LEFT) {
            drawLeftArrowPath(rect, path);
        }
        else if (mArrowLocation == LOCATION_RIGHT) {
            drawRightArrowPath(rect, path);
        }
        else {
            drawNoneArrowPath(rect, path);
        }

        canvas.drawPath(path, mBitmapPaint);
        drawText(canvas, mAngle);
    }

    /**
     * 画进度文字和设置透明度
     *
     * @param canvas
     * @param radiusPx 圆角的半径
     */
    private void drawText(Canvas canvas, int radiusPx) {
        mPaint.setAntiAlias(true); // 消除锯齿
        mPaint.setStyle(Paint.Style.FILL);

        if (mShowShadow) {//根据是否要画阴影
            // 画阴影部分
            mPaint.setColor(Color.parseColor("#70000000"));// 半透明
            Rect shadowRect = null;
            if (mArrowLocation == LOCATION_LEFT) {
                shadowRect = new Rect(mArrowWidth, 0, getWidth(), getHeight() - getHeight()
                        * percent / 100);//阴影的宽度（图片的宽度）为 ImageView 的宽度减去箭头的宽度
            }
            else if (mArrowLocation == LOCATION_RIGHT) {
                shadowRect = new Rect(0, 0, getWidth() - mArrowWidth, getHeight() - getHeight()
                        * percent / 100);//阴影的宽度（图片的宽度）为 ImageView 的宽度减去箭头的宽度
            }
            else {
                shadowRect = new Rect(0, 0, getWidth(), getHeight() - getHeight()
                        * percent / 100);
            }
            RectF shadowRectF = new RectF(shadowRect);
            //shadowRectF.set(0, 0, getWidth(), getHeight() - getHeight()* percent / 100 );
            canvas.drawRoundRect(shadowRectF, radiusPx, radiusPx, mPaint);
        }

        // 是否画文字
        if (mShowText) {
            //画文字
            mPaint.setTextSize(this.textSize);
            mPaint.setColor(Color.parseColor("#FFFFFF"));
            mPaint.setStrokeWidth(2);

            String text = percent + "%";

            Rect rect = new Rect();
            // 确定文字的宽度
            mPaint.getTextBounds(text, 0, text.length(), rect);

            // 文字的左边距
            float marginLeft = 0;
            // 文字上边距
            float marginTop = 0;
            if (mArrowLocation == LOCATION_LEFT) {
//                rect = new Rect(mArrowWidth, 0, 0, 0);
                marginLeft = (getWidth() - mArrowWidth) * 0.5f;
            }
            else if (mArrowLocation == LOCATION_RIGHT) {
//                rect = new Rect(mArrowWidth, 0, 0, 0);
                marginLeft = getWidth() * 0.5f - mArrowWidth;
            }
            else {
//                rect = new Rect(0, 0, 0, 0);
                marginLeft = ((float) (getWidth() - rect.width())) * 0.5f;
            }

            marginTop = ((float) getHeight()) * 0.5f;

            canvas.drawText(text, marginLeft, marginTop, mPaint);
        }
    }

    private void drawNoneArrowPath(RectF rect, Path path) {
//        path.moveTo(0, rect.top);
//        path.lineTo(rect.width(), rect.top);
//        path.lineTo(rect.width(), rect.bottom);
//        path.lineTo(0, rect.bottom);
//        path.lineTo(0, 0);
        path.moveTo(mAngle, rect.top);
        path.lineTo(rect.width(), rect.top);
        path.arcTo(new RectF(rect.right - mAngle * 2, rect.top,
                rect.right, mAngle * 2 + rect.top), 270, 90);
        path.lineTo(rect.right, rect.height() - mAngle);
        path.arcTo(new RectF(rect.right - mAngle * 2, rect.bottom
                - mAngle * 2, rect.right, rect.bottom), 0, 90);
        path.lineTo(rect.left, rect.bottom);
        path.arcTo(new RectF(rect.left, rect.bottom - mAngle * 2, mAngle * 2
                + rect.left, rect.bottom), 90, 90);
        path.lineTo(rect.left, rect.top);
        path.arcTo(new RectF(rect.left, rect.top, mAngle * 2 + rect.left,
                mAngle * 2 + rect.top), 180, 90);
        path.close();
    }

    private void drawRightArrowPath(RectF rect, Path path) {
        path.moveTo(mAngle, rect.top);
        path.lineTo(rect.width(), rect.top);
        path.arcTo(new RectF(rect.right - mAngle * 2 - mArrowWidth, rect.top,
                rect.right - mArrowWidth, mAngle * 2 + rect.top), 270, 90);
        path.lineTo(rect.right - mArrowWidth, mArrowTop);
        path.lineTo(rect.right, mArrowTop - mArrowOffset);
        path.lineTo(rect.right - mArrowWidth, mArrowTop + mArrowHeight);
        path.lineTo(rect.right - mArrowWidth, rect.height() - mAngle);
        path.arcTo(new RectF(rect.right - mAngle * 2 - mArrowWidth, rect.bottom
                - mAngle * 2, rect.right - mArrowWidth, rect.bottom), 0, 90);
        path.lineTo(rect.left, rect.bottom);
        path.arcTo(new RectF(rect.left, rect.bottom - mAngle * 2, mAngle * 2
                + rect.left, rect.bottom), 90, 90);
        path.lineTo(rect.left, rect.top);
        path.arcTo(new RectF(rect.left, rect.top, mAngle * 2 + rect.left,
                mAngle * 2 + rect.top), 180, 90);
        path.close();
    }

    private void drawLeftArrowPath(RectF rect, Path path) {
        path.moveTo(mAngle + mArrowWidth, rect.top);
        path.lineTo(rect.width(), rect.top);
        path.arcTo(new RectF(rect.right - mAngle * 2, rect.top, rect.right,
                mAngle * 2 + rect.top), 270, 90);
        path.lineTo(rect.right, rect.top);
        path.arcTo(new RectF(rect.right - mAngle * 2, rect.bottom - mAngle * 2,
                rect.right, rect.bottom), 0, 90);
        path.lineTo(rect.left + mArrowWidth, rect.bottom);
        path.arcTo(new RectF(rect.left + mArrowWidth, rect.bottom - mAngle * 2,
                mAngle * 2 + rect.left + mArrowWidth, rect.bottom), 90, 90);
        path.lineTo(rect.left + mArrowWidth, mArrowTop + mArrowHeight);
        path.lineTo(rect.left, mArrowTop - mArrowOffset);
        path.lineTo(rect.left + mArrowWidth, mArrowTop);
        path.lineTo(rect.left + mArrowWidth, rect.top);
        path.arcTo(new RectF(rect.left + mArrowWidth, rect.top, mAngle * 2
                + rect.left + mArrowWidth, mAngle * 2 + rect.top), 180, 90);
        path.close();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setup();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        mBitmap = bm;
        setup();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        mBitmap = getBitmapFromDrawable(drawable);
        setup();
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        mBitmap = getBitmapFromDrawable(getDrawable());
        setup();
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        try {
            Bitmap bitmap;

            if (drawable instanceof ColorDrawable) {
                bitmap = Bitmap.createBitmap(COLOR_DRAWABLE_DIMENSION,
                        COLOR_DRAWABLE_DIMENSION, BITMAP_CONFIG);
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight(), BITMAP_CONFIG);
            }

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    private void setup() {
        if (mBitmap == null) {
            return;
        }

        mBitmapShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);

        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setShader(mBitmapShader);

        mBitmapHeight = mBitmap.getHeight();
        mBitmapWidth = mBitmap.getWidth();

        updateShaderMatrix();
        invalidate();
    }

    private void updateShaderMatrix() {
        float scale;
        float dx = 0;
        float dy = 0;

        mShaderMatrix = new Matrix();
        mShaderMatrix.set(null);

        mDrawableRect = new Rect(0, 0, getRight() - getLeft(), getBottom()
                - getTop());

        if (mBitmapWidth * mDrawableRect.height() > mDrawableRect.width()
                * mBitmapHeight) {
            scale = mDrawableRect.height() / (float) mBitmapHeight;
            dx = (mDrawableRect.width() - mBitmapWidth * scale) * 0.5f;
        } else {
            scale = mDrawableRect.width() / (float) mBitmapWidth;
            dy = (mDrawableRect.height() - mBitmapHeight * scale) * 0.5f;
        }

        mShaderMatrix.setScale(scale, scale);
        mShaderMatrix.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));

        mBitmapShader.setLocalMatrix(mShaderMatrix);
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getContext().getResources().getDisplayMetrics());
    }
}
