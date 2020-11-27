package cube.contact.service;

import android.os.Build;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 用户描述自己的账号。
 *
 * @author LiuFeng
 * @data 2020/9/1 18:15
 */
public class Self extends Contact {
    private Device device;

    /**
     * 当前终端的域。
     */
    public static String DOMAIN = "shixincube.com";

    public Self(Long id, String name) {
        super(id, name, DOMAIN);
        String deviceName = Build.MANUFACTURER + "-" + Build.MODEL;
        String platform = TextUtils.isEmpty(Build.MODEL) && Build.MODEL.contains("TV") ? "AndroidTV" : "Android";
        device = new Device(deviceName, platform);
        addDevice(device);
    }

    public Device getDevice() {
        return device;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("device", device.toJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
