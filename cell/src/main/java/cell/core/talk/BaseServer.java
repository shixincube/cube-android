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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cell.api.Servable;
import cell.core.net.Session;
import cell.core.talk.Protocol.HandshakeProtocol;
import cell.core.talk.Protocol.SpeakProtocol;
import cell.core.talk.Protocol.StreamProtocol;

/**
 * 服务器的抽象基类。对服务器的一些公共特性进行描述和实现。
 */
public abstract class BaseServer implements Servable {

	/** 服务器的内核标签。 */
	protected String tag;

	/** 服务器的绑定地址。 */
	protected String host;

	/** 服务器的绑定端口。 */
	protected int port;

	/** 心跳间隔。 */
	protected long heartbeat = 2L * 60L * 1000L;

	/** Cellet 名对应的监听器。 */
	protected ConcurrentHashMap<String, ServerListener> listeners;

	/** 心跳管理机。 */
	protected HeartbeatMachine heartbeatMachine;

	/**
	 * 构造函数。
	 *
	 * @param tag 标签。
	 * @param host 服务的地址。
	 * @param port 服务的端口。
	 */
	public BaseServer(String tag, String host, int port) {
		this.tag = tag.toString();
		this.host = host.toString();
		this.port = port;
		this.listeners = new ConcurrentHashMap<String, ServerListener>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTag() {
		return this.tag;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getHost() {
		return this.host;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getPort() {
		return this.port;
	}

	/**
	 * 设置心跳间隔。
	 *
	 * @param heartbeat 心跳间隔，单位：毫秒。
	 */
	public void setHeartbeat(long heartbeat) {
		this.heartbeat = heartbeat;
		if (null != this.heartbeatMachine) {
			this.heartbeatMachine.setInterval(heartbeat);
		}
	}

	/**
	 * 添加对指定 {@link cell.core.cellet.Cellet} 的监听器。
	 *
	 * @param cellet Cellet 名称。
	 * @param listener 监听器实例。
	 */
	public void addListener(String cellet, ServerListener listener) {
		this.listeners.put(cellet, listener);
	}

	/**
	 * 移除对指定 {@link cell.core.cellet.Cellet} 的监听器。
	 *
	 * @param cellet Cellet 名称。
	 */
	public void removeListener(String cellet) {
		this.listeners.remove(cellet);
	}

	/**
	 * 启动服务器。
	 *
	 * @return 启动成功返回 {@code true} 。
	 */
	public abstract boolean start();

	/**
	 * 停止服务器。
	 */
	public abstract void stop();

	/**
	 * 向指定终端发送原语数据。
	 *
	 * @param session 终端的会话上下文。
	 * @param cellet 源 Cellet 的名称。
	 * @param primitive 原语数据。
	 * @return 数据成功写入发送通道返回 {@code true} 。
	 */
	public abstract boolean speak(Session session, String cellet, Primitive primitive);

	/**
	 * 向指定终端发送原语数据。
	 *
	 * @param session 终端的会话上下文。
	 * @param cellet 源 Cellet 的名称。
	 * @param primitive 原语数据。
	 * @param ack 是否需要终端应答接收。
	 * @return 数据成功写入发送通道返回 {@code true} 。
	 */
	public abstract boolean speak(Session session, String cellet, Primitive primitive, boolean ack);

	/**
	 * 向指定终端发送应答原语数据。终端将进行应答。
	 *
	 * @param session 终端的会话上下文。
	 * @param cellet 源 Cellet 的名称。
	 * @param primitive 原语数据。
	 * @return 数据成功写入发送通道返回 {@code true} 。
	 */
	public abstract boolean speakWithAck(Session session, String cellet, Primitive primitive);

	/**
	 * 向指定终端发送无应答原语数据。终端不会进行应答。
	 *
	 * @param session 终端的会话上下文。
	 * @param cellet 源 Cellet 的名称。
	 * @param primitive 原语数据。
	 * @return 数据成功写入发送通道返回 {@code true} 。
	 */
	public abstract boolean speakWithoutAck(Session session, String cellet, Primitive primitive);

	/**
	 * 关闭指定终端的连接。
	 *
	 * @param session 终端的会话上下文。
	 * @param cellet 源 Cellet 的名称。
	 * @param now 是否立即关闭不等待连接的终端关闭。
	 */
	public abstract void hangup(Session session, String cellet, boolean now);

	/**
	 * 获取所有客户端会话的上下文。
	 *
	 * @return 返回所有客户端会话的上下文列表。
	 */
	public abstract List<TalkContext> getAllContext();

	/**
	 * 处理超时的会话。
	 *
	 * @param session 消息层的会话上下文。
	 */
	protected abstract void processTimeout(Session session);

	/**
	 * 处理来自客户端的握手操作。
	 *
	 * @param session 消息层的会话上下文。
	 * @param protocol 握手协议。
	 * @return 握手成功返回 {@code true} 。
	 */
	protected abstract boolean processHandshake(Session session, HandshakeProtocol protocol);

	/**
	 * 触发 Listened 事件。
	 *
	 * @param session 消息层的会话上下文。
	 * @param protocol 会话协议。
	 */
	protected abstract void fireListened(Session session, SpeakProtocol protocol);

	/**
	 * 触发 Listened 事件。
	 *
	 * @param session 消息层的会话上下文。
	 * @param protocol 流协议。
	 */
	protected abstract void fireListened(Session session, StreamProtocol protocol);

	/**
	 * 触发 Spoke 事件。
	 *
	 * @param session 消息层的会话上下文。
	 * @param messageContext 消息上下文。
	 */
	protected abstract void fireSpoke(Session session, MessageContext messageContext);

	/**
	 * 触发 Ack 事件。
	 *
	 * @param session 消息层的会话上下文。
	 * @param data 应答的数据。
	 */
	protected abstract void fireAck(Session session, byte[] data);

	/**
	 * 触发 Quitted 事件。
	 *
	 * @param session 消息层的会话上下文。
	 */
	protected abstract void fireQuitted(Session session);

	/**
	 * 触发 Fault 事件。
	 *
	 * @param error 会话的错误描述。
	 * @param session 消息层的会话上下文。
	 */
	protected abstract void fireFault(TalkError error, Session session);

}
