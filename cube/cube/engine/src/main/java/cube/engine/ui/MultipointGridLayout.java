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

import java.util.ArrayList;
import java.util.List;

import cube.engine.R;

/**
 * 视频格子布局。
 */
public class MultipointGridLayout extends RelativeLayout {

    private int row = 1;
    private int column = 2;

    private int height = 0;

    private List<Long> displayIds;
    private List<RelativeLayout> grids;

    public MultipointGridLayout(Context context) {
        super(context);
        this.init();
    }

    public MultipointGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init();
    }

    public MultipointGridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init();
    }

    public MultipointGridLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.init();
    }

    private void init() {
        this.displayIds = new ArrayList<>(9);
        this.grids = new ArrayList<>(9);
    }

    private boolean isDisplayed(Long id) {
        synchronized (this.displayIds) {
            return this.displayIds.contains(id);
        }
    }

    private int indexOf(Long id) {
        synchronized (this.displayIds) {
            return this.displayIds.indexOf(id);
        }
    }

    private List<ViewGroup> getDisplayedGrids() {
        ArrayList<ViewGroup> list = new ArrayList<>();
        synchronized (this.displayIds) {
            for (int i = 0; i < this.displayIds.size(); ++i) {
                Long id = this.displayIds.get(i);
                if (id.longValue() > 0) {
                    list.add(this.grids.get(i));
                }
            }
        }
        return list;
    }

    public ImageView showGrid(Long id) {
        if (isDisplayed(id)) {
            return getAvatarView(id);
        }

        synchronized (this.displayIds) {
            for (int i = 0; i < this.displayIds.size(); ++i) {
                Long current = this.displayIds.get(i);
                if (current.longValue() == 0) {
                    this.displayIds.set(i, id);
                    break;
                }
            }
        }

        this.refresh();
        this.requestLayout();
        return this.getAvatarView(id);
    }

    public void closeGrid(Long id) {
        this.stopWaiting(id);

        int index = this.indexOf(id);
        ViewGroup layout = (ViewGroup) getChildAt(index);
        layout.setVisibility(View.GONE);

        synchronized (this.displayIds) {
            this.displayIds.set(index, 0L);
        }

        this.refresh();
        this.requestLayout();
    }

    public ImageView getAvatarView(Long id) {
        int index = this.indexOf(id);
        ViewGroup layout = (ViewGroup) getChildAt(index);
        return layout.findViewWithTag("200");
    }

    public ViewGroup getVideoContainer(Long id) {
        int index = this.indexOf(id);
        ViewGroup layout = (ViewGroup) getChildAt(index);
        return layout.findViewWithTag("100");
    }

    public void playWaiting(Long id) {
        int index = this.indexOf(id);
        ViewGroup layout = (ViewGroup) getChildAt(index);
        ViewGroup mask = layout.findViewWithTag("300");
        mask.setVisibility(View.VISIBLE);

        View animView = mask.findViewWithTag("310");
        animView.setBackgroundResource(R.drawable.cube_group_call_waiting_animation);
        AnimationDrawable anim = (AnimationDrawable) animView.getBackground();
        anim.start();
    }

    public void stopWaiting(Long id) {
        int index = this.indexOf(id);
        ViewGroup layout = (ViewGroup) getChildAt(index);
        ViewGroup mask = layout.findViewWithTag("300");
        mask.setVisibility(View.GONE);

        View animView = mask.findViewWithTag("310");
        AnimationDrawable anim = (AnimationDrawable) animView.getBackground();
        anim.stop();
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);

        this.displayIds.add(0L);
        this.grids.add((RelativeLayout) child);
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
        List<ViewGroup> list = getDisplayedGrids();
        for (int i = 0; i < this.row && index < list.size(); ++i) {
            for (int j = 0; j < this.column && index < list.size(); ++j) {
                View child = list.get(index);
                int width = child.getMeasuredWidth();
                int height = child.getMeasuredHeight();
                int left = j * width;
                int top = i * height;

                child.layout(left, top, left + width, top + height);

                ++index;
            }
        }
    }

    private void refresh() {
        int count = 0;

        for (int i = 0; i < this.displayIds.size(); ++i) {
            Long id = this.displayIds.get(i);
            if (id.longValue() > 0) {
                ++count;
                this.grids.get(i).setVisibility(View.VISIBLE);
            }
            else {
                this.grids.get(i).setVisibility(View.GONE);
            }
        }

        switch (count) {
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
            case 7:
            case 8:
            case 9:
                this.row = 3;
                this.column = 3;
                break;
            default:
                break;
        }
    }
}
