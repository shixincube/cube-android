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

import org.json.JSONException;
import org.json.JSONObject;

import cube.core.model.Entity;

/**
 * 废弃的文件。
 */
public class TrashFile extends Entity {

    private Directory parent;

    private FileLabel fileLabel;

    public TrashFile(Directory parent, FileLabel fileLabel) {
        super(fileLabel.id);
        this.parent = parent;
        this.fileLabel = fileLabel;
    }

    public TrashFile(JSONObject json) throws JSONException {
        super(json);
        this.fileLabel = new FileLabel(json.getJSONObject("file"));
        this.parent = new Directory(json.getJSONObject("parent"));
    }

    public Directory getParent() {
        return this.parent;
    }

    public FileLabel getFileLabel() {
        return this.fileLabel;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("parent", this.parent.toCompactJSON());
            json.put("file", this.fileLabel.toJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
