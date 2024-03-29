package com.shixincube.imagepicker.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.viewpager.widget.ViewPager;

import com.shixincube.imagepicker.ImagePicker;
import com.shixincube.imagepicker.R;
import com.shixincube.imagepicker.bean.ImageItem;
import com.shixincube.imagepicker.view.SuperCheckBox;

/**
 * 图片预览。
 */
public class ImagePreviewActivity extends ImagePreviewBaseActivity
        implements ImagePicker.OnImageSelectedListener, View.OnClickListener,
            CompoundButton.OnCheckedChangeListener {

    public static final String IS_ORIGIN = "isOrigin";

    // 是否选中原图
    private boolean isOrigin;
    // 是否选中当前图片的 CheckBox
    private SuperCheckBox mCbCheck;
    // 原图
    private SuperCheckBox mCbOrigin;
    // 确认图片的选择
    private Button mBtnOk;

    private View bottomBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isOrigin = getIntent().getBooleanExtra(ImagePreviewActivity.IS_ORIGIN, false);
        imagePicker.addOnImageSelectedListener(this);

        mBtnOk = (Button) topBar.findViewById(R.id.btn_ok);
        mBtnOk.setVisibility(View.VISIBLE);
        mBtnOk.setOnClickListener(this);

        bottomBar = findViewById(R.id.bottom_bar);
        bottomBar.setVisibility(View.VISIBLE);

        mCbCheck = (SuperCheckBox) findViewById(R.id.cb_check);
        mCbOrigin = (SuperCheckBox) findViewById(R.id.cb_origin);
        mCbOrigin.setText(getString(R.string.origin));
        mCbOrigin.setOnCheckedChangeListener(this);
        mCbOrigin.setChecked(isOrigin);

        // 初始化当前页面的状态
        onImageSelected(0, null, false);
        ImageItem item = mImageItems.get(mCurrentPosition);
        boolean isSelected = imagePicker.isSelect(item);
        mTitleCount.setText(getString(R.string.preview_image_count, Integer.toString(mCurrentPosition + 1), Integer.toString(mImageItems.size())));
        mCbCheck.setChecked(isSelected);
        updateOriginImageSize();
        // 滑动 ViewPager 的时候，根据外界的数据改变当前的选中状态和当前的图片的位置描述文本
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mCurrentPosition = position;
                ImageItem item = mImageItems.get(mCurrentPosition);
                boolean isSelected = imagePicker.isSelect(item);
                mCbCheck.setChecked(isSelected);
                mTitleCount.setText(getString(R.string.preview_image_count,
                        Integer.toString(mCurrentPosition + 1), Integer.toString(mImageItems.size())));
            }
        });
        // 当点击当前选中按钮的时候，需要根据当前的选中状态添加和移除图片
        mCbCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageItem imageItem = mImageItems.get(mCurrentPosition);
                int selectLimit = imagePicker.getSelectLimit();
                if (mCbCheck.isChecked() && selectedImages.size() >= selectLimit) {
                    Toast.makeText(ImagePreviewActivity.this,
                            ImagePreviewActivity.this.getString(R.string.select_limit, Integer.toString(selectLimit)),
                            Toast.LENGTH_SHORT).show();
                    mCbCheck.setChecked(false);
                }
                else {
                    imagePicker.addSelectedImageItem(mCurrentPosition, imageItem, mCbCheck.isChecked());

                    // 每次选择一张图片就计算一次图片总大小
                    if (selectedImages != null && selectedImages.size() > 0) {
                        updateOriginImageSize();
                    }
                    else {
                        mCbOrigin.setText(getString(R.string.origin));
                    }
                }
            }
        });
    }

    private void updateOriginImageSize() {
        long size = 0;
        for (ImageItem ii : selectedImages)
            size += ii.size;
        if (size > 0) {
            String fileSize = Formatter.formatFileSize(ImagePreviewActivity.this, size);
            mCbOrigin.setText(getString(R.string.origin_size, fileSize));
        } else {
            mCbOrigin.setText(getString(R.string.origin));
        }
    }

    /**
     * 图片添加成功后，修改当前图片的选中数量
     * 当调用 addSelectedImageItem 或 deleteSelectedImageItem 都会触发当前回调
     */
    @Override
    public void onImageSelected(int position, ImageItem item, boolean isAdd) {
        if (imagePicker.getSelectImageCount() > 0) {
            mBtnOk.setText(getString(R.string.select_complete,
                    Integer.toString(imagePicker.getSelectImageCount()), Integer.toString(imagePicker.getSelectLimit())));
            mBtnOk.setEnabled(true);
        }
        else {
            mBtnOk.setText(getString(R.string.complete));
            mBtnOk.setEnabled(false);
        }

//        if (mCbOrigin.isChecked()) {
//            long size = 0;
//            for (ImageItem imageItem : selectedImages)
//                size += imageItem.size;
//            String fileSize = Formatter.formatFileSize(this, size);
//            mCbOrigin.setText(getString(R.string.origin_size, fileSize));
//        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_ok) {
            Intent intent = new Intent();
            intent.putExtra(ImagePicker.EXTRA_RESULT_ITEMS, imagePicker.getSelectedImages());
            intent.putExtra(ImagePreviewActivity.IS_ORIGIN, isOrigin);
            setResult(ImagePicker.RESULT_CODE_ITEMS, intent);
            finish();
        }
        else if (id == R.id.btn_back) {
            Intent intent = new Intent();
            intent.putExtra(ImagePreviewActivity.IS_ORIGIN, isOrigin);
            setResult(ImagePicker.RESULT_CODE_BACK, intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(ImagePreviewActivity.IS_ORIGIN, isOrigin);
        setResult(ImagePicker.RESULT_CODE_BACK, intent);
        finish();
        super.onBackPressed();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        if (id == R.id.cb_origin) {
            if (isChecked) {
                long size = 0;
                for (ImageItem item : selectedImages) {
                    size += item.size;
                }
                String fileSize = Formatter.formatFileSize(this, size);
                isOrigin = true;
//                mCbOrigin.setText(getString(R.string.origin_size, fileSize));
            }
            else {
                isOrigin = false;
//                mCbOrigin.setText(getString(R.string.origin));
            }
        }
    }

    @Override
    protected void onDestroy() {
        imagePicker.removeOnImageSelectedListener(this);
        super.onDestroy();
    }

    /**
     * 单击时，隐藏头和尾
     */
    @Override
    public void onImageSingleTap() {
        if (topBar.getVisibility() == View.VISIBLE) {
            topBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.top_out));
            bottomBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            topBar.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            // 通知栏所需颜色
            tintManager.setStatusBarTintResource(R.color.transparent);

            // 给最外层布局加上这个属性表示，Activity全屏显示，且状态栏被隐藏覆盖掉。
            if (Build.VERSION.SDK_INT >= 16)
                content.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
        else {
            topBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.top_in));
            bottomBar.setAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            topBar.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            tintManager.setStatusBarTintResource(R.color.status_bar);//通知栏所需颜色
            // Activity 全屏显示，但状态栏不会被隐藏覆盖，状态栏依然可见，Activity顶端布局部分会被状态遮住
            if (Build.VERSION.SDK_INT >= 16)
                content.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }
}
