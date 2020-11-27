package cube.whiteboard.api.data;

import com.google.gson.annotations.SerializedName;

/**
 * 文件页数据
 *
 * @author LiuFeng
 * @data 2020/8/20 10:24
 */
public class FilePage {
    @SerializedName("cid")
    public String roomId;
    public String fid;
    public int total;
}
