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

package cube.contact.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import cube.contact.ContactService;
import cube.contact.handler.DefaultGroupHandler;
import cube.contact.handler.GroupAppendixHandler;
import cube.contact.handler.GroupHandler;
import cube.contact.handler.StableGroupAppendixHandler;
import cube.core.Module;
import cube.core.ModuleError;
import cube.core.handler.FailureHandler;
import cube.core.handler.StableFailureHandler;
import cube.core.model.Cacheable;
import cube.util.JSONable;

/**
 * 群组附录。
 */
public class GroupAppendix implements JSONable, Cacheable {

    private ContactService service;

    private Group group;

    /**
     * 群组公告。
     */
    private String notice;

    /**
     * 公告操作员 ID 。
     */
    private Long noticeOperatorId;

    /**
     * 公告操作员。
     */
    private Contact noticeOperator;

    /**
     * 公告时间。
     */
    private long noticeTime;

    /**
     * 成员对应的备注。
     */
    private Map<Long, String> memberRemarks;

    /**
     * 群备注。当前账号私有数据。
     */
    private String remark;

    /**
     * 是否追踪该群。
     */
    private boolean following;

    /**
     * 是否显示成员名。
     */
    private boolean memberNameDisplayed;

    /**
     * 群的上下文。当前账号私有数据。
     */
    private JSONObject context;

    private Long commId;

    public GroupAppendix(ContactService service, Group group, JSONObject json) throws JSONException {
        this.service = service;
        this.group = group;
        this.notice = json.getString("notice");
        this.noticeOperatorId = json.getLong("noticeOperatorId");
        this.noticeTime = json.getLong("noticeTime");

        this.memberRemarks = new HashMap<>();
        JSONArray array = json.getJSONArray("memberRemarks");
        for (int i = 0; i < array.length(); ++i) {
            JSONObject data = array.getJSONObject(i);
            Long id = data.getLong("id");
            String name = data.getString("name");
            this.memberRemarks.put(id, name);
        }

        this.remark = json.getString("remark");
        this.following = json.getBoolean("following");
        this.memberNameDisplayed = json.has("memberNameDisplayed") && json.getBoolean("memberNameDisplayed");

        if (json.has("context")) {
            this.context = json.getJSONObject("context");
        }

        this.commId = json.getLong("commId");
    }

    /**
     * 获取附录对应的群组。
     *
     * @return 返回附录对应的群组。
     */
    public Group getGroup() {
        return this.group;
    }

    /**
     * 是否有公告。
     *
     * @return 如果有公告返回 {@code true} 。
     */
    public boolean hasNotice() {
        return this.notice.length() > 0;
    }

    /**
     * 获取公告文本。
     *
     * @return 返回公告文本。
     */
    public String getNotice() {
        return this.notice;
    }

    /**
     * 获取公告操作员 ID 。
     *
     * @return 返回公告操作员 ID 。
     */
    public Long getNoticeOperatorId() {
        return this.noticeOperatorId;
    }

    /**
     * 获取公告操作员。
     *
     * @return 返回公告操作员。
     */
    public Contact getNoticeOperator() {
        return this.noticeOperator;
    }

    /**
     * 获取公告的更新时间。
     *
     * @return 返回公告的更新时间戳。
     */
    public long getNoticeTime() {
        return this.noticeTime;
    }

    /**
     * 是否有备注。
     *
     * @return 如果有备注信息返回 {@code true} 。
     */
    public boolean hasRemark() {
        return (null != this.remark && this.remark.length() > 0);
    }

    /**
     * 获取备注信息。
     *
     * @return 返回备注信息。
     */
    public String getRemark() {
        return this.remark;
    }

    /**
     * 是否显示群组成员名。
     *
     * @return 如果显示成员名称返回 {@code true} ，否则返回 {@code false} 。
     */
    public boolean isDisplayMemberName() {
        return this.memberNameDisplayed;
    }

    /**
     * 修改群组公告。
     *
     * @param notice 指定新公告内容。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void modifyNotice(String notice, GroupHandler successHandler, FailureHandler failureHandler) {
        if (!this.group.isOwner()) {
            // 不是群主，不允许修改公告
            this.service.execute(failureHandler);
            return;
        }

        // 修改公告
        this.notice = notice;

        JSONObject params = new JSONObject();
        try {
            params.put("notice", notice);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // 更新附录
        this.service.updateAppendix(this, params, successHandler, failureHandler);
    }

    /**
     * 修改群组的备注。
     *
     * @param remark 指定对该群组的备注。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void modifyRemark(String remark, GroupHandler successHandler, FailureHandler failureHandler) {
        this.remark = remark;

        JSONObject params = new JSONObject();
        try {
            params.put("remark", remark);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // 更新附录
        this.service.updateAppendix(this, params, successHandler, failureHandler);
    }

    /**
     * 修改是否显示群成员昵称标志位。
     *
     * @param display 指定是否显示成员名称。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void modifyDisplayNameFlag(boolean display, GroupHandler successHandler, FailureHandler failureHandler) {
        this.memberNameDisplayed = display;

        JSONObject params = new JSONObject();
        try {
            params.put("memberNameDisplayed", display);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // 更新附录
        this.service.updateAppendix(this, params, successHandler, failureHandler);
    }

    /**
     * <b>Non-public API</b>
     *
     * @param contact
     */
    public void setNoticeOperator(Contact contact) {
        this.noticeOperator = contact;
    }

    /**
     * 获取群组的通讯 ID 。
     *
     * @return 返回群组的通讯 ID 。
     */
    public Long getCommId() {
        Object mutex = new Object();

        this.service.getGroupAppendixCommId(this, new StableGroupAppendixHandler() {
            @Override
            public void handleAppendix(Group group, GroupAppendix appendix) {
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        }, new StableFailureHandler() {
            @Override
            public void handleFailure(Module module, ModuleError error) {
                synchronized (mutex) {
                    mutex.notify();
                }
            }
        });

        synchronized (mutex) {
            try {
                mutex.wait(10 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return this.commId;
    }

    /**
     * <b>Non-public API</b>
     *
     * @param commId
     */
    public void setCommId(Long commId) {
        this.commId = commId;
    }

    /**
     * <b>Non-public API</b>
     *
     * @param id
     * @param successHandler
     * @param failureHandler
     */
    public void updateCommId(Long id, GroupAppendixHandler successHandler, FailureHandler failureHandler) {
        this.commId = id;

        JSONObject params = new JSONObject();
        try {
            params.put("commId", this.commId.longValue());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // 更新附录
        this.service.updateAppendix(this, params, new DefaultGroupHandler(successHandler.isInMainThread()) {
            @Override
            public void handleGroup(Group group) {
                successHandler.handleAppendix(group, GroupAppendix.this);
            }
        }, failureHandler);
    }

    @Override
    public int getMemorySize() {
        int size = 8 * 9 + 2;
        size += this.notice.getBytes(StandardCharsets.UTF_8).length;
        if (null != this.noticeOperator) {
            size += this.noticeOperator.getMemorySize();
        }

        for (Map.Entry<Long, String> entry : this.memberRemarks.entrySet()) {
            size += 16 + entry.getValue().getBytes(StandardCharsets.UTF_8).length;
        }

        size += this.remark.getBytes(StandardCharsets.UTF_8).length;

        if (null != this.context) {
            size += this.context.toString().getBytes(StandardCharsets.UTF_8).length;
        }

        return size;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("groupId", this.group.getId());
            json.put("notice", this.notice);
            json.put("noticeOperatorId", this.noticeOperatorId.longValue());
            json.put("noticeTime", this.noticeTime);

            JSONArray array = new JSONArray();
            for (Map.Entry<Long, String> entry : this.memberRemarks.entrySet()) {
                JSONObject data = new JSONObject();
                data.put("id", entry.getKey().longValue());
                data.put("name", entry.getValue());
                array.put(data);
            }
            json.put("memberRemarks", array);

            json.put("remark", this.remark);
            json.put("following", this.following);
            json.put("memberNameDisplayed", this.memberNameDisplayed);

            if (null != this.context) {
                json.put("context", this.context);
            }

            json.put("commId", this.commId.longValue());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
