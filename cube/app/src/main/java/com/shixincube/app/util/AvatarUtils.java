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

package com.shixincube.app.util;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.shixincube.app.R;
import com.shixincube.app.model.Account;
import com.shixincube.app.widget.complexbitmap.ComplexBitmap;
import com.shixincube.app.widget.complexbitmap.layout.SquareNineLayoutManager;

import cube.contact.model.Contact;
import cube.contact.model.Group;

/**
 * 头像辅助函数。
 */
public class AvatarUtils {

    private final static SquareNineLayoutManager sAvatarLayoutManager = new SquareNineLayoutManager();

    private AvatarUtils() {
    }

    public static void fillGroupAvatar(Context context, Group group, ImageView imageView) {
        int[] resources = new int[group.numMembers()];
        int index = 0;
        for (Contact contact : group.getMemberList()) {
            resources[index++] = AvatarUtils.getAvatarResource(contact);
        }

        ComplexBitmap.init(context)
                .setLayoutManager(sAvatarLayoutManager)
                .setSize(60)
                .setGap(4)
                .setGapColor(UIUtils.getColor(R.color.avatar))
                .setResourceIds(resources)
                .setImageView(imageView)
                .build();
    }

    public static int getAvatarResource(Contact contact) {
        String name = (null == contact) ? null : Account.getAvatar(contact);
        return explainAvatarForResource(name);
    }

    public static void fillContactAvatar(Context context, Contact contact, ImageView imageView) {
        Glide.with(context).load(getAvatarResource(contact)).centerCrop().into(imageView);
    }

    public static int getAvatarResource(String avatarName) {
        return explainAvatarForResource(avatarName);
    }

    private static int explainAvatarForResource(String avatarName) {
        if (null == avatarName) {
            return R.mipmap.avatar_default;
        }

        if (avatarName.equals("avatar01")) { return R.mipmap.avatar_01; }
        else if (avatarName.equals("avatar02")) { return R.mipmap.avatar_02; }
        else if (avatarName.equals("avatar03")) { return R.mipmap.avatar_03; }
        else if (avatarName.equals("avatar04")) { return R.mipmap.avatar_04; }
        else if (avatarName.equals("avatar05")) { return R.mipmap.avatar_05; }
        else if (avatarName.equals("avatar06")) { return R.mipmap.avatar_06; }
        else if (avatarName.equals("avatar07")) { return R.mipmap.avatar_07; }
        else if (avatarName.equals("avatar08")) { return R.mipmap.avatar_08; }
        else if (avatarName.equals("avatar09")) { return R.mipmap.avatar_09; }
        else if (avatarName.equals("avatar10")) { return R.mipmap.avatar_10; }
        else if (avatarName.equals("avatar11")) { return R.mipmap.avatar_11; }
        else if (avatarName.equals("avatar12")) { return R.mipmap.avatar_12; }
        else if (avatarName.equals("avatar13")) { return R.mipmap.avatar_13; }
        else if (avatarName.equals("avatar14")) { return R.mipmap.avatar_14; }
        else if (avatarName.equals("avatar15")) { return R.mipmap.avatar_15; }
        else if (avatarName.equals("avatar16")) { return R.mipmap.avatar_16; }
        else { return R.mipmap.avatar_default; }
    }
}
