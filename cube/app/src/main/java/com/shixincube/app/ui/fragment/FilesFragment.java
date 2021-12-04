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

package com.shixincube.app.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.shixincube.app.R;
import com.shixincube.app.ui.activity.MainActivity;
import com.shixincube.app.ui.base.BaseFragment;
import com.shixincube.app.ui.presenter.FilesPresenter;
import com.shixincube.app.ui.view.FilesView;
import com.shixincube.app.widget.FilesTabController;
import com.shixincube.app.widget.recyclerview.RecyclerView;
import com.shixincube.filepicker.Constant;
import com.shixincube.filepicker.activity.NormalFilePickActivity;
import com.shixincube.filepicker.filter.entity.NormalFile;

import java.util.ArrayList;

import butterknife.BindView;

/**
 * 文件清单。
 */
public class FilesFragment extends BaseFragment<FilesView, FilesPresenter> implements FilesView {

    public final static int REQUEST_FILE_PICKER = 2001;

    @BindView(R.id.llFileClassify)
    LinearLayout fileClassifyTab;

    @BindView(R.id.tvPath)
    TextView pathText;

    @BindView(R.id.llNoFile)
    LinearLayout noFileLayout;
    @BindView(R.id.btnUpload)
    Button uploadButton;

    @BindView(R.id.rvFiles)
    RecyclerView filesView;

    private FilesTabController tabController;

    public FilesFragment() {
        super();
    }

    @Override
    public void init() {
        this.tabController = new FilesTabController();
    }

    @Override
    public void initView(View rootView) {
        this.tabController.bind(this.fileClassifyTab);
    }

    @Override
    public void initData() {
        this.presenter.loadData();
    }

    @Override
    public void initListener() {
        this.uploadButton.setOnClickListener((v) -> {
            Intent intent = new Intent(getActivity(), NormalFilePickActivity.class);
            intent.putExtra(Constant.MAX_NUMBER, 9);
            intent.putExtra(com.shixincube.filepicker.activity.BaseActivity.IS_NEED_FOLDER_LIST, true);
            intent.putExtra(NormalFilePickActivity.SUFFIX,
                    new String[] {"jpg", "jpeg", "png", "bmp",
                            "xlsx", "xls", "doc", "docx", "ppt", "pptx",
                            "pdf", "txt", "log"});
            startActivityForResult(intent, REQUEST_FILE_PICKER);
        });
    }

    @Override
    public void onActivityResult(int responseCode, int resultCode, Intent data) {
        super.onActivityResult(responseCode, resultCode, data);

        if (responseCode == REQUEST_FILE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                ArrayList<NormalFile> fileList = data.getParcelableArrayListExtra(Constant.RESULT_PICK_FILE);
                if (!fileList.isEmpty()) {
                    for (NormalFile file : fileList) {
                        presenter.uploadFile(file.getPath());
                    }
                }
            }
        }
    }

    @Override
    protected FilesPresenter createPresenter() {
        return new FilesPresenter((MainActivity) getActivity(), this.tabController);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.fragment_files;
    }

    @Override
    public LinearLayout getNoFileLayout() {
        return this.noFileLayout;
    }

    @Override
    public TextView getPathText() {
        return this.pathText;
    }

    @Override
    public RecyclerView getFileListView() {
        return this.filesView;
    }
}
