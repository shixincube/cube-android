package cube.whiteboard.service;

import android.content.Context;
import android.webkit.WebView;

/**
 * 白板画面
 *
 * @author LiuFeng
 * @data 2020/8/14 14:13
 */
public abstract class WhiteboardView extends WebView {

    public WhiteboardView(Context context) {
        super(context);
    }

    /**
     * 选择画笔
     *
     * @param type
     */
    public abstract void select(BrushType type);

    /**
     * 取消选中
     */
    public abstract void unSelect();

    /**
     * 设置画笔粗细
     *
     * @param weight 画线粗细
     */
    public abstract void setWeight(int weight);

    /**
     * 设置画笔颜色
     *
     * @param color 画线颜色
     */
    public abstract void setColor(String color);

    /**
     * 撤销
     */
    public abstract void undo();

    /**
     * 清空
     */
    public abstract void cleanup();

    /**
     * 分享文件
     *
     * @param fileId 上传文件ID
     */
    public abstract void shareFile(String fileId);

    /**
     * 上一页
     */
    public abstract void prevPage();

    /**
     * 下一页
     */
    public abstract void nextPage();

    /**
     * 跳转页面
     *
     * @param page 某页
     */
    public abstract void gotoPage(int page);
}
