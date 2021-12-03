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

package com.shixincube.app.ui.activity;

import android.content.Intent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.shixincube.app.R;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BasePresenter;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.adapter.AdapterForRecyclerView;
import com.shixincube.app.widget.adapter.ViewHolderForRecyclerView;
import com.shixincube.app.widget.recyclerview.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import cube.contact.model.Contact;
import cube.contact.model.Group;
import cube.engine.CubeEngine;

/**
 * 移除群组成员。
 */
public class RemoveGroupMemberActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener, View.OnClickListener{

    @BindView(R.id.rvMembers)
    RecyclerView memberView;

    private Group group;

    private List<Contact> members = new ArrayList<>();

    private List<Long> selectedMembers = new ArrayList<>();

    private AdapterForRecyclerView<Contact> adapter;

    public RemoveGroupMemberActivity() {
        super();
    }

    @Override
    public void initView() {
        setToolbarTitle(UIUtils.getString(R.string.remove_member));
        this.toolbarFuncButton.setText(UIUtils.getString(R.string.complete));
        this.toolbarFuncButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void initListener() {
        this.toolbarFuncButton.setOnClickListener((view) -> {
            if (!selectedMembers.isEmpty()) {
                showMaterialDialog(UIUtils.getString(R.string.remove_member),
                        UIUtils.getString(R.string.are_you_sure_to_remove_members, selectedMembers.size()),
                        UIUtils.getString(R.string.confirm),
                        UIUtils.getString(R.string.cancel),
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                hideMaterialDialog();

                                long[] idList = new long[selectedMembers.size()];
                                for (int i = 0; i < idList.length; ++i) {
                                    idList[i] = selectedMembers.get(i);
                                }

                                Intent data = new Intent();
                                data.putExtra("members", idList);
                                setResult(RESULT_OK, data);
                                finish();
                            }
                        }, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                hideMaterialDialog();
                            }
                        });
            }
            else {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    public void initData() {
        long groupId = getIntent().getLongExtra("groupId", 0);
        this.group = CubeEngine.getInstance().getContactService().getGroup(groupId);

        this.members.clear();
        this.members.addAll(this.group.getMemberListWithoutOwner());

        setAdapter();
    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_remove_group_member;
    }

    private void setAdapter() {
        if (null == this.adapter) {
            this.adapter = new AdapterForRecyclerView<Contact>(this, this.members, R.layout.item_member) {
                @Override
                public void convert(ViewHolderForRecyclerView helper, Contact item, int position) {
                    ImageView avatarView = (ImageView) helper.getView(R.id.ivAvatar);
                    AvatarUtils.fillContactAvatar(RemoveGroupMemberActivity.this, item, avatarView);

                    helper.setText(R.id.tvName, item.getPriorityName());

                    CheckBox checkBox = helper.getView(R.id.cbSelector);
                    checkBox.setTag(item.getId());
                    checkBox.setOnCheckedChangeListener(RemoveGroupMemberActivity.this);

                    ImageButton button = helper.getView(R.id.btnDetails);
                    button.setTag(item.getId());
                    button.setOnClickListener(RemoveGroupMemberActivity.this);
                }
            };

            this.memberView.setAdapter(this.adapter);
        }
        else {
            this.adapter.notifyDataSetChangedWrapper();
        }
    }

    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        Long contactId = (Long) compoundButton.getTag();
        if (checked) {
            if (!this.selectedMembers.contains(contactId)) {
                this.selectedMembers.add(contactId);
            }
        }
        else {
            this.selectedMembers.remove(contactId);
        }
    }

    @Override
    public void onClick(View view) {
        Long contactId = (Long) view.getTag();
        Intent intent = new Intent(this, ContactDetailsActivity.class);
        intent.putExtra("contactId", contactId.longValue());
        intent.putExtra("readOnly", true);
        jumpToActivity(intent);
    }
}
