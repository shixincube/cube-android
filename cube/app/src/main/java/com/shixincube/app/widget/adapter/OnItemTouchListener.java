package com.shixincube.app.widget.adapter;

import android.view.MotionEvent;
import android.view.View;

/**
 * Item 的触摸回调
 */
public interface OnItemTouchListener {

    boolean onItemTouch(ViewHolder helper, View childView, MotionEvent event, int position);
}
