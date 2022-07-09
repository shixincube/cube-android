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

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import com.shixincube.app.AppConsts;
import com.shixincube.app.R;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.ui.adapter.CommonFragmentPagerAdapter;
import com.shixincube.app.ui.base.BaseActivity;
import com.shixincube.app.ui.base.BaseFragment;
import com.shixincube.app.ui.fragment.FragmentFactory;
import com.shixincube.app.ui.presenter.MainPresenter;
import com.shixincube.app.ui.view.MainView;
import com.shixincube.app.util.AvatarUtils;
import com.shixincube.app.util.PopupWindowUtils;
import com.shixincube.app.util.UIUtils;
import com.shixincube.app.widget.MainBottomMenu;
import com.shixincube.app.widget.MainTabBar;

import java.util.ArrayList;

import butterknife.BindView;
import cube.contact.model.Contact;
import cube.engine.CubeEngine;
import cube.engine.handler.ContactDataHandler;
import cube.engine.service.FloatingVideoWindowBinder;
import cube.engine.service.FloatingVideoWindowService;
import cube.engine.util.Future;
import cube.engine.util.Promise;
import cube.engine.util.PromiseFuture;
import cube.engine.util.PromiseHandler;
import cube.ferry.FerryEventListener;
import cube.ferry.handler.DefaultDetectHandler;
import cube.util.LogUtils;

/**
 * 主界面。
 */
public class MainActivity extends BaseActivity<MainView, MainPresenter> implements
        ViewPager.OnPageChangeListener, MainView, ContactDataHandler, FerryEventListener {

    private final static String TAG = "MainActivity";

    public final static int REQUEST_CREATE_GROUP_CONVERSATION = 1000;

    @BindView(R.id.ibPopupMenu)
    ImageButton popupMenuButton;

    @BindView(R.id.llState)
    LinearLayout stateBarLayout;

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

    @BindView(R.id.rlBottomMenu)
    protected RelativeLayout menuLayout;
    @BindView(R.id.svMenu)
    protected ScrollView menuScrollView;

    private MainTabBar mainTabBar;

    private MainBottomMenu mainBottomMenu;

    private ServiceConnection serviceConnection;

    public MainActivity() {
        super();
    }

    @Override
    public void init() {
        LogUtils.i(TAG, "#init");

        // 启动通话界面悬浮窗
        Intent cubeFloatingWindow = new Intent(this, FloatingVideoWindowService.class);
        cubeFloatingWindow.setAction(FloatingVideoWindowService.ACTION_PREPARE);
        startService(cubeFloatingWindow);

        this.serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                FloatingVideoWindowBinder binder = (FloatingVideoWindowBinder) iBinder;
                binder.setContactDataHandler(MainActivity.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        };
        Intent intent = new Intent(this, FloatingVideoWindowService.class);
        bindService(intent, this.serviceConnection, BIND_AUTO_CREATE);
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

        this.mainBottomMenu = new MainBottomMenu(this, this.menuScrollView);
    }

    @Override
    public void initListener() {
        this.popupMenuButton.setOnClickListener(this::onPopupMenuButtonClick);

        this.conversationLayout.setOnClickListener(this::onBottomTabBarClick);
        this.filesLayout.setOnClickListener(this::onBottomTabBarClick);
        this.contactsLayout.setOnClickListener(this::onBottomTabBarClick);
        this.profileLayout.setOnClickListener(this::onBottomTabBarClick);

        this.contentViewPager.addOnPageChangeListener(this);

        this.menuLayout.setOnClickListener((view) -> {
            hideBottomMenu();
        });
    }

    @Override
    public void initData() {
        Promise.create(new PromiseHandler<Boolean>() {
            @Override
            public void emit(PromiseFuture<Boolean> promise) {
                // 设置域
                AccountHelper.getInstance().setDomain(CubeEngine.getInstance().getConfig().domain);

                // 处理通知
                presenter.processNotice();

                boolean first = CubeEngine.getInstance().getContactService().isFirstSignIn();
                promise.resolve(first);
            }
        }).thenOnMainThread(new Future<Boolean>() {
            @Override
            public void come(Boolean data) {
                // 尝试加载 Demo 数据
                presenter.loadDemoData();

                if (data.booleanValue()) {
                    LogUtils.d(TAG, "First sign-in");
                    presenter.monitorConversation();
                    showWaitingDialog(UIUtils.getString(R.string.please_wait_a_moment));
                }
            }
        }).launch();

        if (AppConsts.FERRY_MODE) {
            CubeEngine.getInstance().getFerryService().detectDomain(new DefaultDetectHandler(true) {
                @Override
                public void handleResult(boolean online, long duration) {
                    LogUtils.d(TAG, "#detectDomain : "
                            + online + " - " + duration);

                    if (online) {
                        stateBarLayout.setVisibility(View.GONE);
                    }
                    else {
                        stateBarLayout.setVisibility(View.VISIBLE);

                        if (!CubeEngine.getInstance().getFerryService().isMembership()) {
                            // 非成员，退回到 FerryActivity
                            Intent intent = new Intent(MainActivity.this, FerryActivity.class);
                            intent.putExtra(FerryActivity.EXTRA_MEMBERSHIP, false);
                            jumpToActivity(intent);
                            finish();
                        }
                    }
                }
            });

            CubeEngine.getInstance().getFerryService().addEventListener(this);
        }
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

    /**
     * 获取底部滑动菜单。
     *
     * @return
     */
    public MainBottomMenu getBottomMenu() {
        return this.mainBottomMenu;
    }

    public void showBottomMenu(Object itemData) {
        this.menuLayout.setVisibility(View.VISIBLE);

        this.mainBottomMenu.setItemData(itemData);

        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 1,
                Animation.RELATIVE_TO_SELF, 0);
        animation.setDuration(200);
        this.menuScrollView.startAnimation(animation);
    }

    public void hideBottomMenu() {
        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 1);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                menuLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animation.setDuration(200);
        this.menuScrollView.startAnimation(animation);
    }

    private void onPopupMenuButtonClick(View view) {
        View menuView = View.inflate(this, R.layout.menu_main, null);

        int item = contentViewPager.getCurrentItem();
        switch (item) {
            case 0:
                menuView.findViewById(R.id.tvNewDir).setVisibility(View.GONE);
                break;
            case 1:
                menuView.findViewById(R.id.tvCreateGroup).setVisibility(View.GONE);
                menuView.findViewById(R.id.tvAddContact).setVisibility(View.GONE);
                break;
            case 2:
                menuView.findViewById(R.id.tvNewDir).setVisibility(View.GONE);
                menuView.findViewById(R.id.tvFileUpload).setVisibility(View.GONE);
                break;
            case 3:
                break;
            default:
                break;
        }

        PopupWindow popupWindow = PopupWindowUtils.getPopupWindowAtLocation(menuView, getWindow().getDecorView(),
                Gravity.TOP | Gravity.END,
                UIUtils.dp2px(5), appBar.getHeight() + UIUtils.dp2px(40));
        PopupWindowUtils.openOutsideTouchable(popupWindow);

        menuView.findViewById(R.id.tvCreateGroup).setOnClickListener((v) -> {
            popupWindow.dismiss();

            // 创建群聊
            Intent intent = new Intent(this, OperateContactActivity.class);
            startActivityForResult(intent, REQUEST_CREATE_GROUP_CONVERSATION);
        });

        menuView.findViewById(R.id.tvAddContact).setOnClickListener((v) -> {
            popupWindow.dismiss();

            // 添加联系人
            jumpToActivity(AddContactActivity.class);
        });

        menuView.findViewById(R.id.tvNewDir).setOnClickListener((v) -> {
            popupWindow.dismiss();

            // 新建文件夹
            jumpToActivity(NewDirectoryActivity.class);
        });

        menuView.findViewById(R.id.tvFileUpload).setOnClickListener((v) -> {
            popupWindow.dismiss();

            // 选取上传文件
            FragmentFactory.getInstance().getFilesFragment().pickFile();
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
    protected void onDestroy() {
        if (AppConsts.FERRY_MODE) {
            CubeEngine.getInstance().getFerryService().removeEventListener(this);
        }

        unbindService(this.serviceConnection);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CREATE_GROUP_CONVERSATION) {
            if (resultCode == RESULT_OK) {
                // 创建群组成功
            }
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
    public void onFerryOnline(String domainName) {
        stateBarLayout.setVisibility(View.GONE);
    }

    @Override
    public void onFerryOffline(String domainName) {
        stateBarLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public ImageButton getPopupMenuButton() {
        return this.popupMenuButton;
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

    @Override
    public int extractContactAvatarResourceId(Contact contact) {
        return AvatarUtils.getAvatarResource(contact);
    }
}
