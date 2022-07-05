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

import static android.content.Context.BIND_AUTO_CREATE;

import android.content.Intent;

import com.shixincube.app.AppConsts;
import com.shixincube.app.CubeApp;
import com.shixincube.app.api.Explorer;
import com.shixincube.app.api.RetryWithDelay;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.manager.CubeConnection;
import com.shixincube.app.manager.PreferenceHelper;
import com.shixincube.app.model.MutableNotice;
import com.shixincube.app.model.Notice;
import com.shixincube.app.model.NoticeType;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.MainView;
import com.shixincube.app.util.FileUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.NoticeDialog;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cube.contact.model.ContactZone;
import cube.engine.CubeEngine;
import cube.engine.service.CubeService;
import cube.messaging.MessagingServiceEvent;
import cube.util.LogUtils;
import cube.util.ObservableEvent;
import cube.util.Observer;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 主界面。
 */
public class MainPresenter extends BasePresenter<MainView> implements Observer {

    private final static String TAG = "MainPresenter";

    private boolean useDemoData = !AppConsts.FERRY_MODE;

    private NoticeDialog noticeDialog;

    private Timer timer;

    public MainPresenter(BaseActivity activity) {
        super(activity);

        this.clear();

        this.check();
    }

    private void clear() {
        // 删除更新安装包
        String dir = FileUtils.getDir("");
        String filePath = dir + "Cube_release_" + AppConsts.VERSION + ".apk";
        File apkFile = new File(filePath);
        if (apkFile.exists() && apkFile.length() > 0) {
            apkFile.delete();
        }
    }

    /**
     * 检查魔方引擎服务是否启动，如果没有启动则启动
     */
    private void check() {
        if (CubeEngine.getInstance().hasSignIn()) {
            // 已调用过签入
            return;
        }

        LogUtils.i("MainPresenter", "Launch Cube Engine");

        CubeApp.getMainThreadHandler().post(() -> {
            // 使用连接服务的方式签入账号
            CubeConnection connection = new CubeConnection();
            Intent intent = new Intent(activity, CubeService.class);
            activity.bindService(intent, connection, BIND_AUTO_CREATE);
        });
    }

    public void monitorConversation() {
        CubeEngine.getInstance().getMessagingService().attachWithName(MessagingServiceEvent.RemoteConversationsCompleted,
                this);
        // 设置超时操作
        if (null == this.timer) {
            this.timer = new Timer();
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    CubeApp.getMainThreadHandler().post(() -> {
                        closeWaiting();
                    });
                }
            }, 10 * 1000);
        }
    }

    public void loadDemoData() {
        if (!this.useDemoData) {
            return;
        }

        ContactZone contactZone = CubeEngine.getInstance().
                getContactService().getDefaultContactZone();
        if (null != contactZone) {
            int num = contactZone.getParticipants().size();
            if (num == 0) {
                // 激活内置数据
                Explorer.getInstance().activateBuildInData(
                        AccountHelper.getInstance().getCurrentAccount().id,
                        CubeEngine.getInstance().getConfig().domain,
                        contactZone.getName())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(ContactZoneResponse -> {
                        // 重置本地数据
                        CubeEngine.getInstance().getContactService()
                                .resetContactZoneLocalData(contactZone.getName());
                    }, throwable -> {
                        LogUtils.e("#activateBuildInData", throwable);
                    });
            }
        }
    }

    public void processNotice() {
        Runnable task = () -> {
            // 加载当前的通知
            List<Notice> list = PreferenceHelper.getInstance().loadNotices();

            MutableNotice mutableNotice = new MutableNotice();

            for (Notice current : list) {
                if (current.getType() == NoticeType.ONLY_ONCE) {
                    if (current.getLastShowTime() == 0) {
                        mutableNotice.value = current;
                        break;
                    }
                }
                else if (current.getType() == NoticeType.ONLY_ONCE_A_DAY) {
                    if (current.getLastShowTime() == 0) {
                        mutableNotice.value = current;
                        break;
                    }
                    else {
                        Calendar last = Calendar.getInstance();
                        last.setTimeInMillis(current.getLastShowTime());

                        Calendar now = Calendar.getInstance();

                        if (last.get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR)) {
                            mutableNotice.value = current;
                            break;
                        }
                    }
                }
                else if (current.getType() == NoticeType.EVERY_TIME_START) {
                    mutableNotice.value = current;
                    break;
                }
            }

            if (null != mutableNotice.value) {
                // 设置时间
                mutableNotice.value.setLastShowTime(System.currentTimeMillis());

                // 刷新数据
                PreferenceHelper.getInstance().refreshNotice(mutableNotice.value);

                UIUtils.postTaskDelay(() -> {
                    showNoticeDialog(mutableNotice.value);
                }, 1000);
            }
        };

        Explorer.getInstance().getNotices()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .doOnError(error -> {
                LogUtils.w(TAG, "#processNotice", error);
            })
            .retryWhen(new RetryWithDelay(3000, 2))
            .subscribe(noticeResponse -> {
                // 保存数据
                PreferenceHelper.getInstance(activity.getApplicationContext())
                        .saveNotices(noticeResponse.notices);

                task.run();
            });
    }

    public void showNoticeDialog(Notice notice) {
        if (null == this.noticeDialog) {
            this.noticeDialog = new NoticeDialog(activity);
        }

        this.noticeDialog.show(notice.getTitle(), notice.getContent());
    }

    @Override
    public void update(ObservableEvent event) {
        CubeApp.getMainThreadHandler().post(() -> {
            closeWaiting();
        });
    }

    private void closeWaiting() {
        if (activity.isWaitingDialogShown()) {
            activity.hideWaitingDialog();
        }

        if (null != this.timer) {
            this.timer.cancel();
            this.timer = null;
        }

        CubeEngine.getInstance().getMessagingService().detachWithName(
                MessagingServiceEvent.RemoteConversationsCompleted, this);
    }
}
