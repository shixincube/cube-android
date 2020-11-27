/**
 * This source file is part of Cell.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
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

package cell.core.talk;

import android.content.Context;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import cell.core.net.BlockingConnector;
import cell.core.net.Message;
import cell.core.net.MessageConnector;
import cell.core.net.MessageHandler;
import cell.core.net.MessageService;
import cell.core.net.NonblockingConnector;
import cell.core.net.Session;

/**
 * Speaker 的连接器实现。
 */
public class SpeakerConnector extends MessageService implements MessageConnector {

    /** 非阻塞连接器。 */
    private NonblockingConnector nonblockingConnector;

    /** 阻塞连接器。 */
    private BlockingConnector blockingConnector;

    /**
     * 构造函数。
     *
     * @param androidContext 应用上下文。
     * @param executor 多线程执行器。
     */
    public SpeakerConnector(Context androidContext, ExecutorService executor) {
        this(androidContext, executor, false);
    }

    /**
     * 构造函数。
     *
     * @param androidContext 应用上下文。
     * @param executor 多线程执行器。
     * @param nonblocking 是否需使用非阻塞模式。
     */
    public SpeakerConnector(Context androidContext, ExecutorService executor, boolean nonblocking) {
        if (nonblocking) {
            this.nonblockingConnector = new NonblockingConnector(androidContext);
            this.blockingConnector = null;
        }
        else {
            this.blockingConnector = new BlockingConnector(androidContext, executor);
            this.nonblockingConnector = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHandler(MessageHandler handler) {
        if (null != this.blockingConnector) {
            this.blockingConnector.setHandler(handler);
        }
        else {
            this.nonblockingConnector.setHandler(handler);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageHandler getHandler() {
        return (null != this.blockingConnector) ?
                this.blockingConnector.getHandler() : this.nonblockingConnector.getHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean connect(InetSocketAddress address) {
        return (null != this.blockingConnector) ?
                this.blockingConnector.connect(address) : this.nonblockingConnector.connect(address);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect() {
        if (null != this.blockingConnector) {
            this.blockingConnector.disconnect();
        }
        else {
            this.nonblockingConnector.disconnect();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected() {
        return (null != this.blockingConnector) ?
                this.blockingConnector.isConnected() : this.nonblockingConnector.isConnected();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConnectTimeout(long timeout) {
        if (null != this.blockingConnector) {
            this.blockingConnector.setConnectTimeout(timeout);
        }
        else {
            this.nonblockingConnector.setConnectTimeout(timeout);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBlockSize(int size) {
        if (null != this.blockingConnector) {
            this.blockingConnector.setBlockSize(size);
        }
        else {
            this.nonblockingConnector.setBlockSize(size);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Session getSession() {
        return (null != this.blockingConnector) ?
                this.blockingConnector.getSession() : this.nonblockingConnector.getSession();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(Message message) throws IOException {
        if (null != this.blockingConnector) {
            this.blockingConnector.write(message);
        }
        else {
            this.nonblockingConnector.write(message);
        }
    }
}
