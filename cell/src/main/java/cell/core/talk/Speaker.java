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
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import cell.api.NucleusTag;
import cell.api.Speakable;
import cell.api.TalkListener;
import cell.core.net.Message;
import cell.core.net.MessageHandler;
import cell.core.net.Session;
import cell.core.talk.Protocol.AckProtocol;
import cell.core.talk.Protocol.HandshakeProtocol;
import cell.core.talk.Protocol.SpeakProtocol;
import cell.util.CachedQueueExecutor;
import cell.util.Utils;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.LogLevel;
import cell.util.log.Logger;

/**
 * Talk 会话客户端实现。
 */
public class Speaker implements Speakable, MessageHandler,
					HeartbeatMachineListener, AcknowledgementListener {

	/**
	 * 连接状态描述。
	 */
	enum State {
		/**
		 * 连接已断开。
		 */
		Disconnected,

		/**
		 * 正在连接。
		 */
		Connecting,

		/**
		 * 连接已建立。
		 */
		Connected,

		/**
		 * 正在断开连接。
		 */
		Disconnecting
	}

	/** 内核标签。 */
	private NucleusTag tag;

	/** 已连接的服务器的标签。 */
	private String serverTag;

	/** 连接地址。 */
	private InetSocketAddress address;

	/** 连接状态描述。 */
	private State state;

	/** 连接器。 */
	private SpeakerConnector connector;

	/** 会话。 */
	private Session session;

	/** 密钥。 */
	private byte[] secretKey;

	/** 线程池执行器。 */
	private ExecutorService executor;

	/** 可应答的接收队列。 */
	private ConcurrentLinkedQueue<Message> receivedWithAckQueue;

	/** 无应答的接收队列。 */
	private ConcurrentLinkedQueue<Message> receivedWithoutAckQueue;

	/** 流接收队列。 */
	private ConcurrentLinkedQueue<Message> receivedStreamQueue;

	/** 发送队列。 */
	private ConcurrentLinkedQueue<Message> sentQueue;

	/** 资源中心。 */
	private SpeakerRes spkRes;

	/** 监听器。 */
	private TalkListener listener;

	/** 已发送的原语列表。 */
	private HashMap<Long, PrimitiveCapsule> spokeList;

	/** 重连定时器。 */
	private Timer reconnectTimer;
	
	/** 启动重连操作的延迟时长。 */
	private long reconnectDelay;

	/** 握手定时器。 */
	private Timer handshakeTimer;

	/** 心跳控制器。 */
	private HeartbeatMachine hm;

	/** 应答跟踪器。 */
	private Acknowledgement acknowledgement;
	
	/** 最大线程数 */
	private int maxThreads;

	/**
	 * 构造函数。
	 *
	 * @param tag 内核标签。
	 * @param host 连接服务器的地址。
	 * @param port 连接服务器的端口。
	 * @param listener 事件监听器。
	 */
	public Speaker(NucleusTag tag, String host, int port, TalkListener listener) {
		this.tag = tag;
		this.address = new InetSocketAddress(host, port);
		this.listener = listener;
		this.state = State.Disconnected;
		this.reconnectDelay = 3000L;
		this.receivedWithAckQueue = new ConcurrentLinkedQueue<Message>();
		this.receivedWithoutAckQueue = new ConcurrentLinkedQueue<Message>();
		this.receivedStreamQueue = new ConcurrentLinkedQueue<Message>();
		this.sentQueue = new ConcurrentLinkedQueue<Message>();
		this.spkRes = new SpeakerRes(this);
		this.spokeList = new HashMap<Long, PrimitiveCapsule>();
		this.acknowledgement = new Acknowledgement(this.spokeList);
		this.acknowledgement.setListener(this);
		this.maxThreads = 2;
	}

	/**
	 * 启动。
	 *
	 * @param maxThreads 最大工作线程数量。
	 * @return 如果启动成功返回 {@code true} ，否则返回 {@code false} 。
	 */
	public boolean start(int maxThreads) {
		if (this.state == State.Connecting || this.state == State.Connected) {
			return true;
		}

		synchronized (this) {
			this.state = State.Connecting;
		}

		if (null == this.executor) {
			this.maxThreads = maxThreads;
			this.executor = CachedQueueExecutor.newCachedQueueThreadPool(maxThreads);
		}

		if (null != this.reconnectTimer) {
			this.reconnectTimer.cancel();
			this.reconnectTimer.purge();
			this.reconnectTimer = null;
		}

		// 启动应答跟踪
		this.acknowledgement.start();

		// 初始化连接器
		this.connector = new SpeakerConnector(this.tag.getContext(), this.executor);
		this.connector.setHandler(this);

		// 初始化 HM
		if (null == this.hm) {
			this.hm = new HeartbeatMachine(this.connector);
			this.hm.setListener(this);
		}
		else {
			this.hm.resetConnector(this.connector);
		}

		// 进行连接
		if (!this.connector.connect(this.address)) {
			synchronized (this) {
				this.state = State.Disconnected;
			}
			this.connector.setHandler(null);
			this.connector = null;
			return false;
		}

		synchronized (this) {
			if (null != this.handshakeTimer) {
				this.handshakeTimer.cancel();
				this.handshakeTimer.purge();
				this.handshakeTimer = null;
			}

			// 进行握手超时控制
			this.handshakeTimer = new Timer();
			this.handshakeTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					Logger.w(Speaker.class, "Handshake timeout");

					stop(true);

					Thread thread = new Thread() {
						@Override
						public void run() {
							synchronized (Speaker.this) {
								if (null != handshakeTimer) {
									handshakeTimer.cancel();
									handshakeTimer.purge();
									handshakeTimer = null;
								}
							}

							TalkError fault = new TalkError(TalkError.HandshakeTimeout);
							listener.onFailed(Speaker.this, fault);
						}
					};
					thread.start();
				}
			}, 5000L);
		}

		return true;
	}

	/**
	 * 关闭连接。
	 *
	 * @param now 是否立即关闭，不等待连接断开。
	 */
	public void stop(boolean now) {
		if (null == this.connector || State.Disconnecting == this.state) {
			return;
		}

		synchronized (this) {
			this.state = State.Disconnecting;
		}

		if (!now) {
			int count = 0;
			// 不立即关闭，等待所有原语被确认
			while (this.numNoAckPrimitive() > 0) {
				try {
					Thread.sleep(10L);
				} catch (InterruptedException e) {
					// Nothing
				}
				++count;
				if (count >= 120) {
					break;
				}
			}
		}

		this.acknowledgement.stop();

		if (null != this.hm) {
			this.hm.shutdown();
		}

		if (null != this.reconnectTimer) {
			this.reconnectTimer.cancel();
			this.reconnectTimer.purge();
			this.reconnectTimer = null;
		}

		synchronized (this) {
			if (null != this.handshakeTimer) {
				this.handshakeTimer.cancel();
				this.handshakeTimer.purge();
				this.handshakeTimer = null;
			}
		}

		SpeakerConnector sc = this.connector;
		this.connector = null;

		sc.disconnect();

		int count = 100;
		while (count > 0) {
			if (this.state == State.Disconnected) {
				break;
			}

			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}

			--count;
		}

		sc = null;

		this.serverTag = null;
		this.secretKey = null;

		if (null != this.executor) {
			this.executor.shutdown();
			this.executor = null;
		}

		this.receivedWithAckQueue.clear();
		this.receivedWithoutAckQueue.clear();
		this.receivedStreamQueue.clear();
		this.sentQueue.clear();

		this.spokeList.clear();

		synchronized (this) {
			this.state = State.Disconnected;
		}
	}

	/**
	 * 获取服务器的标签。
	 *
	 * @return 返回服务器的标签。
	 */
	public String getServerTag() {
		return this.serverTag;
	}

	/**
	 * 获取连接状态。
	 *
	 * @return 返回连接状态。
	 */
	public State getState() {
		return this.state;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InetSocketAddress getRemoteAddress() {
		return this.address;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean speak(String cellet, Primitive primitive) {
		return speakWithoutAck(cellet, primitive);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean speak(String cellet, Primitive primitive, boolean ack) {
		if (ack) {
			return speakWithAck(cellet, primitive);
		}
		else {
			return speakWithoutAck(cellet, primitive);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean speakWithAck(String cellet, Primitive primitive) {
		synchronized (this) {
			if (State.Connected != this.state) {
				return false;
			}
		}

		// 记录
		PrimitiveCapsule pc = this.spokeList.get(this.session.getId());
		pc.addPrimitive(cellet, primitive);

		// 序列化数据
		FlexibleByteBuffer buf = Protocol.serializeSpeakAck(cellet, primitive);

		Message message = new Message(buf.array(), 0, buf.limit());
		message.setContext(new MessageContext(cellet, primitive));

		Protocol.recovery(buf);

		try {
			this.connector.write(message);
		} catch (IOException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);

			// 写数据失败，删除数据
			pc.removePrimitive(cellet, primitive);

			return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean speakWithoutAck(String cellet, Primitive primitive) {
		synchronized (this) {
			if (State.Connected != this.state) {
				Logger.w(this.getClass(), "Speaker state error: " + this.state.name());
				return false;
			}
		}

		// 序列化数据
		FlexibleByteBuffer buf = Protocol.serializeSpeakNoAck(cellet, primitive);

		Message message = new Message(buf.array(), 0, buf.limit());
		message.setContext(new MessageContext(cellet, primitive));

		Protocol.recovery(buf);

		try {
			this.connector.write(message);
		} catch (IOException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);

			return false;
		}

		return true;
	}

	/**
	 * 设置应答超时时间，单位：毫秒。
	 *
	 * @param timeout 应答超时时间。
	 */
	public void setAckTimeout(long timeout) {
		this.acknowledgement.resetTimeout(timeout);
	}

	/**
	 * 获取应答超时时间，单位：毫秒。
	 *
	 * @return 返回应答超时时间。
	 */
	public long getAckTimeout() {
		return this.acknowledgement.getTimeout();
	}

	/**
	 * 获取当前未应答的原语数量。
	 *
	 * @return 返回当前未应答的原语数量。
	 */
	public int numNoAckPrimitive() {
		if (null == this.session) {
			return 0;
		}

		PrimitiveCapsule pc = this.spokeList.get(this.session.getId());
		if (null == pc) {
			return 0;
		}

		return pc.getPrimitiveNum();
	}

	/**
	 * 获得 {@link HeartbeatMachine} 实例。
	 *
	 * @return 返回 {@link HeartbeatMachine} 实例。
	 */
	public HeartbeatMachine getHeartbeatMachine() {
		return this.hm;
	}

	/**
	 * 获取当前连接的消息层会话。
	 *
	 * @return 返回当前连接的消息层会话。
	 */
	public Session getSession() {
		return this.session;
	}

	/**
	 * 执行握手逻辑。
	 */
	private void handshake() {
		if (Logger.isDebugLevel()) {
//			Logger.d(this.getClass(), "Handshake : " + this.session.toString());
		}

		String skey = Utils.randomString(8);
		this.secretKey = skey.getBytes();
		FlexibleByteBuffer buf = Protocol.serializeHandshake(this.secretKey, this.tag.asString());
		Message message = new Message(buf.array(), 0, buf.limit());
		Protocol.recovery(buf);
		try {
			this.connector.write(message);
		} catch (IOException e) {
			Logger.e(this.getClass(), "Handshake failed", e);
		}
	}

	@Override
	public void sessionCreated(Session session) {
		this.session = session;

		// 启动 HM
		this.hm.addSession(session, this.tag.asString());

		// 加入 Session
		this.spokeList.put(session.getId(), new PrimitiveCapsule(session));
	}

	@Override
	public void sessionDestroyed(Session session) {
		// 如果 connector 不为空，则尝试恢复连接
		if (null != this.connector) {
			this.reconnectTimer = new Timer();
			this.reconnectTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					(new Thread() {
						@Override
						public void run() {
							// 先关闭
							Speaker.this.stop(true);

							// 进行重连连接
							Logger.i(Speaker.class, "Reconnect to " + address.getHostString() + ":" + address.getPort());
							Speaker.this.start(maxThreads);
						}
					}).start();
				}
			}, this.reconnectDelay);
		}

		// 移除 Session
		PrimitiveCapsule pc = this.spokeList.remove(session.getId());
		if (null != pc) {
			pc.clean();
		}

		// 关闭 HM
		if (null != this.hm) {
			this.hm.removeSession(session);
			this.hm.shutdown();
		}
	}

	@Override
	public void sessionOpened(Session session) {
		// 执行握手
		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				handshake();
			}
		});
	}

	@Override
	public void sessionClosed(Session session) {
		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				listener.onQuitted(Speaker.this);

				synchronized (Speaker.this) {
					state = State.Disconnected;
				}
			}
		});
	}

	@Override
	public void messageReceived(final Session session, final Message message) {
		byte[] protocol = Protocol.recognize(message.getPayload());
		if (Protocol.SpeakNoAck == protocol) {
			// 入队
			this.receivedWithoutAckQueue.offer(message);

			// 异步执行
			this.executor.execute(this.spkRes.borrowSpeakTask(false));
		}
		else if (Protocol.Stream == protocol) {
			// 入队
			this.receivedStreamQueue.offer(message);

			// 异步执行
			this.executor.execute(this.spkRes.borrowStreamTask());
		}
		else if (Protocol.SpeakAck == protocol) {
			// 入队
			this.receivedWithAckQueue.offer(message);

			// 异步执行
			this.executor.execute(this.spkRes.borrowSpeakTask(true));
		}
		else if (Protocol.Ack == protocol) {
			// 异步执行
			this.executor.execute(this.spkRes.borrowAckTask(session, message));
		}
		else if (Protocol.Heartbeat == protocol) {
			long timestamp = System.currentTimeMillis();
			// 异步执行
			this.executor.execute(this.spkRes.borrowHeartbeatTask(this.hm, session, message.getPayload(), timestamp));
		}
		else if (Protocol.HeartbeatAck == protocol) {
			// 异步执行
			this.executor.execute(this.spkRes.borrowHeartbeatAckTask(this.hm, session, message.getPayload()));
		}
		else if (Protocol.Handshake == protocol) {
			this.executor.execute(new Runnable() {
				@Override
				public void run() {
					processHandshake(session, message);
				}
			});
		}
		else if (Protocol.Goodbye == protocol) {
			// TODO
		}
		else {
			Logger.w(this.getClass(), "Unknown protocol");
		}
	}

	@Override
	public void messageSent(Session session, Message message) {
		if (null == session.getSecretKey()) {
			this.session.setSecretKey(this.secretKey);
		}

		byte[] protocol = Protocol.recognize(message.getPayload());
		if (Protocol.SpeakNoAck == protocol || Protocol.SpeakAck == protocol) {
			// 入队
			this.sentQueue.offer(message);

			// 异步执行
			this.executor.execute(this.spkRes.borrowFireSpokeTask());
		}
	}

	@Override
	public void errorOccurred(final int errorCode, final Session session) {
		if (Logger.isDebugLevel()) {
			Logger.d(this.getClass(), "Error: " + errorCode);
		}

		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				TalkError fault = TalkError.transformError(errorCode);
				listener.onFailed(Speaker.this, fault);
			}
		});
	}

	@Override
	public void onSessionTimeout(Session session, long duration) {
		// 上报会话超时
		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				TalkError fault = new TalkError(TalkError.HeartbeatTimeout);
				listener.onFailed(Speaker.this, fault);
			}
		});
	}

	@Override
	public void onAckTimeout(final Session session, final String cellet, final Primitive primitive) {
		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				listener.onSpeakTimeout(Speaker.this, cellet, primitive);
			}
		});
	}

	/**
	 * 发送 Ack 应答。
	 *
	 * @param cellet 目标 Cellet 名称。
	 * @param primitive 数据。
	 */
	private void sendAck(String cellet, Primitive primitive) {
		FlexibleByteBuffer buf = Protocol.serializeAck(cellet, primitive.getSN());
		Message message = new Message(buf.array(), 0, buf.limit());
		try {
			this.connector.write(message);
		} catch (IOException e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}
		Protocol.recovery(buf);
	}

	/**
	 * 处理接收到的数据，并回调 Listened 事件。
	 *
	 * @param ack 是否需要给服务器进行应答。
	 */
	protected void processSpeak(boolean ack) {
		Message message = ack ? this.receivedWithAckQueue.poll() : this.receivedWithoutAckQueue.poll();
		if (null != message) {
			SpeakProtocol protocol = Protocol.deserializeSpeak(message.getPayload());
			protocol.ack = ack;

			if (ack) {
				// 发送应答
				sendAck(protocol.target, protocol.primitive);
			}

			this.listener.onListened(this, protocol.target, protocol.primitive);

			protocol.destroy();
		}
		else {
			// TODO 错误处理
			Logger.e(this.getClass(), "#processSpeak Message is null");
		}
	}

	/**
	 * 处理接收到的流数据，并回调 Listened 事件。
	 */
	protected void processSpeakStream() {
		// TODO
	}

	/**
	 * 处理接收到原语应答。
	 *
	 * @param session 消息层的会话上下文。
	 * @param message 原始消息。
	 */
	protected void processAck(Session session, Message message) {
		AckProtocol protocol = Protocol.deserializeAck(message.getPayload());
		Primitive primitive = null;

		// 从 SpokeList 中移除
		PrimitiveCapsule pc = this.spokeList.get(session.getId());
		if (null != pc) {
			primitive = pc.removePrimitive(protocol.target, protocol.sn);
		}

		if (null != primitive) {
			this.listener.onAck(this, protocol.target, primitive);
		}

		protocol.destroy();
	}

	/**
	 * 处理接收到的握手回复。
	 *
	 * @param session 消息层的会话上下文。
	 * @param message 原始消息。
	 */
	private void processHandshake(Session session, Message message) {
		// 握手成功启动心跳
		this.hm.startup();

		synchronized (this) {
			if (null != this.handshakeTimer) {
				this.handshakeTimer.cancel();
				this.handshakeTimer.purge();
				this.handshakeTimer = null;
			}

			this.state = State.Connected;
		}

		HandshakeProtocol protocol = Protocol.deserializeHandshake(message.getPayload());
		this.serverTag = protocol.tag.toString();

		this.listener.onContacted(null, this);

		protocol.destroy();
	}

	/**
	 * 处理原语已发送事件。
	 */
	protected void processSpoke() {
		synchronized (this.sentQueue) {
			Message msg = this.sentQueue.poll();
			if (null != msg) {
				MessageContext ctx = (MessageContext) msg.getContext();
				this.listener.onSpoke(this, ctx.cellet, ctx.primitive);
				msg.setContext(null);
			}
		}
	}

	/**
	 * 消息上下文。
	 */
	private class MessageContext {
		protected String cellet;
		protected Primitive primitive;

		protected MessageContext(String cellet, Primitive primitive) {
			this.cellet = cellet;
			this.primitive = primitive;
		}
	}

}
