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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.shixincube.app.R;

/**
 * 快速导航条。
 */
public class QuickIndexBar extends View {

    private Paint paint;

    private float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());

    private static final String[] LETTERS = new String[]{
            "↑", "☆", "A", "B", "C", "D", "E", "F", "G", "H",
            "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
            "S", "T", "U", "V", "W", "X", "Y", "Z", "#"
    };

    private int cellWidth;
    private float cellHeight;

    /**
     * 用于记录当前触摸的索引值
     */
    private int touchIndex = -1;

    private LetterUpdateListener listener;

    public QuickIndexBar(Context context) {
        this(context, null);
    }

    public QuickIndexBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuickIndexBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public QuickIndexBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.paint.setColor(getResources().getColor(R.color.side_bar, context.getTheme()));
        this.paint.setTextSize(this.textSize);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        this.cellWidth = getMeasuredWidth();
        this.cellHeight = getMeasuredHeight() * 1.0f / LETTERS.length;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        setBackgroundColor(Color.TRANSPARENT);

        for (int i = 0; i < LETTERS.length; ++i) {
            String text = LETTERS[i];

            // 计算坐标
            float x = (cellWidth * 0.5f - paint.measureText(text) * 0.5f);

            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, text.length(), bounds);

            // 文本高度
            int textHeight = bounds.height();

            float y = (cellHeight * 0.5f + textHeight * 0.5f + i * cellHeight);

            // 绘制文本
            canvas.drawText(text, x, y, this.paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int index = -1;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 获取当前索引，根据坐标位置计算
                // 索引 = 当前 Y 坐标 / 单元格高度
                index = (int) (event.getY() / cellHeight);
                if (index >= 0 && index < LETTERS.length) {
                    if (index != this.touchIndex) {
                        if (null != this.listener) {
                            this.listener.onLetterUpdate(LETTERS[index]);
                            this.touchIndex = index;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // 索引 = 当前 Y 坐标 / 单元格高度
                index = (int) (event.getY() / cellHeight);
                if (index >= 0 && index < LETTERS.length) {
                    if (index != this.touchIndex) {
                        if (null != this.listener) {
                            this.listener.onLetterUpdate(LETTERS[index]);
                            this.touchIndex = index;
                        }
                    }
                }
                setBackgroundColor(getResources().getColor(R.color.side_bar_pressed, getContext().getTheme()));
                break;
            case MotionEvent.ACTION_UP:
                this.touchIndex = -1;
                if (null != this.listener) {
                    this.listener.onLetterCancel();
                }
                setBackgroundColor(Color.TRANSPARENT);
                break;
            default:
                break;
        }

        return true;
    }

    public LetterUpdateListener getListener() {
        return this.listener;
    }

    public void setListener(LetterUpdateListener listener) {
        this.listener = listener;
    }

    public interface LetterUpdateListener {

        void onLetterUpdate(String letter);

        void onLetterCancel();
    }
}
