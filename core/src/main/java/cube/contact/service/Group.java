package cube.contact.service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 群主类
 *
 * @author LiuFeng
 * @data 2020/9/2 16:24
 */
public class Group extends Contact {
    private List<Contact> members;

    public Group(Long id, String name) {
        super(id, name, null);
        this.members = new ArrayList<>();
    }

    public synchronized void addContact(Contact contact) {
        members.add(contact);
    }

    public synchronized void removeContact(Contact contact) {
        members.remove(contact);
    }

    public List<Contact> getMembers() {
        return new ArrayList<>(members);
    }

    @Override
    public synchronized JSONObject toJSON() {
        JSONObject jsonObject = super.toJSON();
        JSONArray array = new JSONArray();
        for (Contact contact : members) {
            array.put(contact.toJSON());
        }
        try {
            jsonObject.put("members", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
