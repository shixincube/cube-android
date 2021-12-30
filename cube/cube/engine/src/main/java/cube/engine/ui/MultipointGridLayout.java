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

package cube.engine.ui;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import cube.engine.R;

/**
 * 视频格子布局。
 */
public class MultipointGridLayout extends RelativeLayout {

    private int count = 1;
    private int row = 1;
    private int column = 2;

    private int height = 0;

    public MultipointGridLayout(Context context) {
        super(context);
    }

    public MultipointGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultipointGridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MultipointGridLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setNeededCount(int count) {
        this.count = count;

        this.analyse();

        this.requestLayout();
    }

    public ImageView getAvatarView(int index) {
        ViewGroup layout = (ViewGroup) getChildAt(index);
        return layout.findViewWithTag("200");
    }

    public ViewGroup getVideoContainer(int index) {
        ViewGroup layout = (ViewGroup) getChildAt(index);
        return layout.findViewWithTag("100");
    }

    public void stopWaiting(int index) {
        ViewGroup layout = (ViewGroup) getChildAt(index);
        View animView = layout.findViewWithTag("300");
        AnimationDrawable anim = (AnimationDrawable) animView.getBackground();
        anim.stop();
        animView.setVisibility(View.GONE);
    }

    public void closeView(int index) {
        this.stopWaiting(index);
        ViewGroup layout = (ViewGroup) getChildAt(index);
        layout.setVisibility(View.GONE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // 计算单个 View 宽度
        int itemWidth = (width - getPaddingLeft() - getPaddingRight()) / this.column;
        int childCount = getChildCount();

        for (int i = 0; i < childCount; ++i) {
            View child = getChildAt(i);
            int itemSpec = MeasureSpec.makeMeasureSpec(itemWidth, MeasureSpec.EXACTLY);
            measureChild(child, itemSpec, itemSpec);
        }

        this.height = width;
        setMeasuredDimension(width, this.height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int index = 0;
        for (int i = 0; i < this.row; ++i) {
            for (int j = 0; j < this.column; ++j) {
                View child = getChildAt(index);
                int width = child.getMeasuredWidth();
                int height = child.getMeasuredHeight();
                int left = j * width;
                int top = i * height;

                child.layout(left, top, left + width, top + height);

                ++index;
            }
        }
    }

    private void analyse() {
        switch (this.count) {
            case 1:
                this.row = 1;
                this.column = 1;
                break;
            case 2:
                this.row = 1;
                this.column = 2;
                break;
            case 3:
            case 4:
                this.row = 2;
                this.column = 2;
                break;
            case 5:
            case 6:
                this.row = 2;
                this.column = 3;
                break;
            default:
                break;
        }

        for (int i = 0, len = getChildCount(); i < len; ++i) {
            View view = getChildAt(i);
            if (i < this.count) {
                view.setVisibility(View.VISIBLE);
                View animView = view.findViewWithTag("300");
                animView.setVisibility(View.VISIBLE);
                animView.setBackgroundResource(R.drawable.cube_group_call_waiting_animation);
                AnimationDrawable anim = (AnimationDrawable) animView.getBackground();
                anim.start();
            }
            else {
                view.setVisibility(View.GONE);
            }
        }
    }
}
