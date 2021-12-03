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

package com.shixincube.app.ui.presenter;

import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.FilesView;
import com.shixincube.app.widget.FilesTabController;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;

import cube.engine.CubeEngine;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;
import cube.filestorage.model.Directory;
import cube.util.LogUtils;

/**
 * 文件清单。
 */
public class FilesPresenter extends BasePresenter<FilesView> implements FilesTabController.TabChangedListener {

    private final static String TAG = FilesPresenter.class.getSimpleName();

    private FilesTabController tabController;

    private AdapterForRecyclerView adapter;

    private Directory currentDirectory;

    public FilesPresenter(BaseActivity activity, FilesTabController tabController) {
        super(activity);
        this.tabController = tabController;
        this.tabController.setTabChangedListener(this);
    }

    public void loadData() {
        setAdapter();

        // 加载根文件夹
        Promise.create(new PromiseHandler<Directory>() {
            @Override
            public void emit(PromiseFuture<Directory> promise) {
                Directory directory = CubeEngine.getInstance().getFileStorage().getSelfRoot();
                if (null != directory) {
                    currentDirectory = directory;
                    promise.resolve(directory);
                }
                else {
                    promise.reject();
                }
            }
        }).thenOnMainThread(new Future<Directory>() {
            @Override
            public void come(Directory data) {
                filterFiles();
            }
        }).catchReject(new Future<Directory>() {
            @Override
            public void come(Directory data) {
                LogUtils.w(TAG, "#loadData failed");
            }
        }).launch();
    }

    private void setAdapter() {
        if (null == this.adapter) {

        }
    }

    private void filterFiles() {
        int tab = this.tabController.getActiveTab();
        switch (tab) {
            case FilesTabController.TAB_ALL_FILES:
                break;
            case FilesTabController.TAB_IMAGE_FILES:
                break;
            case FilesTabController.TAB_DOC_FILES:
                break;
            case FilesTabController.TAB_VIDEO_FILES:
                break;
            case FilesTabController.TAB_AUDIO_FILES:
                break;
            default:
                break;
        }
    }

    @Override
    public void onTabChanged(int tab) {
        this.filterFiles();
    }
}
