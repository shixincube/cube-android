package com.shixincube.app.widget.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

/**
 * ViewHolder 的抽象类 (是 ViewHolderForAbsListView 和 ViewHolderForRecyclerView 的同一父类)
 */
public class ViewHolder extends RecyclerView.ViewHolder {

    protected Context mContext;
    protected View mConvertView;
    protected SparseArray<View> mViews;
    protected int mMyPosition;// ViewHolderForAbsListView专用（另一个自带getPosition方法）

    protected OnItemClickListener mOnItemClickListener;
    protected OnItemLongClickListener mOnItemLongClickListener;
    protected OnItemTouchListener mOnItemTouchListener;

    public ViewHolder(View itemView) {
        super(itemView);
    }

    /**
     * 根据id得到布局中的View(使用SparseArray保管，提高效率)
     */
    public <T extends View> T getView(int viewId) {
        View view = mViews.get(viewId);
        if (view == null) {
            view = mConvertView.findViewById(viewId);
            mViews.put(viewId, view);
        }
        return (T) view;
    }


    /**
     * 得到当前item对应的View
     */
    public View getConvertView() {
        return mConvertView;
    }

    public int getMyPosition() {
        return mMyPosition;
    }

    public void setMyPosition(int myPosition) {
        mMyPosition = myPosition;
    }

    public OnItemClickListener getOnItemClickListener() {
        return mOnItemClickListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public OnItemLongClickListener getOnItemLongClickListener() {
        return mOnItemLongClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        mOnItemLongClickListener = onItemLongClickListener;
    }

    public OnItemTouchListener getOnItemTouchListener() {
        return mOnItemTouchListener;
    }

    public void setOnItemTouchListener(OnItemTouchListener onItemTouchListener) {
        mOnItemTouchListener = onItemTouchListener;
    }

    /*================== 一切有可能的操作控件的方法 begin ==================*/

    /**
     * 设置 Tag 。
     *
     * @param viewId
     * @param tag
     * @return
     */
    public ViewHolder setTag(int viewId, Object tag) {
        getView(viewId).setTag(tag);
        return this;
    }

    /**
     * 设置 TextView 文字
     */
    public ViewHolder setText(int viewId, String text) {
        TextView tv = getView(viewId);
        tv.setText(text);
        return this;
    }

    /**
     * 设置 TextView 的文字颜色
     */
    public ViewHolder setTextColor(int viewId, int colorId) {
        TextView tv = getView(viewId);
        tv.setTextColor(mContext.getResources().getColor(colorId));
        return this;
    }

    /**
     * 设置ImageView的图片，并返回this
     */
    public ViewHolder setImageResource(int viewId, int resId) {
        ImageView iv = getView(viewId);
        iv.setImageResource(resId);
        return this;
    }

    /**
     * 设置ImageView的图片，并返回this
     */
    public ViewHolder setImageBitmap(int viewId, Bitmap bitmap) {
        ImageView iv = getView(viewId);
        iv.setImageBitmap(bitmap);
        return this;
    }

    /**
     * 设置ImageView的图片，并返回this
     */
    public ViewHolder setImageFileResource(int viewId, String path) {
        ImageView iv = getView(viewId);
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        iv.setImageBitmap(bitmap);
        return this;
    }

    /**
     * 设置背景颜色，并返回this
     */
    public ViewHolder setBackgroundColor(int viewId, int colorId) {
        View view = getView(viewId);
        view.setBackgroundColor(mContext.getResources().getColor(colorId));
        return this;
    }


    /**
     * 设置背景资源，并返回this
     */
    public ViewHolder setBackgrounResource(int viewId, int resId) {
        View view = getView(viewId);
        view.setBackgroundResource(resId);
        return this;
    }

    /**
     * 设置显隐，并返回this
     */
    public ViewHolder setViewVisibility(int viewId, int visibility) {
        View view = getView(viewId);
        view.setVisibility(visibility);
        return this;
    }

    /**
     * 设置是否可用，并返回this
     */
    public ViewHolder setEnabled(int viewId, boolean enabled) {
        View view = getView(viewId);
        view.setEnabled(enabled);
        return this;
    }

    /**
     * 设置是否可获取焦点，并返回this
     */
    public ViewHolder setFocusable(int viewId, boolean focusable) {
        View view = getView(viewId);
        view.setFocusable(focusable);
        return this;
    }

    /*================== 一切有可能操作控件的方法 end ==================*/

}
