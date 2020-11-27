package cube.whiteboard.api.data;

import java.util.List;

import cube.api.data.BaseData;
import cube.whiteboard.impl.WhiteboardFile;

/**
 * 查询文件返回数据
 *
 * @author LiuFeng
 * @data 2020/8/18 17:35
 */
public class QueryFileData extends BaseData {
    public List<WhiteboardFile> files;
}
