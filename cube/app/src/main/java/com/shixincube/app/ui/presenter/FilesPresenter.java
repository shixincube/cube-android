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

import android.content.Intent;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.shixincube.app.R;
import com.shixincube.app.ui.activity.TextInputActivity;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.fragment.FilesFragment;
import com.shixincube.app.ui.view.FilesView;
import com.shixincube.app.util.DateUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.FilesTabController;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.OnItemClickListener;
import com.shixincube.app.widget.adapter.OnItemLongClickListener;
import com.shixincube.app.widget.adapter.ViewHolder;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;

import java.io.File;
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
import cube.filestorage.DirectoryListener;
import cube.filestorage.handler.DefaultDirectoryFileUploadHandler;
import cube.filestorage.handler.DefaultDirectoryHandler;
import cube.filestorage.handler.DefaultFileItemListHandler;
import cube.filestorage.model.Directory;
import cube.filestorage.model.FileAnchor;
import cube.filestorage.model.FileItem;
import cube.filestorage.model.FileLabel;
import cube.util.LogUtils;

/**
 * 文件清单。
 */
public class FilesPresenter extends BasePresenter<FilesView> implements FilesTabController.TabChangedListener, DirectoryListener {

    private final static String TAG = FilesPresenter.class.getSimpleName();

    private FilesFragment fragment;

    private FilesTabController tabController;

    private List<FileItem> fileItemList;

    private AdapterForRecyclerView<FileItem> adapter;

    private Directory rootDirectory;

    private Directory currentDirectory;

    public FilesPresenter(BaseActivity activity, FilesFragment fragment, FilesTabController tabController) {
        super(activity);
        this.fragment = fragment;
        this.tabController = tabController;
        this.tabController.setTabChangedListener(this);
        this.fileItemList = new ArrayList<>();
    }

    private FilesFragment getFragment() {
        return this.fragment;
    }

    public Directory getCurrentDirectory() {
        return this.currentDirectory;
    }

    public void loadData() {
        setAdapter();

        // 加载根文件夹
        Promise.create(new PromiseHandler<Directory>() {
            @Override
            public void emit(PromiseFuture<Directory> promise) {
                // 获取自己的根目录
                rootDirectory = CubeEngine.getInstance().getFileStorage().getSelfRoot();

                if (null != rootDirectory) {
                    currentDirectory = rootDirectory;
                    promise.resolve(rootDirectory);

                    // 更新正在上传和下载文件数量
                    tabController.setTransmittingNum(rootDirectory.numUploadingFiles()
                            + rootDirectory.numDownloadingFiles());
                }
                else {
                    promise.reject();
                }
            }
        }).thenOnMainThread(new Future<Directory>() {
            @Override
            public void come(Directory data) {
                refreshData();
            }
        }).catchReject(new Future<Directory>() {
            @Override
            public void come(Directory data) {
                LogUtils.w(TAG, "#loadData failed");
            }
        }).launch();
    }

    /**
     * 上传文件到当前文件夹。
     *
     * @param filepath
     */
    public void uploadFile(String filepath) {
        this.tabController.setTransmittingNum(this.rootDirectory.numUploadingFiles()
                + rootDirectory.numDownloadingFiles() + 1);

        this.currentDirectory.uploadFile(new File(filepath), new DefaultDirectoryFileUploadHandler(true) {
            @Override
            public void handleProgress(FileAnchor fileAnchor, Directory directory) {
                // Nothing
            }

            @Override
            public void handleComplete(FileLabel fileLabel, Directory directory) {
                LogUtils.d(TAG, "#uploadFile - handleComplete : " + fileLabel.getFileName());
                tabController.setTransmittingNum(rootDirectory.numUploadingFiles()
                        + rootDirectory.numDownloadingFiles());
                refreshData();
            }
        }, new DefaultFailureHandler(true) {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                UIUtils.showToast(UIUtils.getString(R.string.operate_failure_with_code, error.code));
            }
        });
    }

    /**
     * 重命名当前文件夹。
     *
     * @param newName
     */
    public void rename(String dirName, String newName) {
        activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

        Directory directory = this.currentDirectory.getSubdirectory(dirName);
        if (null == directory) {
            activity.hideWaitingDialog();
            LogUtils.w(TAG, "Can NOT find subdirectory: " + dirName);
            return;
        }

        directory.renameDirectory(newName, new DefaultDirectoryHandler(true) {
            @Override
            public void handleDirectory(Directory directory) {
                activity.hideWaitingDialog();
            }
        }, new DefaultFailureHandler(true) {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                activity.hideWaitingDialog();
                UIUtils.showToast(UIUtils.getString(R.string.operate_failure_with_code, error.code));
            }
        });
    }

    /**
     * 删除目录。
     *
     * @param directory
     */
    public void deleteDirectory(Directory directory) {
        Runnable deleteTask = () -> {
            activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

            currentDirectory.deleteDirectory(directory, true, new DefaultDirectoryHandler(true) {
                @Override
                public void handleDirectory(Directory directory) {
                    activity.hideWaitingDialog();
                }
            }, new DefaultFailureHandler(true) {
                @Override
                public void handleFailure(Module module, ModuleError error) {
                    activity.hideWaitingDialog();
                    UIUtils.showToast(UIUtils.getString(R.string.operate_failure_with_code, error.code));
                }
            });
        };

        if (directory.isEmpty()) {
            // 空文件夹直接删除
            deleteTask.run();
            return;
        }

        // 提示用户是否确认删除
        activity.showMaterialDialog(UIUtils.getString(R.string.file_delete_dir),
                UIUtils.getString(R.string.tip_delete_directory, directory.getName()),
                UIUtils.getString(R.string.sure),
                UIUtils.getString(R.string.cancel),
                (view) -> {
                    activity.hideMaterialDialog();
                    deleteTask.run();
                }, (view) -> {
                    activity.hideMaterialDialog();
                });
    }

    /**
     * 删除文件。
     *
     * @param fileLabel
     */
    public void deleteFile(FileLabel fileLabel) {
        activity.showMaterialDialog(UIUtils.getString(R.string.file_delete),
                UIUtils.getString(R.string.tip_delete_file, fileLabel.getFileName()),
                UIUtils.getString(R.string.sure),
                UIUtils.getString(R.string.cancel),
                (view) -> {
                    activity.hideMaterialDialog();

                    activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

                    currentDirectory.deleteFile(fileLabel, new DefaultDirectoryHandler(true) {
                        @Override
                        public void handleDirectory(Directory directory) {
                            activity.hideWaitingDialog();
                            refreshData();
                        }
                    }, new DefaultFailureHandler(true) {
                        @Override
                        public void handleFailure(Module module, ModuleError error) {
                            activity.hideWaitingDialog();
                            UIUtils.showToast(UIUtils.getString(R.string.operate_failure_with_code, error.code));
                        }
                    });
                }, (view) -> {
                    activity.hideMaterialDialog();
                });
    }

    private void setAdapter() {
        if (null == this.adapter) {
            this.adapter = new AdapterForRecyclerView<FileItem>(activity, this.fileItemList, R.layout.item_file) {
                @Override
                public void convert(ViewHolderForRecyclerView helper, FileItem item, int position) {
                    if (item.type == FileItem.ItemType.File) {
                        helper.setImageResource(R.id.ivFileIcon, UIUtils.getFileIcon(item.getFileLabel().getFileType()));
                        helper.setText(R.id.tvName, item.getName());
                        helper.setText(R.id.tvDate, DateUtils.formatYMDHM(item.getLastModified()));
                        helper.setViewVisibility(R.id.cbSelector, View.VISIBLE);
                    }
                    else if (item.type == FileItem.ItemType.Directory) {
                        helper.setImageResource(R.id.ivFileIcon, R.mipmap.ic_file_folder);
                        helper.setText(R.id.tvName, item.getName());
                        helper.setText(R.id.tvDate, DateUtils.formatYMDHM(item.getLastModified()));
                        helper.setViewVisibility(R.id.cbSelector, View.VISIBLE);
                    }
                    else if (item.type == FileItem.ItemType.ParentDirectory) {
                        helper.setImageResource(R.id.ivFileIcon, R.mipmap.ic_file_parent);
                        helper.setText(R.id.tvName, "");
                        helper.setText(R.id.tvDate, "");
                        helper.setViewVisibility(R.id.cbSelector, View.GONE);
                    }
                    else if (item.type == FileItem.ItemType.TrashDirectory) {
                        helper.setImageResource(R.id.ivFileIcon, R.mipmap.ic_file_folder);
                        helper.setText(R.id.tvName, item.getName());
                        helper.setText(R.id.tvDate, DateUtils.formatYMDHM(item.getLastModified()));
                        helper.setViewVisibility(R.id.cbSelector, View.VISIBLE);
                    }
                    else if (item.type == FileItem.ItemType.TrashFile) {
                        helper.setImageResource(R.id.ivFileIcon, UIUtils.getFileIcon(item.getFileLabel().getFileType()));
                        helper.setText(R.id.tvName, item.getName());
                        helper.setText(R.id.tvDate, DateUtils.formatYMDHM(item.getLastModified()));
                        helper.setViewVisibility(R.id.cbSelector, View.VISIBLE);
                    }
                }
            };

            // 点击
            this.adapter.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(ViewHolder helper, ViewGroup parent, View itemView, int position) {
                    FileItem fileItem = fileItemList.get(position);
                    if (fileItem.type == FileItem.ItemType.ParentDirectory) {
                        currentDirectory = fileItem.getDirectory();
                        refreshData();
                    }
                    else if (fileItem.type == FileItem.ItemType.Directory) {
                        currentDirectory = fileItem.getDirectory();
                        refreshData();
                    }
                    else if (fileItem.type == FileItem.ItemType.File) {
                        UIUtils.showToast(UIUtils.getString(R.string.developing));
                    }
                }
            });

            // 长按
            this.adapter.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(ViewHolder helper, ViewGroup parent, View itemView, int position) {
                    FileItem fileItem = fileItemList.get(position);
                    if (fileItem.type == FileItem.ItemType.ParentDirectory) {
                        // "回到上一级"不显示菜单
                        return true;
                    }

                    // 显示在文件名下面
                    PopupMenu menu = new PopupMenu(activity, itemView.findViewById(R.id.tvName), Gravity.CENTER);
                    menu.getMenuInflater().inflate(R.menu.menu_file, menu.getMenu());

                    if (fileItem.type == FileItem.ItemType.File) {
                        menu.getMenu().getItem(0).setVisible(false);
                        menu.getMenu().getItem(1).setVisible(false);
                    }
                    else if (fileItem.type == FileItem.ItemType.Directory) {
                        menu.getMenu().getItem(2).setVisible(false);
                        menu.getMenu().getItem(3).setVisible(false);
                        menu.getMenu().getItem(4).setVisible(false);
                    }
                    else {
                        menu.getMenu().getItem(0).setVisible(false);
                        menu.getMenu().getItem(1).setVisible(false);
                        menu.getMenu().getItem(2).setVisible(false);
                        menu.getMenu().getItem(3).setVisible(false);
                        menu.getMenu().getItem(4).setVisible(false);
                    }

                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            if (menuItem.getItemId() == R.id.menuRename) {
                                Intent intent = new Intent(activity, TextInputActivity.class);
                                intent.putExtra("title", UIUtils.getString(R.string.file_rename_dir));
                                intent.putExtra("minLength", 1);
                                intent.putExtra("content", fileItem.getName());
                                getFragment().startActivityForResult(intent, FilesFragment.REQUEST_RENAME_DIR);
                            }
                            else if (menuItem.getItemId() == R.id.menuDeleteDir) {
                                deleteDirectory(fileItem.getDirectory());
                            }
                            else if (menuItem.getItemId() == R.id.menuDownloadFile) {
                                UIUtils.showToast(UIUtils.getString(R.string.developing));
                            }
                            else if (menuItem.getItemId() == R.id.menuMoveFile) {
                                UIUtils.showToast(UIUtils.getString(R.string.developing));
                            }
                            else if (menuItem.getItemId() == R.id.menuDeleteFile) {
                                deleteFile(fileItem.getFileLabel());
                            }

                            return true;
                        }
                    });

                    menu.show();
                    return true;
                }
            });

            getView().getFileListView().setAdapter(this.adapter);
        }
    }

    private void refreshData() {
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

                        if (currentDirectory.hasParent()) {
                            FileItem parent = FileItem.createParentDirectory(currentDirectory.getParent());
                            fileItemList.add(parent);
                        }

                        fileItemList.addAll(itemList);

                        if (fileItemList.isEmpty() && !currentDirectory.hasParent()) {
                            getView().getNoFileLayout().setVisibility(View.VISIBLE);
                            getView().getFileListView().setVisibility(View.GONE);
                        }
                        else {
                            getView().getNoFileLayout().setVisibility(View.GONE);
                            getView().getFileListView().setVisibility(View.VISIBLE);
                        }

                        adapter.notifyDataSetChangedWrapper();
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

            case FilesTabController.TAB_TRASH_FILES:
                getView().getPathText().setText(UIUtils.getString(R.string.file_trash));

                getView().getNoFileLayout().setVisibility(View.GONE);
                getView().getFileListView().setVisibility(View.VISIBLE);

                CubeEngine.getInstance().getFileStorage().getSelfTrashFileItems(new DefaultFileItemListHandler(true) {
                    @Override
                    public void handleFileItemList(List<FileItem> itemList) {
                        fileItemList.clear();

                        fileItemList.addAll(itemList);

                        adapter.notifyDataSetChangedWrapper();

                        getView().getPathText().setText(UIUtils.getString(R.string.tip_recyclebin_trash_num, itemList.size()));
                    }
                });
                break;

            case FilesTabController.TAB_TRANSMITTING_FILES:
                break;

            default:
                break;
        }
    }

    @Override
    public void onTabChanged(int tab) {
        this.refreshData();
    }

    @Override
    public void onNewDirectory(Directory workingDirectory, Directory newDirectory) {
        int tab = this.tabController.getActiveTab();
        if (tab == FilesTabController.TAB_ALL_FILES) {
            this.refreshData();
        }
    }

    @Override
    public void onDeleteDirectory(Directory workingDirectory, Directory deletedDirectory) {
        int tab = this.tabController.getActiveTab();
        if (tab == FilesTabController.TAB_ALL_FILES) {
            this.refreshData();
        }
    }

    @Override
    public void onRenameDirectory(Directory directory) {
        int tab = this.tabController.getActiveTab();
        if (tab == FilesTabController.TAB_ALL_FILES) {
            this.refreshData();
        }
    }

    private String getPathString() {
        if (null == this.currentDirectory) {
            return "/";
        }

        if (this.currentDirectory.isRoot()) {
            return "/";
        }

        StringBuilder buf = new StringBuilder();

        Directory directory = this.currentDirectory;
        while (null != directory) {
            buf.insert(0, directory.getName());
            buf.insert(0, "/");
            directory = directory.getParent();
            if (null == directory || directory.isRoot()) {
                break;
            }
        }

        return buf.toString();
    }
}
