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

package cell.core.net;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

import cell.util.collection.FlexibleByteBuffer;

/**
 * 非阻塞网络接收器会话。
 */
public class NonblockingAcceptorSession extends Session {

	/** 数据块大小。 */
	private int block;

	/** 最近一次读数据的计时点。 */
	protected long readTick = 0;
	/** 最近一次写数据的计时点。 */
	protected long writeTick = 0;

	/** 接收消息时，当次未处理数据。 */
	protected FlexibleByteBuffer readCache = new FlexibleByteBuffer(4096);

	/** 待发送消息列表。 */
	private LinkedList<Message> sendBuffer = new LinkedList<Message>();

	protected SelectionKey selectionKey = null;

	/** 当前会话对应的 SocketChannel 。 */
	protected SocketChannel socketChannel = null;

	/** 所属的工作器。 */
	protected NonblockingAcceptorWorker worker = null;

	/**
	 * 构造函数。
	 *
	 * @param address 对应的地址。
	 * @param block 数据块大小。
	 */
	public NonblockingAcceptorSession(InetSocketAddress address, int block) {
		super(address);
		this.block = block;
	}

	/**
	 * 得到缓存块大小。
	 * 
	 * @return 返回缓存块大小。
	 */
	public int getBlock() {
		return this.block;
	}

	/**
	 * 添加消息到发送缓存。
	 * 
	 * @param message 待添加的消息。
	 */
	protected void putMessage(Message message) {
		synchronized (this.sendBuffer) {
			this.sendBuffer.add(message);
		}
	}

	/**
	 * 消息队列是否为空。
	 * 
	 * @return 如果消息队列为空则返回 {@code true} 。
	 */
	protected boolean isEmptyMessage() {
		synchronized (this.sendBuffer) {
			return this.sendBuffer.isEmpty();
		}
	}

	/**
	 * 将消息队列里的第一条消息出队。
	 * 
	 * @return 返回消息队列里的第一条消息。
	 */
	protected Message pollMessage() {
		synchronized (this.sendBuffer) {
			return this.sendBuffer.poll();
		}
	}

	/**
	 * 消息发送队列里消息数量。
	 * 
	 * @return 返回消息发送队列里消息数量。
	 */
	protected int numMessages() {
		synchronized (this.sendBuffer) {
			return this.sendBuffer.size();
		}
	}

}
