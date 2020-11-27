package cube.common;

import org.json.JSONObject;

/**
 * 可转 JSON 结构对象接口。
 */
public interface JSONable {

    JSONObject toJSON();
}
