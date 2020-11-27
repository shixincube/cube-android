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

import java.util.concurrent.atomic.AtomicBoolean;

import cell.core.net.Session;

/**
 * 会话上下文。
 */
public class TalkContext {

	/** 对应的服务器。 */
	protected BaseServer server;

	/** 网络层会话。 */
	protected Session session;

	/** 对端标签。 */
	protected String tag;

	/** 是否已经被上锁。 */
	protected AtomicBoolean lock = new AtomicBoolean(false);

	/**
	 * 构造函数。
	 *
	 * @param server 服务器实例。
	 * @param session 消息层会话上下文。
	 */
	public TalkContext(BaseServer server, Session session) {
		this.server = server;
		this.session = session;
	}

	/**
	 * 获取上下文所在的服务器实例。
	 *
	 * @return 返回上下文所在的服务器实例。
	 */
	public BaseServer getServer() {
		return this.server;
	}

	/**
	 * 返回对端标签。
	 *
	 * @return 返回对端标签。
	 */
	public String getTag() {
		return this.tag;
	}

	/**
	 * 设置标签。
	 *
	 * @param tag 设置对端标签。
	 */
	protected void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * 获取服务器端口。
	 *
	 * @return 返回服务器端口。
	 */
	public int getServerPort() {
		return this.server.getPort();
	}

	/**
	 * 获取当前上下文的会话。
	 *
	 * @return 返回当前上下文的会话。
	 */
	public Session getSession() {
		return this.session;
	}

	/**
	 * 获取会话 ID 。
	 *
	 * @return 返回会话 ID 。
	 */
	public Long getSessionId() {
		return this.session.getId();
	}

	/**
	 * 获取 Session 对应的主机名。
	 *
	 * @return 返回 Session 对应的主机名。
	 */
	public String getSessionHost() {
		return this.session.getAddress().getHostString();
	}

	/**
	 * 获取 Session 对应的连接端口。
	 *
	 * @return 返回 Session 对应的连接端口。
	 */
	public int getSessionPort() {
		return this.session.getAddress().getPort();
	}

	/**
	 * 是否已经上锁。
	 *
	 * @return 如果上锁返回 {@code true} 。
	 */
	public boolean isLock() {
		return this.lock.get();
	}

	/**
	 * 设置锁状态。
	 *
	 * @param lock 是否上锁。
	 */
	public void setLock(boolean lock) {
		this.lock.set(lock);
	}

}
