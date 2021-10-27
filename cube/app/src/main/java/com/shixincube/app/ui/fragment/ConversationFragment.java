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

import com.shixincube.app.R;
import com.shixincube.app.ui.activity.MainActivity;
import com.shixincube.app.ui.base.BaseFragment;
import com.shixincube.app.ui.presenter.ConversationPresenter;
import com.shixincube.app.ui.view.ConversationView;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import butterknife.BindView;
import cube.engine.CubeEngine;

/**
 * 最近消息会话界面。
 */
public class ConversationFragment extends BaseFragment<ConversationView, ConversationPresenter> implements ConversationView {

    @BindView(R.id.rvConversations)
    RecyclerView recentConversationView;

    private boolean first = true;

    public ConversationFragment() {
        super();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!this.first) {
            this.presenter.loadConversations();
        }
    }

    @Override
    public void initData() {
        (new Thread() {
            @Override
            public void run() {
                int count = 100;
                while (!CubeEngine.getInstance().getMessagingService().isReady()) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    --count;
                    if (count <= 0) {
                        break;
                    }
                }

                presenter.loadConversations();
                first = false;
            }
        }).start();
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
}
