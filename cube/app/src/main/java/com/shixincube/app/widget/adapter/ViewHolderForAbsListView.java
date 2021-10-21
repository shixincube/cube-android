package com.shixincube.app.widget.adapter;

import android.content.Context;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * ListView和GridView通用的ViewHodler
 */
public class ViewHolderForAbsListView extends ViewHolder {

    public ViewHolderForAbsListView(Context context, int defaultLayouId, final ViewGroup parent) {
        super(parent);
        mContext = context;
        mViews = new SparseArray<>();
        mConvertView = View.inflate(context, defaultLayouId, null);
        mConvertView.setTag(this);

        mConvertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(ViewHolderForAbsListView.this, parent, v, getPosition());
                }
            }
        });

        mConvertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mOnItemLongClickListener != null) {
                    return mOnItemLongClickListener.onItemLongClick(ViewHolderForAbsListView.this, parent, v, getPosition());
                }
                return false;
            }
        });

        mConvertView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mOnItemTouchListener != null) {
                    return mOnItemTouchListener.onItemTouch(ViewHolderForAbsListView.this, v, event, getPosition());
                }
                return false;
            }
        });
    }

    /**
     * 得到ViewHolder
     * <p>
     * 如果之前对应的convertView没有Viewholder就创建一个新的，否则直接从convertView中获取。
     */
    public static ViewHolderForAbsListView get(Context context, int defaultLayouId, int position, View convertView, ViewGroup viewGroup) {

        ViewHolderForAbsListView viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolderForAbsListView(context, defaultLayouId, viewGroup);
        } else {
            viewHolder = (ViewHolderForAbsListView) convertView.getTag();
        }

        viewHolder.setMyPosition(position);
        return viewHolder;
    }


}
