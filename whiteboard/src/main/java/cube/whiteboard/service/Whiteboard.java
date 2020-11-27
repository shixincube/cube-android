package cube.whiteboard.service;

import android.view.View;

import java.util.List;

import cube.utils.Utils;

/**
 * 白板对象
 *
 * @author workerinchina@163.com
 */
public class Whiteboard {
    /**
     * 白板ID
     */
    private String whiteboardId;
    /**
     * 白名字
     */
    private String name;
    /**
     * 当前共享此白板用户
     */
    private List<String> shares;

    /**
     * 是否共享
     */
    private boolean isShared;

    public Whiteboard() {
        this.whiteboardId = String.valueOf(Utils.createSN());
        this.name = whiteboardId;
    }

    public Whiteboard(String wid) {
        this.whiteboardId = wid;
        this.name = wid;
    }

    public Whiteboard(String whiteboardId, String name, View view, List<String> shares, boolean isShared) {
        this.whiteboardId = whiteboardId;
        this.name = name;
        this.shares = shares;
        this.isShared = isShared;
    }

    public String getWhiteboardId() {
        return whiteboardId;
    }

    public void setWhiteboardId(String whiteboardId) {
        this.whiteboardId = whiteboardId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getShares() {
        return shares;
    }

    public void setShares(List<String> shares) {
        this.shares = shares;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean shared) {
        isShared = shared;
    }
}
