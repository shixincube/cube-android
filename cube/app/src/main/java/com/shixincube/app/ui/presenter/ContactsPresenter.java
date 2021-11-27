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
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.shixincube.app.R;
import com.shixincube.app.model.Account;
import com.shixincube.app.ui.activity.ContactDetailsActivity;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.ui.fragment.ContactsFragment;
import com.shixincube.app.ui.view.ContactsView;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.HeaderAndFooterAdapter;
import com.shixincube.app.widget.adapter.OnItemClickListener;
import com.shixincube.app.widget.adapter.ViewHolder;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;

import java.util.ArrayList;
import java.util.List;

import cube.contact.ContactService;
import cube.contact.ContactZoneListener;
import cube.contact.model.Contact;
import cube.contact.model.ContactZone;
import cube.contact.model.ContactZoneParticipantState;
import cube.engine.CubeEngine;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;

/**
 * 联系人清单。
 */
public class ContactsPresenter extends BasePresenter<ContactsView> implements ContactZoneListener {

    /**
     * 正常状态的联系人列表。
     */
    private List<Contact> contacts;

    /**
     * 待处理的联系人列表。
     */
    private List<Contact> pendingContacts;

    private HeaderAndFooterAdapter adapter;

    public ContactsPresenter(BaseActivity activity) {
        super(activity);
        this.contacts = new ArrayList<>();
        this.pendingContacts = new ArrayList<>();
    }

    public void loadContacts() {
        this.setAdapter();

        // 从引擎获取默认的联系人分区
        Promise.create(new PromiseHandler<ContactZone>() {
            @Override
            public void emit(PromiseFuture<ContactZone> promise) {
                ContactZone contactZone = CubeEngine.getInstance().getContactService().getDefaultContactZone();

                if (null != contactZone) {
                    // 排序
                    contacts.clear();
                    pendingContacts.clear();

                    List<Contact> contactList = contactZone.getParticipantContacts(ContactZoneParticipantState.Normal);
                    for (Contact contact : contactList) {
                        Account.setNameSpelling(contact);
                        contacts.add(contact);
                    }

                    pendingContacts.addAll(contactZone.getParticipantContactsByExcluding(ContactZoneParticipantState.Normal));

                    promise.resolve(contactZone);
                }
                else {
                    promise.reject();
                }
            }
        }).thenOnMainThread(new Future<ContactZone>() {
            @Override
            public void come(ContactZone data) {
                if (null != getView().getFooterView()) {
                    getView().getFooterView().setText(UIUtils.getString(R.string.count_of_contacts, contacts.size()));
                }

                if (null != adapter) {
                    adapter.notifyDataSetChanged();
                }
            }
        }).catchRejectOnMainThread(new Future<ContactZone>() {
            @Override
            public void come(ContactZone data) {
                // 加载出错
            }
        }).launch();
    }

    private synchronized void setAdapter() {
        if (null == this.adapter) {
            AdapterForRecyclerView adapter = new AdapterForRecyclerView<Contact>(activity, this.contacts, R.layout.item_contact) {
                @Override
                public void convert(ViewHolderForRecyclerView helper, Contact item, int position) {
                    // 头像
                    ImageView avatarView = helper.getView(R.id.ivAvatar);
                    Glide.with(activity).load(AvatarUtils.getAvatarResource(item)).centerCrop().into(avatarView);

                    // 名称
                    helper.setText(R.id.tvName, item.getPriorityName());

                    // 判断是否显示索引字母
                    String indexLetter = "";
                    String currentLetter = String.valueOf(Account.getNameSpelling(item).charAt(0));
                    if (0 == position) {
                        indexLetter = currentLetter;
                    }
                    else {
                        // 获取上一个字母
                        String preLetter = String.valueOf(Account.getNameSpelling(contacts.get(position - 1)).charAt(0));
                        // 如果和上一个字母的首字母不同则显示字母栏
                        if (!preLetter.equalsIgnoreCase(currentLetter)) {
                            indexLetter = currentLetter;
                        }
                    }

                    // 判断是否分在一组
                    int nextIndex = position + 1;
                    if (nextIndex < contacts.size() - 1) {
                        // 得到下一个字母
                        String nextLetter = String.valueOf(Account.getNameSpelling(contacts.get(nextIndex)).charAt(0));
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

                    if (position == contacts.size() - 1) {
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

            adapter.addHeaderView(getView().getHeaderView());
            adapter.addFooterView(getView().getFooterView());
            this.adapter = adapter.getHeaderAndFooterAdapter();
            getView().getContactsView().setAdapter(this.adapter);

            // 设置事件监听
            ((AdapterForRecyclerView) this.adapter.getInnerAdapter()).setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(ViewHolder helper, ViewGroup parent, View itemView, int position) {
                    jumpToContactDetails(helper, parent, itemView, position);
                }
            });

            // 监听引擎层的更新分区事件
            CubeEngine.getInstance().getContactService().addContactZoneListener(this);
        }
    }

    private void jumpToContactDetails(ViewHolder helper, ViewGroup parent, View itemView, int position) {
        Intent intent = new Intent(activity, ContactDetailsActivity.class);
        // position 需要 -1 ，0 位置是 Header View
        Contact contact = contacts.get(position - 1);
        intent.putExtra("contactId", contact.getId());
        activity.startActivityForResult(intent, ContactsFragment.REQUEST_CONTACT_DETAILS);
    }

    @Override
    public void onContactZoneUpdated(ContactZone contactZone, ContactService service) {
        this.loadContacts();
    }
}
