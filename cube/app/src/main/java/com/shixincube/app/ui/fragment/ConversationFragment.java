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

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.shixincube.app.AppConsts;
import com.shixincube.app.R;
import com.shixincube.app.ui.activity.MainActivity;
import com.shixincube.app.ui.base.BaseFragment;
import com.shixincube.app.ui.presenter.ConversationPresenter;
import com.shixincube.app.ui.view.ConversationView;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import cube.engine.CubeEngine;
import cube.ferry.handler.DefaultDetectHandler;
import cube.messaging.MessagingServiceEvent;
import cube.util.LogUtils;
import cube.util.ObservableEvent;
import cube.util.Observer;

/**
 * 最近消息会话界面。
 */
public class ConversationFragment extends BaseFragment<ConversationView, ConversationPresenter> implements ConversationView, Observer {

    @BindView(R.id.llState)
    LinearLayout layoutState;

    @BindView(R.id.rvConversations)
    RecyclerView recentConversationView;

    private AtomicBoolean first = new AtomicBoolean(true);

    private AtomicBoolean loaded = new AtomicBoolean(false);

    public ConversationFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CubeEngine.getInstance().getMessagingService().attachWithName(MessagingServiceEvent.Ready, this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!this.first.get()) {
            this.presenter.loadConversations();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        CubeEngine.getInstance().getMessagingService().setConversationEventListener(null);
        CubeEngine.getInstance().getMessagingService().detachWithName(MessagingServiceEvent.Ready, this);
    }

    @Override
    public void init() {
    }

    @Override
    public void initData() {
        if (CubeEngine.getInstance().getMessagingService().isReady() && !this.loaded.get()) {
            this.loaded.set(true);
            this.presenter.loadConversations();
            this.first.set(false);
        }

        CubeEngine.getInstance().getMessagingService().setConversationEventListener(this.presenter);

        if (AppConsts.FERRY_MODE) {
            CubeEngine.getInstance().getFerryService().detectDomain(new DefaultDetectHandler() {
                @Override
                public void handleResult(boolean online, long duration) {
                    LogUtils.d("ConversationFragment", "#detectDomain : "
                            + online + " - " + duration);

                    if (online) {
                        layoutState.setVisibility(View.GONE);
                    }
                    else {
                        layoutState.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    @Override
    protected ConversationPresenter createPresenter() {
        return new ConversationPresenter((MainActivity) getActivity());
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.fragment_conversation;
    }

    @Override
    public RecyclerView getRecentConversationView() {
        return this.recentConversationView;
    }

    @Override
    public void update(ObservableEvent event) {
        if (MessagingServiceEvent.Ready.equals(event.getName())) {
            if (!this.loaded.get()) {
                this.loaded.set(true);
                this.first.set(false);
                if (null != presenter) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            presenter.loadConversations();
                        }
                    });
                }
            }
        }
    }
}
