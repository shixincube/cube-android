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

package com.shixincube.app.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import androidx.core.content.ContextCompat;

import com.shixincube.app.AppConsts;
import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.UIUtils;
import com.shixincube.camerapicker.CameraView;
import com.shixincube.camerapicker.listener.CameraListener;
import com.shixincube.camerapicker.listener.ClickListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;
import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;

/**
 * 拍摄界面。
 */
public class TakePhotoActivity extends BaseActivity {

    @BindView(R.id.cameraView)
    CameraView cameraView;

    public TakePhotoActivity() {
        super();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null != cameraView) {
            cameraView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != cameraView) {
            cameraView.onPause();
        }
    }

    @Override
    public void init() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // 没有摄像机、写入外部存储或录音权限
            PermissionGen.with(this)
                    .addRequestCode(100)
                    .permissions(Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO)
                    .request();
        }
    }

    @Override
    public void initView() {
        cameraView.setSaveVideoPath(AppConsts.CAMERA_VIDEO_DIR);
    }

    @Override
    public void initListener() {
        cameraView.setCameraListener(new CameraListener() {
            @Override
            public void captureSuccess(Bitmap bitmap) {
                // 保存图像
                String filePath = saveBitmap(bitmap);
                Intent data = new Intent();
                data.putExtra("photo", true);
                data.putExtra("path", filePath);
                setResult(RESULT_OK, data);
                finish();
            }

            @Override
            public void recordSuccess(String url, Bitmap firstFrame) {
                // 保存视频缩略图
                String thumbPath = saveVideoThumbnail(url, firstFrame);
                Intent data = new Intent();
                data.putExtra("photo", false);
                data.putExtra("path", url);
                data.putExtra("thumb", thumbPath);
                setResult(RESULT_OK, data);
                finish();
            }
        });

        cameraView.setLeftClickListener(new ClickListener() {
            @Override
            public void onClick() {
                finish();
            }
        });
    }

    @PermissionSuccess(requestCode = 100)
    public void onPermissionSuccess() {
        UIUtils.postTaskDelay(() -> {
            recreate();
        }, 500);
    }

    @PermissionFail(requestCode = 100)
    public void onPermissionFail() {
        UIUtils.showToast(UIUtils.getString(R.string.can_not_get_camera_permission));
        UIUtils.postTaskDelay(() -> {
            finish();
        }, 500);
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_take_photo;
    }

    private String saveBitmap(Bitmap bitmap) {
        File file = new File(AppConsts.CAMERA_PHOTO_DIR, "photo_" + System.currentTimeMillis() + ".jpg");
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file.getAbsolutePath();
    }

    private String saveVideoThumbnail(String videoURL, Bitmap bitmap) {
        // 提取文件名
        File file = null;
        int index = videoURL.lastIndexOf(File.separator);
        if (index > 0) {
            String name = videoURL.substring(index + 1);
            index = name.lastIndexOf(".");
            if (index > 0) {
                name = name.substring(0, index);
            }
            file = new File(AppConsts.CAMERA_VIDEO_DIR, name + "_thumb.jpg");
        }
        else {
            file = new File(AppConsts.CAMERA_VIDEO_DIR, "video_thumb_" + System.currentTimeMillis() + ".jpg");
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file.getAbsolutePath();
    }
}
