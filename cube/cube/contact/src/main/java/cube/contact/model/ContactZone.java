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

import cube.core.model.Entity;

/**
 * 联系人分区。
 */
public class ContactZone extends Entity {

    public final String name;

    private String displayName;

    private ContactZoneState state;

    private List<ContactZoneParticipant> participants;

    private boolean ordered = false;

    public ContactZone(Long id, String name, String displayName, long timestamp, ContactZoneState state) {
        super(id, timestamp);
        this.name = name;
        this.displayName = displayName;
        this.state = state;
        this.participants = new ArrayList<>();
    }

    public ContactZone(JSONObject json) throws JSONException {
        super(json);
        this.name = json.getString("name");
        this.displayName = json.getString("displayName");
        this.state = ContactZoneState.parse(json.getInt("state"));
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

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public ContactZoneState getState() {
        return this.state;
    }

    public List<ContactZoneParticipant> getOrderedParticipants() {
        if (!this.ordered) {
            this.ordered = true;
            Collections.sort(this.participants, new PinYinComparator());
        }

        return this.participants;
    }

    public List<ContactZoneParticipant> getParticipants() {
        return this.participants;
    }

    /**
     * 获取参与人的联系人列表。
     *
     * @return 返回参与人的联系人列表。
     */
    public List<Contact> getParticipantContacts() {
        List<ContactZoneParticipant> czpList = this.getOrderedParticipants();
        ArrayList<Contact> list = new ArrayList<>();
        for (ContactZoneParticipant czp : czpList) {
            list.add(czp.getContact());
        }
        return list;
    }

    public void addParticipant(ContactZoneParticipant participant) {
        this.participants.add(participant);
        this.ordered = false;
    }

    public void removeParticipant(ContactZoneParticipant participant) {
        this.participants.remove(participant);
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

    /**
     * @private
     * @param contact
     */
    public synchronized void matchContact(Contact contact) {
        long id = contact.id;
        for (ContactZoneParticipant participant : this.participants) {
            if (participant.getId().longValue() == id) {
                participant.setContact(contact);
                break;
            }
        }
    }

    protected class PinYinComparator implements Comparator<ContactZoneParticipant> {

        private Collator collator;

        public PinYinComparator() {
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
