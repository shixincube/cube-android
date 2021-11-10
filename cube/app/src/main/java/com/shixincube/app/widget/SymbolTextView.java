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

package com.shixincube.app.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * 支持显示末尾字符的 TextView 封装。
 */
@SuppressLint("AppCompatCustomView")
public class SymbolTextView extends TextView {

    public SymbolTextView(Context context) {
        super(context);
    }

    public SymbolTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SymbolTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SymbolTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (getLineCount() == 0) {
            return;
        }

        if (getLayout() == null) {
            return;
        }

        int ellipsisCount = getLayout().getEllipsisCount(getLineCount() - 1);
        if (ellipsisCount == 0) {
            return;
        }

        String content = getText().toString();

        String lastString = null;

        // 尝试提取以 . 号分隔的最后几个字符
        int index = content.lastIndexOf(".");
        if (index > 0) {
            lastString = content.substring(index + 1);
            if (lastString.length() > 4) {
                lastString = lastString.substring(lastString.length() - 4);
            }

            int measuredWidth = getMeasuredWidth();
            int lineCount = getLineCount();
            int maxMW = measuredWidth * lineCount;

            while (getPaint().measureText(content + "..." + lastString) > maxMW) {
                content = content.substring(0, content.length() - 1);
            }

            setText(content + "..." + lastString);
        }
    }
}
