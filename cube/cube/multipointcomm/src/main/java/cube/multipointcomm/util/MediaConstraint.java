/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.multipointcomm.util;

import org.json.JSONException;
import org.json.JSONObject;

import cube.util.JSONable;

/**
 * 媒体约束。
 */
public class MediaConstraint implements JSONable {

    /**
     * 是否使用 Video 设备。
     */
    public final boolean videoEnabled;

    /**
     * 是否使用 Audio 设备。
     */
    public final boolean audioEnabled;

    /**
     * 视频画面尺寸描述。
     */
    private VideoDimension videoDimension;

    private int videoFps;

    /**
     * 强制限制模式。
     */
    public boolean limitPattern = false;

    public MediaConstraint(boolean videoEnabled) {
        this(videoEnabled, true);
    }

    /**
     * 构造函数。
     *
     * @param videoEnabled 是否使用 Video 设备。
     * @param audioEnabled 是否使用 Audio 设备。
     */
    public MediaConstraint(boolean videoEnabled, boolean audioEnabled) {
        this.videoEnabled = videoEnabled;
        this.audioEnabled = audioEnabled;
        this.videoDimension = VideoDimension.QVGA;
        this.videoFps = 15;
    }

    /**
     * 构造函数。
     *
     * @param json
     * @throws JSONException
     */
    public MediaConstraint(JSONObject json) throws JSONException {
        this.audioEnabled = json.getBoolean("audio");
        this.videoEnabled = json.getBoolean("video");
        this.videoDimension = VideoDimension.parse(json.getJSONObject("dimension"));
        this.videoFps = 15;
    }

    public VideoDimension getVideoDimension() {
        if (this.limitPattern) {
            this.videoDimension = VideoDimension.QVGA;
            return VideoDimension.QVGA;
        }
        else {
            return this.videoDimension;
        }
    }

    public int getVideoFps() {
        return this.videoFps;
    }

    public void setVideoDimension(VideoDimension dimension) {
        this.videoDimension = dimension;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("audio", this.audioEnabled);
            json.put("video", this.videoEnabled);
            json.put("dimension", this.videoDimension.toJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
