package cube.api;

import cube.api.data.BaseData;
import cube.api.data.ResultData;

/**
 * api异常处理
 *
 * @author LiuFeng
 * @data 2020/2/10 11:14
 */
public class ApiException extends RuntimeException {

    private int      code;
    private String   desc;
    private Object data;

    public ApiException(ResultData resultData) {
        this.data = resultData.data;
        this.code = resultData.code;
        this.desc = resultData.desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return "ApiException{" + "code=" + code + ", desc='" + desc + '\'' + ", data=" + data + '}';
    }
}
