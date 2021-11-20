package com.shixincube.app.widget.complexbitmap;

import android.content.Context;

import com.shixincube.app.widget.complexbitmap.helper.Builder;

public class ComplexBitmap {

    public static Builder init(Context context) {
        return new Builder(context);
    }
}
