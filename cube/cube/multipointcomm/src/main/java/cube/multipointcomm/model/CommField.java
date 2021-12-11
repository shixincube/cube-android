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

package cube.multipointcomm.model;

import cube.contact.model.Contact;
import cube.contact.model.Self;
import cube.core.Pipeline;
import cube.core.model.Entity;
import cube.multipointcomm.MediaListener;

/**
 * 多方通信场域。
 */
public class CommField extends Entity {

    private Self self;

    /**
     * 通信管道。
     */
    private Pipeline pipeline;

    /**
     * 通信场创建人。
     */
    private Contact founder;

    /**
     * 场域名称。
     */
    private String name;

    /**
     * 通讯域开始通话时间。
     */
    private long startTime;

    /**
     * 通讯域结束通话时间。
     */
    private long endTime;

    /**
     * 媒体监听器。
     */
    private MediaListener mediaListener;

    public CommField(Self self, Pipeline pipeline) {
        super(self.id);
        this.self = self;
        this.pipeline = pipeline;
        this.founder = self;
        this.name = this.founder.getName() + "#" + this.id;
    }


}
