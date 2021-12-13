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

/**
 * 视频画面尺寸描述。
 */
public enum VideoDimension {

    QVGA(320, 240),

    VGA(640, 480),

    SVGA(800, 600),

    HD(1280, 720),

    FullHD(1920, 1080),

    FourK(4096, 2160),

    EightK(7680, 4320)

    ;

    public final int width;

    public final int height;

    VideoDimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("width", this.width);
            json.put("height", this.height);

            StringBuilder constraintsString = new StringBuilder();
            constraintsString.append("{");
            constraintsString.append("\"width\":{\"max\":").append(this.width).append("}");
            constraintsString.append("\"height\":{\"max\":").append(this.height).append("}");
            constraintsString.append("}");
            JSONObject constraints = new JSONObject(constraintsString.toString());
            json.put("constraints", constraints);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static VideoDimension parse(JSONObject json) throws JSONException {
        int width = json.getInt("width");
        int height = json.getInt("height");
        for (VideoDimension dimension : VideoDimension.values()) {
            if (dimension.width == width || dimension.height == height ||
                    (dimension.width == height && dimension.height == width)) {
                return dimension;
            }
        }

        return VideoDimension.VGA;
    }
}
