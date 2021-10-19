/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Shixin Cube Team.
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

package cube.contact;

import cube.contact.handler.SignHandler;
import cube.contact.model.Self;
import cube.core.Module;

/**
 * 联系人模块。
 */
public class ContactService extends Module {

    /**
     * 模块名。
     */
    public final static String NAME = "Contact";

    private ContactPipelineListener pipelineListener;

    private boolean selfReady;

    public ContactService() {
        super(NAME);
        this.selfReady = false;
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        this.pipelineListener = new ContactPipelineListener(this);
        this.pipeline.addListener(NAME, this.pipelineListener);

        return true;
    }

    @Override
    public void stop() {
        super.stop();

        if (null != this.pipelineListener) {
            this.pipeline.removeListener(NAME, this.pipelineListener);
            this.pipelineListener = null;
        }
    }

    @Override
    public boolean isReady() {
        return false;
    }

    /**
     * 签入。
     *
     * @param self
     * @param handler
     * @return
     */
    public boolean signIn(Self self, SignHandler handler) {
        if (this.selfReady) {
            return false;
        }

        if (!this.hasStarted()) {
            this.start();
        }



        return true;
    }
}
