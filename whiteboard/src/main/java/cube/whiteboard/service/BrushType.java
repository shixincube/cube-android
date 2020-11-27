package cube.whiteboard.service;

/**
 * 画笔类型
 *
 * @author LiuFeng
 * @data 2020/8/14 14:14
 */
public enum BrushType {
    /**
     * 铅笔
     */
    Pencil(3),

    /**
     * 方框
     */
    Rect(4),

    /**
     * 文字
     */
    Text(5),

    /**
     * 椭圆
     */
    Ellipse(6),

    /**
     * 箭头
     */
    Arrow(7);


    public int type;

    BrushType(int type) {
        this.type = type;
    }
}

