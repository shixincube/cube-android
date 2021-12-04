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

import android.view.View;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.FilesView;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.FilesTabController;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;

import java.util.ArrayList;
import java.util.List;

import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;
import cube.filestorage.handler.DefaultFileItemListHandler;
import cube.filestorage.model.Directory;
import cube.filestorage.model.FileItem;
import cube.util.LogUtils;

/**
 * 文件清单。
 */
public class FilesPresenter extends BasePresenter<FilesView> implements FilesTabController.TabChangedListener {

    private final static String TAG = FilesPresenter.class.getSimpleName();

    private FilesTabController tabController;

    private List<FileItem> fileItemList;

    private AdapterForRecyclerView<FileItem> adapter;

    private Directory currentDirectory;

    public FilesPresenter(BaseActivity activity, FilesTabController tabController) {
        super(activity);
        this.tabController = tabController;
        this.tabController.setTabChangedListener(this);
        this.fileItemList = new ArrayList<>();
    }

    public void loadData() {
        setAdapter();

        // 加载根文件夹
        Promise.create(new PromiseHandler<Directory>() {
            @Override
            public void emit(PromiseFuture<Directory> promise) {
                // 获取自己的根目录
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

    public void uploadFile(String filepath) {
//        this.currentDirectory.uploadFile(new File(filepath), new DefaultFileUploadDirectoryHandler(true) {
//            @Override
//            public void handleProgress(FileAnchor fileAnchor, Directory directory) {
//
//            }
//
//            @Override
//            public void handleComplete(FileLabel fileLabel, Directory directory) {
//
//            }
//        }, new DefaultFailureHandler(true) {
//            @Override
//            public void handleFailure(Module module, ModuleError error) {
//
//            }
//        });
    }

    private void setAdapter() {
        if (null == this.adapter) {
            this.adapter = new AdapterForRecyclerView<FileItem>(activity, this.fileItemList, R.layout.item_file) {
                @Override
                public void convert(ViewHolderForRecyclerView helper, FileItem item, int position) {

                }
            };

            getView().getFileListView().setAdapter(this.adapter);
        }
    }

    private void filterFiles() {
        int tab = this.tabController.getActiveTab();
        switch (tab) {
            case FilesTabController.TAB_ALL_FILES:
                // 显示路径
                getView().getPathText().setText(getPathString());

                // 获取文件项列表
                this.currentDirectory.listFileItems(new DefaultFileItemListHandler(true) {
                    @Override
                    public void handleFileItemList(List<FileItem> itemList) {
                        fileItemList.clear();
                        fileItemList.addAll(itemList);

                        if (fileItemList.isEmpty()) {
                            getView().getNoFileLayout().setVisibility(View.VISIBLE);
                            getView().getFileListView().setVisibility(View.GONE);
                        }
                        else {
                            getView().getNoFileLayout().setVisibility(View.GONE);
                            getView().getFileListView().setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChangedWrapper();
                        }
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        UIUtils.showToast(UIUtils.getString(R.string.operate_failure_with_code, error.code));
                    }
                });
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

    private String getPathString() {
        if (null == this.currentDirectory) {
            return "/";
        }

        if (this.currentDirectory.isRoot()) {
            return "/";
        }

        StringBuilder buf = new StringBuilder();

        Directory parent = null;
        while ((parent = this.currentDirectory.getParent()) != null) {
            buf.insert(0, parent.getName());
            buf.insert(0, "/");
        }

        return buf.toString();
    }
}
