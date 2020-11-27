package cube.whiteboard.api.data;

import com.google.gson.annotations.SerializedName;

/**
 * 文件转换信息类
 *
 * @author LiuFeng
 * @data 2020/8/20 10:32
 */
public class FileConvert {
    @SerializedName("cid")
    public String roomId;
    public int fid;
    @SerializedName("index")
    public int page;
    public String fileName;
    public String originalUrl;
}
