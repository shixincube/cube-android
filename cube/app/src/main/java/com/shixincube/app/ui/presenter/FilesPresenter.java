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
import com.shixincube.app.ui.activity.MainActivity;
import com.shixincube.app.ui.activity.TextInputActivity;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.fragment.FilesFragment;
import com.shixincube.app.ui.view.FilesView;
import com.shixincube.app.util.DateUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.FilesTabController;
import com.shixincube.app.widget.MainBottomMenu;
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
import cube.fileprocessor.util.CalculationUtils;
import cube.filestorage.DirectoryListener;
import cube.filestorage.handler.DefaultDirectoryFileUploadHandler;
import cube.filestorage.handler.DefaultDirectoryHandler;
import cube.filestorage.handler.DefaultFileItemListHandler;
import cube.filestorage.handler.DefaultSearchResultHandler;
import cube.filestorage.handler.DefaultSharingTagHandler;
import cube.filestorage.handler.DefaultTrashHandler;
import cube.filestorage.model.Directory;
import cube.filestorage.model.FileAnchor;
import cube.filestorage.model.FileItem;
import cube.filestorage.model.FileLabel;
import cube.filestorage.model.SearchFilter;
import cube.filestorage.model.SearchResultItem;
import cube.filestorage.model.SharingTag;
import cube.filestorage.model.Trash;
import cube.util.LogUtils;

/**
 * 文件清单。
 */
public class FilesPresenter extends BasePresenter<FilesView>
        implements FilesTabController.TabChangedListener, DirectoryListener, MainBottomMenu.OnClickListener {

    private final static String TAG = FilesPresenter.class.getSimpleName();

    private final static String[] sImageTypes = new String[]{ "png", "jpg", "jpeg", "bmp", "webp", "gif" };
    private final static String[] sDocTypes = new String[]{ "doc", "docx", "pdf", "ppt", "pptx", "xls", "xlsx" };
    private final static String[] sVideoTypes = new String[]{ "mp4", "avi", "mov", "rm", "rmvb", "3gp", "mkv", "mpg", "mpeg" };
    private final static String[] sAudioTypes = new String[]{ "mp3", "wav", "wma", "amr", "flac", "ogg", "mid", "aac", "raw" };

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

        ((MainActivity) activity).getBottomMenu().setOnClickListener(this);
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
        if (null == this.currentDirectory) {
            return;
        }

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

    /**
     * 抹除数据。
     *
     * @param item
     */
    public void eraseTrash(FileItem item) {
        CubeEngine.getInstance().getFileStorage().eraseTrash(
                (item.type == FileItem.ItemType.TrashFile) ? item.getTrashFile() : item.getTrashDirectory(),
                new DefaultTrashHandler(true) {
                    @Override
                    public void handleTrash(Trash trash) {
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
     * 恢复数据。
     *
     * @param item
     */
    public void restoreTrash(FileItem item) {
        CubeEngine.getInstance().getFileStorage().restoreTrash(
                (item.type == FileItem.ItemType.TrashFile) ? item.getTrashFile() : item.getTrashDirectory(),
                new DefaultTrashHandler(true) {
                    @Override
                    public void handleTrash(Trash trash) {
                        refreshData();
                    }
                }, new DefaultFailureHandler(true) {
                    @Override
                    public void handleFailure(Module module, ModuleError error) {
                        UIUtils.showToast(UIUtils.getString(R.string.operate_failure_with_code, error.code));
                    }
                });
    }

    public void showSharingMenu(FileItem item) {
        ((MainActivity) this.activity).showBottomMenu(item);
    }

    public void filterFile(String[] types) {

    }

    private void setAdapter() {
        if (null == this.adapter) {
            this.adapter = new AdapterForRecyclerView<FileItem>(activity, this.fileItemList, R.layout.item_file) {
                @Override
                public void convert(ViewHolderForRecyclerView helper, FileItem item, int position) {
                    if (item.type == FileItem.ItemType.File) {
                        helper.setImageResource(R.id.ivFileIcon, UIUtils.getFileIcon(item.getFileLabel().getFileType()));
                        helper.setText(R.id.tvName, item.getName());
                        // 文件修改日期
                        helper.setText(R.id.tvDate, DateUtils.formatYMDHM(item.getLastModified()));
                        // 文件大小
                        helper.setViewVisibility(R.id.tvSize, View.VISIBLE);
                        helper.setText(R.id.tvSize,
                                CalculationUtils.formatByteDataSize(item.getFileLabel().getFileSize()));
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
                        // 文件修改日期
                        helper.setText(R.id.tvDate, DateUtils.formatYMDHM(item.getLastModified()));
                        // 文件大小
                        helper.setViewVisibility(R.id.tvSize, View.VISIBLE);
                        helper.setText(R.id.tvSize,
                                CalculationUtils.formatByteDataSize(item.getFileLabel().getFileSize()));
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
                        menu.getMenu().getItem(5).setVisible(false);
                        menu.getMenu().getItem(6).setVisible(false);
                    }
                    else if (fileItem.type == FileItem.ItemType.Directory) {
                        menu.getMenu().getItem(2).setVisible(false);
                        menu.getMenu().getItem(3).setVisible(false);
                        menu.getMenu().getItem(4).setVisible(false);
                        menu.getMenu().getItem(5).setVisible(false);
                        menu.getMenu().getItem(6).setVisible(false);
                        menu.getMenu().getItem(7).setVisible(false);
                    }
                    else if (fileItem.type == FileItem.ItemType.TrashDirectory ||
                        fileItem.type == FileItem.ItemType.TrashFile) {
                        menu.getMenu().getItem(0).setVisible(false);
                        menu.getMenu().getItem(1).setVisible(false);
                        menu.getMenu().getItem(2).setVisible(false);
                        menu.getMenu().getItem(3).setVisible(false);
                        menu.getMenu().getItem(4).setVisible(false);
                        menu.getMenu().getItem(7).setVisible(false);
                    }
                    else {
                        menu.getMenu().getItem(0).setVisible(false);
                        menu.getMenu().getItem(1).setVisible(false);
                        menu.getMenu().getItem(2).setVisible(false);
                        menu.getMenu().getItem(3).setVisible(false);
                        menu.getMenu().getItem(4).setVisible(false);
                        menu.getMenu().getItem(5).setVisible(false);
                        menu.getMenu().getItem(6).setVisible(false);
                        menu.getMenu().getItem(7).setVisible(false);
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
                            else if (menuItem.getItemId() == R.id.menuRestoreTrash) {
                                restoreTrash(fileItem);
                            }
                            else if (menuItem.getItemId() == R.id.menuEraseTrash) {
                                eraseTrash(fileItem);
                            }
                            else if (menuItem.getItemId() == R.id.menuSharing) {
                                showSharingMenu(fileItem);
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
        if (null == this.currentDirectory) {
            return;
        }

        if (null == getView()) {
            UIUtils.postTaskDelay(() -> {
                refreshData();
            }, 1000);
            return;
        }

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
                activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

                // 显示内容提示
                getView().getPathText().setText(UIUtils.getString(R.string.file_image));

                getView().getNoFileLayout().setVisibility(View.GONE);
                getView().getFileListView().setVisibility(View.VISIBLE);

                CubeEngine.getInstance().getFileStorage().searchSelfFile(new SearchFilter(sImageTypes),
                        new DefaultSearchResultHandler(true) {
                    @Override
                    public void handleSearchResult(List<SearchResultItem> resultItems) {
                        fileItemList.clear();

                        for (SearchResultItem item : resultItems) {
                            fileItemList.add(item.toFileItem());
                        }

                        if (fileItemList.isEmpty()) {
                            getView().getFileListView().setVisibility(View.GONE);
                            getView().getNotFindTipText().setVisibility(View.VISIBLE);
                        }
                        else {
                            getView().getFileListView().setVisibility(View.VISIBLE);
                            getView().getNotFindTipText().setVisibility(View.GONE);
                        }

                        adapter.notifyDataSetChangedWrapper();

                        activity.hideWaitingDialog();
                    }
                });
                break;

            case FilesTabController.TAB_DOC_FILES:
                activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

                // 显示内容提示
                getView().getPathText().setText(UIUtils.getString(R.string.file_doc));

                getView().getNoFileLayout().setVisibility(View.GONE);
                getView().getFileListView().setVisibility(View.VISIBLE);

                CubeEngine.getInstance().getFileStorage().searchSelfFile(new SearchFilter(sDocTypes),
                        new DefaultSearchResultHandler(true) {
                    @Override
                    public void handleSearchResult(List<SearchResultItem> resultItems) {
                        fileItemList.clear();

                        for (SearchResultItem item : resultItems) {
                            fileItemList.add(item.toFileItem());
                        }

                        if (fileItemList.isEmpty()) {
                            getView().getFileListView().setVisibility(View.GONE);
                            getView().getNotFindTipText().setVisibility(View.VISIBLE);
                        }
                        else {
                            getView().getFileListView().setVisibility(View.VISIBLE);
                            getView().getNotFindTipText().setVisibility(View.GONE);
                        }

                        adapter.notifyDataSetChangedWrapper();

                        activity.hideWaitingDialog();
                    }
                });
                break;

            case FilesTabController.TAB_VIDEO_FILES:
                activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

                // 显示内容提示
                getView().getPathText().setText(UIUtils.getString(R.string.file_video));

                getView().getNoFileLayout().setVisibility(View.GONE);
                getView().getFileListView().setVisibility(View.VISIBLE);

                CubeEngine.getInstance().getFileStorage().searchSelfFile(new SearchFilter(sVideoTypes),
                        new DefaultSearchResultHandler(true) {
                    @Override
                    public void handleSearchResult(List<SearchResultItem> resultItems) {
                        fileItemList.clear();

                        for (SearchResultItem item : resultItems) {
                            fileItemList.add(item.toFileItem());
                        }

                        if (fileItemList.isEmpty()) {
                            getView().getFileListView().setVisibility(View.GONE);
                            getView().getNotFindTipText().setVisibility(View.VISIBLE);
                        }
                        else {
                            getView().getFileListView().setVisibility(View.VISIBLE);
                            getView().getNotFindTipText().setVisibility(View.GONE);
                        }

                        adapter.notifyDataSetChangedWrapper();

                        activity.hideWaitingDialog();
                    }
                });
                break;

            case FilesTabController.TAB_AUDIO_FILES:
                activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

                // 显示内容提示
                getView().getPathText().setText(UIUtils.getString(R.string.file_audio));

                getView().getNoFileLayout().setVisibility(View.GONE);
                getView().getFileListView().setVisibility(View.VISIBLE);

                CubeEngine.getInstance().getFileStorage().searchSelfFile(new SearchFilter(sAudioTypes),
                        new DefaultSearchResultHandler(true) {
                    @Override
                    public void handleSearchResult(List<SearchResultItem> resultItems) {
                        fileItemList.clear();

                        for (SearchResultItem item : resultItems) {
                            fileItemList.add(item.toFileItem());
                        }

                        if (fileItemList.isEmpty()) {
                            getView().getFileListView().setVisibility(View.GONE);
                            getView().getNotFindTipText().setVisibility(View.VISIBLE);
                        }
                        else {
                            getView().getFileListView().setVisibility(View.VISIBLE);
                            getView().getNotFindTipText().setVisibility(View.GONE);
                        }

                        adapter.notifyDataSetChangedWrapper();

                        activity.hideWaitingDialog();
                    }
                });
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

                        if (fileItemList.isEmpty()) {
                            getView().getFileListView().setVisibility(View.GONE);
                            getView().getNotFindTipText().setVisibility(View.VISIBLE);
                        }
                        else {
                            getView().getFileListView().setVisibility(View.VISIBLE);
                            getView().getNotFindTipText().setVisibility(View.GONE);
                        }

                        adapter.notifyDataSetChangedWrapper();

                        getView().getPathText().setText(UIUtils.getString(R.string.tip_recyclebin_trash_num, itemList.size()));
                    }
                });
                break;

            case FilesTabController.TAB_TRANSMITTING_FILES:
                getView().getPathText().setText(UIUtils.getString(R.string.file_transmitting));

                // TODO

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

    @Override
    public void onItemClick(int resourceId, Object data) {
        FileItem fileItem = (FileItem) data;
        switch (resourceId) {
            case R.id.llShareToContact:
                break;
            case R.id.llShareToHyperlink:
                CubeEngine.getInstance().getFileStorage().createSharingTag(fileItem.fileLabel,
                        new DefaultSharingTagHandler() {
                            @Override
                            public void handle(SharingTag sharingTag) {
                                // 复制分享链接
                            }
                        }, new DefaultFailureHandler() {
                            @Override
                            public void handleFailure(Module module, ModuleError error) {

                            }
                        });
                break;
            case R.id.llShareToWeChat:
                break;
            case R.id.llShareToQQ:
                break;
            case R.id.llShareToOther:
                UIUtils.showToast(UIUtils.getString(R.string.developing));
                break;
            default:
                break;
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
