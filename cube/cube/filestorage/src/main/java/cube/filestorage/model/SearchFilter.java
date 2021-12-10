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

package cube.filestorage.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cube.util.JSONable;

/**
 * 文件搜索过滤器。
 */
public class SearchFilter implements JSONable {

    private int beginIndex;
    private int endIndex;
    private List<String> typeList;

    public SearchFilter() {
        this.beginIndex = 0;
        this.endIndex = 99;
        this.typeList = new ArrayList<>();
    }

    public SearchFilter(String[] types) {
        this.beginIndex = 0;
        this.endIndex = 99;
        this.typeList = new ArrayList<>();
        setTypes(types);
    }

    public void setRange(int beginIndex, int endIndex) {
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    public void addType(String type) {
        if (!this.typeList.contains(type)) {
            this.typeList.add(type);
        }
    }

    public void setTypes(String[] types) {
        for (String type : types) {
            this.addType(type);
        }
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("begin", this.beginIndex);
            json.put("end", this.endIndex);

            if (!this.typeList.isEmpty()) {
                JSONArray array = new JSONArray();
                for (String type : this.typeList) {
                    array.put(type);
                }
                json.put("type", array);
            }
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
