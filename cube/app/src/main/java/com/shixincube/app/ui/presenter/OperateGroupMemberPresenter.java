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

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.shixincube.app.R;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.model.Account;
import com.shixincube.app.ui.activity.OperateGroupMemberMemberActivity;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.view.OperateGroupMemberView;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.HeaderAndFooterAdapter;
import com.shixincube.app.widget.adapter.ViewHolder;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;

import java.util.ArrayList;
import java.util.List;

import cube.contact.handler.DefaultGroupHandler;
import cube.contact.model.Contact;
import cube.contact.model.ContactZone;
import cube.contact.model.Group;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.DefaultFailureHandler;
import cube.engine.CubeEngine;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;

/**
 * 创建群组。
 */
public class OperateGroupMemberPresenter extends BasePresenter<OperateGroupMemberView> {

    private boolean createMode;

    private List<Contact> allContacts;
    private List<Contact> selectedMembers;

    private HeaderAndFooterAdapter contactsAdapter;
    private AdapterForRecyclerView<Contact> selectedAdapter;

    public OperateGroupMemberPresenter(BaseActivity activity, List<Contact> selectedMembers, boolean createMode) {
        super(activity);
        this.createMode = createMode;
        this.allContacts = new ArrayList<>();
        this.selectedMembers = selectedMembers;
    }

    public void load() {
        this.setAdapter();
        this.loadData();
    }

    public void createGroupAndJump() {
        this.activity.showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));

        CubeEngine.getInstance().getContactService().createGroup(this.selectedMembers, new DefaultGroupHandler(true) {
            @Override
            public void handleGroup(Group group) {

            }
        }, new DefaultFailureHandler(true) {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                UIUtils.showToast(UIUtils.getString(R.string.create_group_failure));
            }
        });
    }

    private void loadData() {
        Promise.create(new PromiseHandler<List<Contact>>() {
            @Override
            public void emit(PromiseFuture<List<Contact>> promise) {
                ContactZone contactZone = CubeEngine.getInstance().getContactService().getDefaultContactZone();
                allContacts.clear();
                allContacts.addAll(contactZone.getParticipantContacts());
                promise.resolve(allContacts);
            }
        }).thenOnMainThread(new Future<List<Contact>>() {
            @Override
            public void come(List<Contact> data) {
                if (null != contactsAdapter) {
                    contactsAdapter.notifyDataSetChanged();
                }
            }
        }).launch();
    }

    private void setAdapter() {
        if (null == this.contactsAdapter) {
            AdapterForRecyclerView adapter = new AdapterForRecyclerView<Contact>(activity, this.allContacts, R.layout.item_contact) {
                @Override
                public void convert(ViewHolderForRecyclerView helper, Contact item, int position) {
                    // 头像
                    Glide.with(activity)
                            .load(AccountHelper.getAvatarResource(item))
                            .centerCrop()
                            .into((ImageView) helper.getView(R.id.ivAvatar));
                    // 名字
                    helper.setText(R.id.tvName, item.getPriorityName());

                    // 是否可选，已经选择的标记为不可选
                    CheckBox selector = helper.getView(R.id.cbSelector);
                    selector.setVisibility(View.VISIBLE);
                    Group group = ((OperateGroupMemberMemberActivity) activity).group;
                    if (null != group && group.isMember(item)) {
                        selector.setChecked(true);
                        selector.setEnabled(false);
                        helper.setEnabled(R.id.llRoot, false);
                    }
                    else {
                        selector.setChecked(selectedMembers.contains(item));
                        selector.setEnabled(true);
                        helper.setEnabled(R.id.llRoot, true);
                    }

                    // 判断是否显示索引字母
                    String indexLetter = "";
                    String currentLetter = String.valueOf(Account.getNameSpelling(item).charAt(0));
                    if (0 == position) {
                        indexLetter = currentLetter;
                    }
                    else {
                        // 获取上一个字母
                        String preLetter = String.valueOf(Account.getNameSpelling(allContacts.get(position - 1)).charAt(0));
                        // 如果和上一个字母的首字母不同则显示字母栏
                        if (!preLetter.equalsIgnoreCase(currentLetter)) {
                            indexLetter = currentLetter;
                        }
                    }

                    // 判断是否分在一组
                    int nextIndex = position + 1;
                    if (nextIndex < allContacts.size() - 1) {
                        // 得到下一个字母
                        String nextLetter = String.valueOf(Account.getNameSpelling(allContacts.get(nextIndex)).charAt(0));
                        // 如果和下一个字母的首字母不同则隐藏下划线
                        if (!nextLetter.equalsIgnoreCase(currentLetter)) {
                            helper.setViewVisibility(R.id.vLine, View.INVISIBLE);
                        }
                        else {
                            helper.setViewVisibility(R.id.vLine, View.VISIBLE);
                        }
                    }
                    else {
                        helper.setViewVisibility(R.id.vLine, View.INVISIBLE);
                    }

                    if (position == allContacts.size() - 1) {
                        helper.setViewVisibility(R.id.vLine, View.GONE);
                    }

                    // 判断索引字母栏是否显示
                    if (TextUtils.isEmpty(indexLetter)) {
                        helper.setViewVisibility(R.id.tvIndex, View.GONE);
                    }
                    else {
                        helper.setText(R.id.tvIndex, indexLetter);
                        helper.setViewVisibility(R.id.tvIndex, View.VISIBLE);
                    }
                }
            };

            // 设置适配器
            adapter.addHeaderView(getView().getHeaderView());
            this.contactsAdapter = adapter.getHeaderAndFooterAdapter();
            getView().getAllContactsView().setAdapter(this.contactsAdapter);

            // 事件
            adapter.setOnItemClickListener(this::onClickContact);
        }

        if (null == this.selectedAdapter) {
            this.selectedAdapter = new AdapterForRecyclerView<Contact>(activity, this.selectedMembers, R.layout.item_member_selected) {
                @Override
                public void convert(ViewHolderForRecyclerView helper, Contact item, int position) {
                    Glide.with(activity)
                            .load(AccountHelper.getAvatarResource(item))
                            .centerCrop()
                            .into((ImageView) helper.getView(R.id.aivAvatar));
                }
            };
            getView().getSelectedContactsView().setAdapter(this.selectedAdapter);
        }
    }

    private void onClickContact(ViewHolder helper, ViewGroup parent, View itemView, int position) {
        // 位置要 -1 ，因为有 Header View
        Contact contact = this.allContacts.get(position - 1);

        // 反选
        if (this.selectedMembers.contains(contact)) {
            this.selectedMembers.remove(contact);
        }
        else {
            this.selectedMembers.add(contact);
        }

        this.selectedAdapter.notifyDataSetChangedWrapper();
        this.contactsAdapter.notifyDataSetChanged();

        // 创建模式下至少需要 2 个联系人
        if (this.selectedMembers.size() >= (this.createMode ? 2 : 1)) {
            getView().getToolbarFunctionButton().setText(UIUtils.getString(R.string.complete_with_count, this.selectedMembers.size()));
            getView().getToolbarFunctionButton().setEnabled(true);
        }
        else {
            getView().getToolbarFunctionButton().setText(UIUtils.getString(R.string.complete));
            getView().getToolbarFunctionButton().setEnabled(false);
        }
    }
}
