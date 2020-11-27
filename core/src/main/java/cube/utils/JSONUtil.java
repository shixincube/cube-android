package cube.utils;

import android.os.Bundle;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Set;

import cube.utils.log.LogUtil;


/**
 * @author fldy
 */
public class JSONUtil {
    /**
     * 把 bundle 转换成 json 对象, 只取用 String, Boolean, Integer, Long, Double
     *
     * @param bundle
     *
     * @return
     *
     * @throws JSONException
     */
    public static JSONObject bundleToJSON(Bundle bundle) throws JSONException {
        JSONObject json = new JSONObject();
        if (bundle == null || bundle.isEmpty()) {
            return json;
        }
        Set<String> keySet = bundle.keySet();
        for (String key : keySet) {
            Object object = bundle.get(key);
            if (object instanceof String || object instanceof Boolean || object instanceof Integer || object instanceof Long || object instanceof Double) {
                json.put(key, object);
            }
        }
        return json;
    }

    /**
     * 把 bundle 转换成 json 字符串, 只取用 String, Boolean, Integer, Long, Double
     *
     * @param bundle
     *
     * @return
     *
     * @throws JSONException
     */
    public static String bundleToJSONString(Bundle bundle) throws JSONException {
        JSONObject json = bundleToJSON(bundle);
        return json.toString();
    }

    /**
     * 将josnArray转化为ArrayList 具体的josn到object的转换由Json2Object提供
     *
     * @param jsonArray
     * @param json2Object
     * @param <E>
     *
     * @return
     */
    public static <E> ArrayList<E> JsonArray2List(JSONArray jsonArray, Json2Object<E> json2Object) {
        //检查参数
        if (jsonArray == null || jsonArray.length() == 0 || json2Object == null) {
            return new ArrayList<>();
        }
        try {
            ArrayList<E> arrayList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                E realObj = json2Object.json2RealObj(jsonArray.getString(i));
                arrayList.add(realObj);
            }
            return arrayList;
        } catch (JSONException e1) {
            LogUtil.e(e1);
        }
        return new ArrayList<>();
    }

    public static abstract class Json2Object<E> {
        public abstract E json2RealObj(String json) throws JSONException;
    }
}
