package cube.whiteboard.api.data;

import java.util.List;

import cube.api.data.BaseData;

/**
 * 查询历史返回数据
 *
 * @author LiuFeng
 * @data 2020/8/18 19:03
 */
public class HistoryData extends BaseData {
    public List<Drawing> drawings;

        /*did: 白板数据id, long
        "roomId"：房间ID,string,
        data: string,白板详细数据信息,
        "createTime": long 创建时间,
        "updateTime": long 更新时间,
        "reset": 重置动作标记 1 为正常，2 为清楚动作,
        "from": 数据上报者 用户id,
        del: 1 为正常 大于1表示被清楚
        */
    public static class Drawing {
        public long did;
        public String roomId;
        public String data;
        public long createTime;
        public long updateTime;
        public int reset;
        public String from;
        public int del;
    }
}
