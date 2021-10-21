package com.shixincube.app.widget.adapter;

import android.view.View;
import android.view.ViewGroup;

/**
 * Item 点击回调
 */
public interface OnItemClickListener {
    /**
     * @param helper
     * @param parent 如果是RecyclerView的话，parent为空
     * @param itemView
     * @param position
     */
    void onItemClick(ViewHolder helper, ViewGroup parent, View itemView, int position);
}
