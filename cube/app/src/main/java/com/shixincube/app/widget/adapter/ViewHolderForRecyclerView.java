package com.shixincube.app.widget.adapter;

import android.content.Context;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

/**
 * RecyclerView 通用的 ViewHodler
 */
public class ViewHolderForRecyclerView extends ViewHolder {

    public ViewHolderForRecyclerView(Context context, View itemView) {
        super(itemView);
        mContext = context;
        mConvertView = itemView;
        mViews = new SparseArray<>();

        mConvertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(ViewHolderForRecyclerView.this, null, v, getPosition());
                }
            }
        });

        mConvertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mOnItemLongClickListener != null) {
                    return mOnItemLongClickListener.onItemLongClick(ViewHolderForRecyclerView.this, null, v, getPosition());
                }
                return false;
            }
        });

        mConvertView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mOnItemTouchListener != null) {
                    return mOnItemTouchListener.onItemTouch(ViewHolderForRecyclerView.this, v, event, getPosition());
                }
                return false;
            }
        });

    }
}
