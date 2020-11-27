package cube.api.data;

import com.google.gson.annotations.SerializedName;

/**
 * 服务器响应的结果数据
 *
 * @param <T>
 */
public class ResultData<T> extends BaseData {

    /**
     * 状态码
     */
    @SerializedName(value = "code", alternate = "state")
    public int code;

    /**
     * 描述
     */
    public String desc;

    /**
     * 操作结果
     */
    public boolean ok;

    /**
     * 数据
     */
    public T data;

    @Override
    public String toString() {
        return "ResultData{" + "code=" + code + ", desc='" + desc + '\'' + ", ok=" + ok + ", data=" + data + '}';
    }
}
