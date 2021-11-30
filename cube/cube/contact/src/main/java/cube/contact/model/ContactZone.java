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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import cube.contact.ContactService;
import cube.contact.handler.ContactZoneHandler;
import cube.core.handler.FailureHandler;
import cube.core.model.Entity;

/**
 * 联系人分区。
 */
public class ContactZone extends Entity {

    private final ContactService service;

    public final String name;

    private String displayName;

    private ContactZoneState state;

    private boolean peerMode = false;

    private List<ContactZoneParticipant> participants;

    private boolean ordered = false;

    public ContactZone(ContactService service, Long id, String name, String displayName,
                       boolean peerMode, long timestamp, ContactZoneState state) {
        super(id, timestamp);
        this.service = service;
        this.name = name;
        this.displayName = displayName;
        this.peerMode = peerMode;
        this.state = state;
        this.participants = new ArrayList<>();
    }

    public ContactZone(ContactService service, JSONObject json) throws JSONException {
        super(json);
        this.service = service;
        this.name = json.getString("name");
        this.displayName = json.getString("displayName");
        this.state = ContactZoneState.parse(json.getInt("state"));
        this.peerMode = json.getBoolean("peerMode");
        this.participants = new ArrayList<>();

        if (json.has("participants")) {
            JSONArray array = json.getJSONArray("participants");
            for (int i = 0, len = array.length(); i < len; ++i) {
                JSONObject data = array.getJSONObject(i);
                ContactZoneParticipant participant = new ContactZoneParticipant(data);
                this.participants.add(participant);
            }
        }
    }

    /**
     * 获取分区名称。
     *
     * @return 返回分区名称。
     */
    public String getName() {
        return this.name;
    }

    /**
     * 获取分区显示名。
     *
     * @return 返回分区显示名。
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * 获取分区状态。
     *
     * @return 返回分区状态。
     */
    public ContactZoneState getState() {
        return this.state;
    }

    /**
     * 返回已排序的参与人列表。
     *
     * @return 返回已排序的参与人列表。
     */
    public List<ContactZoneParticipant> getOrderedParticipants() {
        if (!this.ordered) {
            this.ordered = true;
            Collections.sort(this.participants, new NameComparator());
        }

        return this.participants;
    }

    /**
     * 返回参与人列表。
     *
     * @return 返回参与人列表。
     */
    public List<ContactZoneParticipant> getParticipants() {
        return this.participants;
    }

    /**
     * 获取参与人的联系人列表。
     *
     * @return 返回参与人的联系人列表。
     */
    public List<Contact> getParticipantContacts() {
        List<ContactZoneParticipant> participantList = this.getOrderedParticipants();
        ArrayList<Contact> list = new ArrayList<>();
        for (ContactZoneParticipant participant : participantList) {
            list.add(participant.getContact());
        }
        return list;
    }

    /**
     * 获取参与人的联系人列表。
     *
     * @param allowedState 指定参与人状态。
     * @return 返回包含指定状态的参与人对应的联系人列表。
     */
    public List<Contact> getParticipantContacts(ContactZoneParticipantState allowedState) {
        List<ContactZoneParticipant> participantList = this.getOrderedParticipants();
        ArrayList<Contact> list = new ArrayList<>();
        for (ContactZoneParticipant participant : participantList) {
            if (participant.getState() == allowedState) {
                list.add(participant.getContact());
            }
        }
        return list;
    }

    /**
     * 获取参与人列表。
     *
     * @param excludedState 指定排除的参与人状态。
     * @return 返回不包含排除状态的联系人列表。
     */
    public List<ContactZoneParticipant> getParticipantsByExcluding(ContactZoneParticipantState excludedState) {
        ArrayList<ContactZoneParticipant> list = new ArrayList<>();

        for (ContactZoneParticipant participant : this.participants) {
            if (participant.getState() != excludedState) {
                list.add(participant);
            }
        }

        if (list.isEmpty()) {
            return list;
        }

        // 时间倒序
        Collections.sort(list, new Comparator<ContactZoneParticipant>() {
            @Override
            public int compare(ContactZoneParticipant participant1, ContactZoneParticipant participant2) {
                return (int) (participant2.getTimestamp() - participant1.getTimestamp());
            }
        });

        return list;
    }

    /**
     * 是否是对等模式。
     *
     * @return 如果是对等模式返回 {@code true} 。
     */
    public boolean isPeerMode() {
        return this.peerMode;
    }

    /**
     * 非公开方法。
     *
     * @param participant
     */
    public void addParticipant(ContactZoneParticipant participant) {
        if (!this.participants.contains(participant)) {
            this.participants.add(participant);
            this.ordered = false;
        }
    }

    /**
     * 非公开方法。
     *
     * @param participant
     */
    public void removeParticipant(ContactZoneParticipant participant) {
        this.participants.remove(participant);
    }

    /**
     * 添加联系人。
     *
     * @param contact 指定联系人。
     * @param postscript
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void addParticipant(Contact contact, String postscript, ContactZoneHandler successHandler, FailureHandler failureHandler) {
        if (this.contains(contact)) {
            this.service.execute(failureHandler);
            return;
        }

        ContactZoneParticipant participant = new ContactZoneParticipant(contact.id, System.currentTimeMillis(),
                ContactZoneParticipantType.Contact, ContactZoneParticipantState.Pending,
                this.service.getSelf().id, postscript);

        this.service.addParticipantToZone(this, participant, successHandler, failureHandler);
    }

    /**
     * 移除参与人。
     *
     * @param contact 指定联系人。
     * @param successHandler 指定操作成功回调句柄。
     * @param failureHandler 指定操作失败回调句柄。
     */
    public void removeParticipant(Contact contact, ContactZoneHandler successHandler, FailureHandler failureHandler) {
        ContactZoneParticipant participant = this.getParticipant(contact);
        if (null == participant) {
            this.service.execute(failureHandler);
            return;
        }

        this.service.removeParticipantFromZone(this, participant, successHandler, failureHandler);
    }

    /**
     * 指定的联系人是否包含在该联系人分区里。
     *
     * @param contact 指定联系人。
     * @return 如果该分区有该联系人返回 {@code true} ，否则返回 {@code false} 。
     */
    public boolean contains(Contact contact) {
        for (ContactZoneParticipant participant : this.participants) {
            if (participant.getId().equals(contact.id)) {
                return true;
            }
        }

        return false;
    }

    private ContactZoneParticipant getParticipant(Contact contact) {
        for (ContactZoneParticipant participant : this.participants) {
            if (participant.id.longValue() == contact.id.longValue()) {
                return participant;
            }
        }

        return null;
    }

    /**
     * 名称拼写比较器。
     */
    protected class NameComparator implements Comparator<ContactZoneParticipant> {

        private Collator collator;

        public NameComparator() {
            this.collator = Collator.getInstance(Locale.CHINESE);
        }

        @Override
        public int compare(ContactZoneParticipant participant1, ContactZoneParticipant participant2) {
            if (null != participant1.getContact() && null != participant2.getContact()) {
                return this.collator.compare(participant1.getContact().getPriorityName(),
                        participant2.getContact().getPriorityName());
            }
            else if (null != participant1.getGroup() && null != participant2.getGroup()) {
                return this.collator.compare(participant1.getGroup().getName(),
                        participant2.getGroup().getName());
            }
            else {
                return 0;
            }
        }
    }
}
