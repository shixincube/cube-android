package cube.whiteboard.impl;

import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cube.utils.SpUtil;
import cube.whiteboard.api.data.FileConvert;

/**
 * 白板文件
 *
 * @author LiuFeng
 * @data 2020/8/18 17:31
 */
public class WhiteboardFile {
    public File file;

    @SerializedName("cid")
    public String roomId;
    public String fid;
    public int convert;
    public String from;
    public String fName;
    public int fSize;
    public String fUrl;
    public long createTime;
    public long updateTime;
    public int pageTotal;

    private Map<Integer, FileConvert> fileConvertMap = new ConcurrentHashMap<>();

    public void addFileConvert(int page, FileConvert convert) {
        fileConvertMap.put(page, convert);
    }

    public FileConvert getFileConvert(int page) {
        return fileConvertMap.get(page);
    }

    @Override
    public String toString() {
        JSONObject ret = new JSONObject();
        try {
            ret.put("account", SpUtil.getCubeId());
            ret.put("origin", fName);
            ret.put("alias", fName);
            ret.put("url", fUrl);
            ret.put("size", fSize);

            JSONArray array = new JSONArray();
            if (!fileConvertMap.isEmpty()) {
                for (FileConvert url : fileConvertMap.values()) {
                    array.put(url.originalUrl);
                }
            }
            ret.put("urls", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret.toString();
    }
}
