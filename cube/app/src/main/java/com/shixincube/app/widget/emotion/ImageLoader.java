package com.shixincube.app.widget.emotion;

import android.content.Context;
import android.widget.ImageView;

/**
 * 图片加载器
 */
public interface ImageLoader {

    void displayImage(Context context, String path, ImageView imageView);
}
