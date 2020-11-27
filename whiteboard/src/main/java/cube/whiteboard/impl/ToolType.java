package cube.whiteboard.impl;

/**
 * 白板工具类型
 *
 * @author LiuFeng
 * @data 2020/8/21 10:40
 */
public enum ToolType {

    /**
     * 清除
     */
    CLEAR(0),

    /**
     * 撤销
     */
    UNDO(1),

    /**
     * 重做
     */
    REDO(2),

    /**
     * 马赛克
     */
    MOSAIC(8),

    /**
     * 图片
     */
    IMAGE(10);


    public int type;

    ToolType(int type) {
        this.type = type;
    }
}

