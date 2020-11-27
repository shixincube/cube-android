package cube.auth.service;

import org.json.JSONObject;

import java.util.Date;

import cube.common.JSONable;
import cube.utils.GsonUtil;

/**
 * 授权访问令牌
 *
 * @author LiuFeng
 * @data 2020/9/1 14:04
 */
public class AuthToken implements JSONable {
    public String code;     // 令牌编码
    public String domain;   // 令牌对应的域。
    public String appKey;   // 令牌指定的 App Key 信息。
    public long cid;        // 令牌绑定的 Contact ID 。
    public long issues;     // 发布时间。
    public long expiry;     // 过期时间。
    public PrimaryDescription description; // 主描述信息。


    /**
     * 令牌有效
     *
     * @return
     */
    public boolean isValid() {
        return (new Date().getTime() < this.expiry);
    }

    @Override
    public JSONObject toJSON() {
        return GsonUtil.toJSONObject(this);
    }

    public static AuthToken create(JSONObject json) {
        return GsonUtil.toBean(json, AuthToken.class);
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }
}
