package com.shixincube.app.widget.adapter;

import android.view.View;
import android.view.ViewGroup;

/**
 * Item 的长按回调
 */
public interface OnItemLongClickListener {
    /**
     * @param helper
     * @param parent   如果是RecyclerView的话，parent为空
     * @param itemView
     * @param position
     */
    boolean onItemLongClick(ViewHolder helper, ViewGroup parent, View itemView, int position);
}
