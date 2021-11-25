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

import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import com.shixincube.app.R;
import com.shixincube.app.ui.adapter.CommonFragmentPagerAdapter;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BaseFragment;
import com.shixincube.app.ui.fragment.FragmentFactory;
import com.shixincube.app.ui.presenter.MainPresenter;
import com.shixincube.app.ui.view.MainView;
import com.shixincube.app.util.PopupWindowUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.MainTabBar;

import java.util.ArrayList;

import butterknife.BindView;

/**
 * 主界面。
 */
public class MainActivity extends BaseActivity<MainView, MainPresenter> implements ViewPager.OnPageChangeListener, MainView {

    private MainTabBar mainTabBar;

    @BindView(R.id.ibPopupMenu)
    ImageButton popupMenuButton;

    @BindView(R.id.vpContent)
    ViewPager contentViewPager;

    @BindView(R.id.llConversation)
    LinearLayout conversationLayout;
    @BindView(R.id.tvConversationNormal)
    TextView conversationNormal;
    @BindView(R.id.tvConversationPressed)
    TextView conversationPressed;
    @BindView(R.id.tvConversationTextNormal)
    TextView conversationTextNormal;
    @BindView(R.id.tvConversationTextPressed)
    TextView conversationTextPressed;
    @BindView(R.id.tvConversationBadge)
    TextView conversationBadge;

    @BindView(R.id.llFiles)
    LinearLayout filesLayout;
    @BindView(R.id.tvFilesNormal)
    TextView filesNormal;
    @BindView(R.id.tvFilesPressed)
    TextView filesPressed;
    @BindView(R.id.tvFilesTextNormal)
    TextView filesTextNormal;
    @BindView(R.id.tvFilesTextPressed)
    TextView filesTextPressed;

    @BindView(R.id.llContacts)
    LinearLayout contactsLayout;
    @BindView(R.id.tvContactsNormal)
    TextView contactsNormal;
    @BindView(R.id.tvContactsPressed)
    TextView contactsPressed;
    @BindView(R.id.tvContactsTextNormal)
    TextView contactsTextNormal;
    @BindView(R.id.tvContactsTextPressed)
    TextView contactsTextPressed;

    @BindView(R.id.llProfile)
    LinearLayout profileLayout;
    @BindView(R.id.tvProfileNormal)
    TextView profileNormal;
    @BindView(R.id.tvProfilePressed)
    TextView profilePressed;
    @BindView(R.id.tvProfileTextNormal)
    TextView profileTextNormal;
    @BindView(R.id.tvProfileTextPressed)
    TextView profileTextPressed;

    public MainActivity() {
        super();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void init() {

    }

    @Override
    public void initView() {
        this.mainTabBar = new MainTabBar(this.presenter.getView());
        this.mainTabBar.toggleToConversation();

        this.popupMenuButton.setVisibility(View.VISIBLE);

        // 设置缓存
        this.contentViewPager.setOffscreenPageLimit(3);

        ArrayList<BaseFragment> fragmentList = new ArrayList<>(4);
        fragmentList.add(FragmentFactory.getInstance().getConversationFragment());
        fragmentList.add(FragmentFactory.getInstance().getFilesFragment());
        fragmentList.add(FragmentFactory.getInstance().getContactsFragment());
        fragmentList.add(FragmentFactory.getInstance().getProfileFragment());
        this.contentViewPager.setAdapter(new CommonFragmentPagerAdapter(getSupportFragmentManager(), fragmentList));
    }

    @Override
    public void initListener() {
        this.popupMenuButton.setOnClickListener(this::onPopupMenuButtonClick);

        this.conversationLayout.setOnClickListener(this::onBottomTabBarClick);
        this.filesLayout.setOnClickListener(this::onBottomTabBarClick);
        this.contactsLayout.setOnClickListener(this::onBottomTabBarClick);
        this.profileLayout.setOnClickListener(this::onBottomTabBarClick);

        this.contentViewPager.addOnPageChangeListener(this);
    }

    @Override
    public void initData() {
    }

    @Override
    protected boolean isToolbarCanBack() {
        return false;
    }

    @Override
    protected MainPresenter createPresenter() {
        return new MainPresenter(this);
    }

    @Override
    protected int provideContentViewId() {
        return R.layout.activity_main;
    }

    private void onPopupMenuButtonClick(View view) {
        View menuView = View.inflate(this, R.layout.menu_main, null);
        PopupWindow popupWindow = PopupWindowUtils.getPopupWindowAtLocation(menuView, getWindow().getDecorView(),
                Gravity.TOP | Gravity.END,
                UIUtils.dp2px(5), appBar.getHeight() + 30);
        menuView.findViewById(R.id.tvCreateGroup).setOnClickListener((v) -> {
            popupWindow.dismiss();
        });
        menuView.findViewById(R.id.tvAddContact).setOnClickListener((v) -> {
            popupWindow.dismiss();
        });
    }

    private void onBottomTabBarClick(View view) {
        switch (view.getId()) {
            case R.id.llConversation:
                this.contentViewPager.setCurrentItem(0, false);
                this.mainTabBar.toggleToConversation();
                break;
            case R.id.llFiles:
                this.contentViewPager.setCurrentItem(1, false);
                this.mainTabBar.toggleToFiles();
                break;
            case R.id.llContacts:
                this.contentViewPager.setCurrentItem(2, false);
                this.mainTabBar.toggleToContacts();
                break;
            case R.id.llProfile:
                this.contentViewPager.setCurrentItem(3, false);
                this.mainTabBar.toggleToProfile();
                break;
            default:
                break;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void setToolbarTitle(String title) {
        super.setToolbarTitle(title);
    }

    @Override
    public TextView getConversationNormalIcon() {
        return this.conversationNormal;
    }
    @Override
    public TextView getConversationNormalTitle() {
        return this.conversationTextNormal;
    }
    @Override
    public TextView getConversationPressedIcon() {
        return this.conversationPressed;
    }
    @Override
    public TextView getConversationPressedTitle() {
        return this.conversationTextPressed;
    }

    @Override
    public TextView getFilesNormalIcon() {
        return this.filesNormal;
    }
    @Override
    public TextView getFilesNormalTitle() {
        return this.filesTextNormal;
    }
    @Override
    public TextView getFilesPressedIcon() {
        return this.filesPressed;
    }
    @Override
    public TextView getFilesPressedTitle() {
        return this.filesTextPressed;
    }

    @Override
    public TextView getContactsNormalIcon() {
        return this.contactsNormal;
    }
    @Override
    public TextView getContactsNormalTitle() {
        return this.contactsTextNormal;
    }
    @Override
    public TextView getContactsPressedIcon() {
        return this.contactsPressed;
    }
    @Override
    public TextView getContactsPressedTitle() {
        return this.contactsTextPressed;
    }

    @Override
    public TextView getProfileNormalIcon() {
        return this.profileNormal;
    }
    @Override
    public TextView getProfileNormalTitle() {
        return this.profileTextNormal;
    }
    @Override
    public TextView getProfilePressedIcon() {
        return this.profilePressed;
    }
    @Override
    public TextView getProfilePressedTitle() {
        return this.profileTextPressed;
    }
}
