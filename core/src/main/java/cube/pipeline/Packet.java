package cube.pipeline;

import org.json.JSONException;
import org.json.JSONObject;

import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cube.common.JSONable;
import cube.core.Service;

/**
 * 来自客户端数据通道的包结构。
 */
public class Packet implements JSONable {

    /**
     * 封包的唯一序号。
     */
    public Long sn;

    /**
     * 封包名称。
     */
    public String name;

    /**
     * 封包的负载数据。
     */
    public JSONObject data;

    public Packet(ActionDialect dialect) {
        this.name = dialect.getName();
        this.sn = dialect.getParamAsLong(Service.SN);
        this.data = dialect.getParamAsJson(Service.DATA);
    }

    public Packet(String name, JSONObject data) {
        this(Utils.generateSerialNumber(), name, data);
    }

    public Packet(Long sn, String name, JSONObject data) {
        this.sn = sn;
        this.name = name;
        this.data = data;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put(Service.SN, this.sn);
            json.put("name", this.name);
            json.put(Service.DATA, this.data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Packet 转 ActionDialect
     *
     * @return
     */
    public ActionDialect toDialect(String tokenCode) {
        ActionDialect result = new ActionDialect(this.name);
        result.addParam(Service.SN, this.sn);
        result.addParam(Service.DATA, this.data);
        if (tokenCode != null) {
            result.addParam("token", tokenCode);
        }
        return result;
    }
}
