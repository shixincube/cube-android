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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cell.core.net.Message;
import cell.core.net.NonblockingAcceptor;
import cell.core.net.Session;
import cell.core.talk.Protocol.AckProtocol;
import cell.core.talk.Protocol.HandshakeProtocol;
import cell.core.talk.Protocol.SpeakProtocol;
import cell.core.talk.Protocol.StreamProtocol;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.LogLevel;
import cell.util.log.Logger;

/**
 * 为 Cellet 接收数据服务的服务器实现。
 */
public class Server extends BaseServer implements HeartbeatMachineListener, AcknowledgementListener {

	/** 接收器数据处理句柄。 */
	private AcceptorHandler handler = null;

	/** 接收器。 */
	private NonblockingAcceptor acceptor = null;

	/** 最大线程数。 */
	private int maxThread = 8;

	/** 消息层会话对应的 Talk 上下文。 Key: Session ID, Value: Talk context */
	private ConcurrentHashMap<Long, TalkContext> sessionContextMap;

	/** 已发送的原语列表。 */
	private HashMap<Long, PrimitiveCapsule> spokeMap;

	/** 应答超时管理。 */
	private Acknowledgement acknowledgement;

	/** 允许的最大连接数。 */
	private int maxConnectNum = 1000;

	/**
	 * 构造函数。
	 *
	 * @param tag 服务器的标签。
	 * @param host 服务器绑定地址。
	 * @param port 服务器绑定端口。
	 */
	public Server(String tag, String host, int port) {
		super(tag, host, port);
		this.sessionContextMap = new ConcurrentHashMap<Long, TalkContext>();
		this.spokeMap = new HashMap<Long, PrimitiveCapsule>();
		this.acknowledgement = new Acknowledgement(this.spokeMap);
		this.acknowledgement.setListener(this);
	}

	/**
	 * 设置最大工作线程数。
	 *
	 * @param max 最大线程数。
	 */
	public void setMaxThread(int max) {
		this.maxThread = max;
		if (null != this.handler) {
			this.handler.resetMaxThreads(max);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMaxConnections(int maxConnectNum) {
		this.maxConnectNum = maxConnectNum;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getMaxConnections() {
		return this.maxConnectNum;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean start() {
		if (null != this.acceptor) {
			Logger.i(this.getClass(), "The acceptor is not null");
			return false;
		}

		this.acceptor = new NonblockingAcceptor();
		this.acceptor.setMaxConnectNum(this.maxConnectNum);
		this.handler = new AcceptorHandler(this, this.maxThread);

		this.acceptor.setHandler(this.handler);

		if (null == this.heartbeatMachine) {
			this.heartbeatMachine = new HeartbeatMachine(this.acceptor);
			this.heartbeatMachine.setListener(this);
			this.heartbeatMachine.setInterval(this.heartbeat);
		}

		// 绑定地址和端口
		InetSocketAddress address = new InetSocketAddress(this.host, this.port);
		if (!this.acceptor.bind(address)) {
			// 地址绑定错误
			this.acceptor.setHandler(null);
			this.acceptor = null;

			this.handler.shutdown();
			this.handler = null;

			Logger.w(this.getClass(), "Bind port " + this.port + " failed");
			return false;
		}

		// 启动 HM
		this.heartbeatMachine.startup();

		// 启动 Ack
		this.acknowledgement.start();

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() {
		if (null == this.acceptor) {
			return;
		}

		this.acknowledgement.stop();

		// 解绑
		this.acceptor.unbind();
		this.acceptor = null;

		if (null != this.handler) {
			this.handler.shutdown();
		}

		if (null != this.heartbeatMachine) {
			this.heartbeatMachine.shutdown();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean speak(Session session, String cellet, Primitive primitive) {
		return speakWithoutAck(session, cellet, primitive);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean speak(Session session, String cellet, Primitive primitive, boolean ack) {
		if (ack) {
			return speakWithAck(session, cellet, primitive);
		}
		else {
			return speakWithoutAck(session, cellet, primitive);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean speakWithAck(Session session, String cellet, Primitive primitive) {
		if (!this.acceptor.isRunning()) {
			return false;
		}

		// 记录
		synchronized (this.spokeMap) {
			PrimitiveCapsule pc = this.spokeMap.get(session.getId());
			if (null == pc) {
				pc = new PrimitiveCapsule(session);
				this.spokeMap.put(session.getId(), pc);
			}
			pc.addPrimitive(cellet, primitive);
		}

		FlexibleByteBuffer buf = Protocol.serializeSpeakAck(cellet, primitive);

		Message message = new Message(buf.array(), 0, buf.limit());
		message.setContext(new MessageContext(cellet, primitive));

		Protocol.recovery(buf);

		try {
			this.acceptor.write(session, message);
		} catch (IOException e) {
			Logger.e(this.getClass(), "Speak error : " + session.getId() + "(" + session.toString() + ")", e);

			// 清理
			synchronized (this.spokeMap) {
				PrimitiveCapsule pc = this.spokeMap.get(session.getId());
				if (null != pc) {
					pc.removePrimitive(cellet, primitive);
				}
			}

			return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean speakWithoutAck(Session session, String cellet, Primitive primitive) {
		if (!this.acceptor.isRunning()) {
			return false;
		}

		// 指定为 SpeakNoAck 包
		FlexibleByteBuffer buf = Protocol.serializeSpeakNoAck(cellet, primitive);

		Message message = new Message(buf.array(), 0, buf.limit());
		message.setContext(new MessageContext(cellet, primitive));

		Protocol.recovery(buf);

		try {
			this.acceptor.write(session, message);
		} catch (IOException e) {
			Logger.e(this.getClass(), "Speak error : " + session.getId() + "(" + session.toString() + ")", e);
			return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void hangup(Session session, String cellet, boolean now) {
		if (null == this.acceptor || !this.acceptor.isRunning()) {
			return;
		}

		if (now) {
			// 立即关闭
			PrimitiveCapsule pc = null;
			synchronized (this.spokeMap) {
				pc = this.spokeMap.remove(session.getId());
			}
			if (null != pc) {
				pc.clean();
			}
		}
		else {
			// 等待数据发送完成之后关闭
			PrimitiveCapsule pc = null;
			synchronized (this.spokeMap) {
				pc = this.spokeMap.get(session.getId());
			}

			int count = 0;
			while (pc.getPrimitiveNum() > 0) {
				try {
					Thread.sleep(10L);
				} catch (InterruptedException e) {
					Logger.log(this.getClass(), e, LogLevel.ERROR);
				}

				++count;
				if (count >= 100) {
					break;
				}
			}

			synchronized (this.spokeMap) {
				this.spokeMap.remove(session.getId());
			}
		}

		// 关闭会话
		this.acceptor.close(session);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<TalkContext> getAllContext() {
		return new ArrayList<TalkContext>(this.sessionContextMap.values());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TalkContext getTalkContext(Long sessionId) {
		return this.sessionContextMap.get(sessionId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean processHandshake(Session session, HandshakeProtocol handshake) {
		TalkContext context = this.sessionContextMap.get(session.getId());

		if (null == context) {
			context = new TalkContext(this, session);
			this.sessionContextMap.put(session.getId(), context);
		}

		// 建立数据关系
		session.setSecretKey(handshake.key);
		context.setTag(handshake.tag);

		handshake.destroy();

		// 向客户端返回数据
		FlexibleByteBuffer buf = Protocol.serializeHandshake(session.getSecretKey(), this.tag);
		Message message = new Message(buf.array(), 0, buf.limit());
		Protocol.recovery(buf);
		try {
			this.acceptor.write(session, message);
		} catch (IOException e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		}

		if (Logger.isDebugLevel()) {
			Logger.d(this.getClass(), "Handshake with " + session.toString() + " (" + session.getId() + ")");
		}

		for (ServerListener listener : this.listeners.values()) {
			listener.onContacted(context, this);
		}

		// 添加到 HM
		this.heartbeatMachine.addSession(session, context.getTag());

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processTimeout(Session session) {
		// 移除会话
		this.heartbeatMachine.removeSession(session);

		// 关闭会话
		this.acceptor.close(session);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void fireListened(Session session, SpeakProtocol protocol) {
		if (protocol.ack) {
			// 发送应答
			FlexibleByteBuffer buf = Protocol.serializeAck(protocol.target, protocol.primitive.getSN());
			Message message = new Message(buf.array(), 0, buf.limit());
			try {
				this.acceptor.write(session, message);
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.WARNING);
			}
			Protocol.recovery(buf);
		}

		// 回调
		ServerListener listener = this.listeners.get(protocol.target);
		TalkContext context = this.sessionContextMap.get(session.getId());
		if (null != listener && null != context) {
			listener.onListened(context, protocol.primitive);
		}
		else {
			// TODO 异常处理
			Logger.w(this.getClass(), "Can not find cellet : " + protocol.target + " for session : " + session.getId());
		}

		protocol.destroy();
	}

	protected void fireListened(Session session, StreamProtocol protocol) {
		// TODO
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void fireSpoke(Session session, MessageContext context) {
		ServerListener listener = this.listeners.get(context.target);
		TalkContext talkContext = this.sessionContextMap.get(session.getId());
		if (null != listener && null != talkContext) {
			listener.onSpoke(talkContext, context.primitive);
		}
		else {
			// TODO 异常处理
			Logger.w(this.getClass(), "Can not find target : " + context.target + " for session : " + session.getId());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void fireAck(Session session, byte[] data) {
		AckProtocol protocol = Protocol.deserializeAck(data);
		Primitive primitive = null;

		synchronized (this.spokeMap) {
			PrimitiveCapsule pc = this.spokeMap.get(session.getId());
			primitive = pc.removePrimitive(protocol.target, protocol.sn);
		}

		if (null != primitive) {
			ServerListener listener = this.listeners.get(protocol.target);
			TalkContext talkContext = this.sessionContextMap.get(session.getId());
			if (null != listener && null != talkContext) {
				listener.onAck(talkContext, primitive);
			}
		}

		protocol.destroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void fireQuitted(Session session) {
		TalkContext talkContext = this.sessionContextMap.remove(session.getId());
		if (null != talkContext) {
			for (ServerListener listener : this.listeners.values()) {
				listener.onQuitted(talkContext, this);
			}
		}

		// 从 HM 里删除 Session
		this.heartbeatMachine.removeSession(session);

		synchronized (this.spokeMap) {
			PrimitiveCapsule pc = this.spokeMap.remove(session.getId());
			if (null != pc) {
				pc.clean();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void fireFault(TalkError error, Session session) {
		if (null != session) {
			TalkContext talkContext = this.sessionContextMap.remove(session.getId());
			if (null != talkContext) {
				error.setTalkContext(talkContext);
			}
		}

		for (ServerListener listener : this.listeners.values()) {
			listener.onFailed(error);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSessionTimeout(Session session, long duration) {
		Logger.w(this.getClass(), "Session " + session.toString() + " (" + session.getId() + ") heartbeat timeout.");

		// 对于超时的会话进行关闭
		this.acceptor.close(session);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAckTimeout(Session session, String cellet, Primitive primitive) {
		ServerListener listener = this.listeners.get(cellet);
		TalkContext context = this.sessionContextMap.get(session.getId());
		listener.onSpeakTimeout(context, primitive);
	}

}
