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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import cell.core.net.Message;
import cell.core.net.MessageHandler;
import cell.core.net.Session;
import cell.core.talk.Protocol.HandshakeProtocol;
import cell.core.talk.Protocol.SpeakProtocol;
import cell.core.talk.Protocol.StreamProtocol;
import cell.util.CachedQueueExecutor;
import cell.util.log.Logger;

/**
 * Talk 服务器的接收处理器。
 */
public class AcceptorHandler implements MessageHandler {

	/** 关联服务器。 */
	private BaseServer server;

	/** 线程池执行器。 */
	private ExecutorService executor;

	/** 维护任务定时器，用于维护超时会话等。 */
	private Timer timer;

	/** 会话建立时的时间戳。 */
	private ConcurrentHashMap<Session, Long> sessionTimestampMap;

	/** 握手任务队列。 */
	private ConcurrentLinkedQueue<HandshakeTask> handshakeTaskQueue;

	/** Listened 事件任务队列。 */
	private ConcurrentLinkedQueue<FireListenedTask> fireListenedTaskQueue;

	/** Listened 流事件任务队列。 */
	private ConcurrentLinkedQueue<FireListenedStreamTask> fireListenedStreamTaskQueue;

	/** Spoke 事件任务队列。 */
	private ConcurrentLinkedQueue<FireSpokeTask> fireSpokeTaskQueue;

	/** Ack 事件任务队列。 */
	private ConcurrentLinkedQueue<FireAckTask> fireAckTaskQueue;

	/** 心跳事件任务队列。 */
	private ConcurrentLinkedQueue<HeartbeatTask> hbTaskQueue;

	/** 心跳应答时间任务队列。 */
	private ConcurrentLinkedQueue<HeartbeatAckTask> hbAckTaskQueue;

	/** 接收 SpeakProtocol 队列。 */
	private ConcurrentHashMap<Long, ConcurrentLinkedQueue<SpeakProtocol>> receivedSpeakProtocol;

	/** 接收 StreamProtocol 队列。 */
	private ConcurrentHashMap<Long, ConcurrentLinkedQueue<StreamProtocol>> receivedStreamProtocol;

	/** 发送 MessageContext 队列。 */
	private ConcurrentHashMap<Long, ConcurrentLinkedQueue<MessageContext>> sentMessageContext;

	/**
	 * 构造函数。
	 *
	 * @param server 服务器实例。
	 * @param maxThreads 最大允许启用的工作线程数量。
	 */
	public AcceptorHandler(BaseServer server, int maxThreads) {
		this.server = server;
		this.executor = CachedQueueExecutor.newCachedQueueThreadPool(maxThreads);
		this.handshakeTaskQueue = new ConcurrentLinkedQueue<HandshakeTask>();
		this.fireListenedTaskQueue = new ConcurrentLinkedQueue<FireListenedTask>();
		this.fireListenedStreamTaskQueue = new ConcurrentLinkedQueue<FireListenedStreamTask>();
		this.fireSpokeTaskQueue = new ConcurrentLinkedQueue<FireSpokeTask>();
		this.fireAckTaskQueue = new ConcurrentLinkedQueue<FireAckTask>();
		this.hbTaskQueue = new ConcurrentLinkedQueue<HeartbeatTask>();
		this.hbAckTaskQueue = new ConcurrentLinkedQueue<HeartbeatAckTask>();
		this.sessionTimestampMap = new ConcurrentHashMap<Session, Long>();
		this.receivedSpeakProtocol = new ConcurrentHashMap<Long, ConcurrentLinkedQueue<SpeakProtocol>>();
		this.receivedStreamProtocol = new ConcurrentHashMap<Long, ConcurrentLinkedQueue<StreamProtocol>>();
		this.sentMessageContext = new ConcurrentHashMap<Long, ConcurrentLinkedQueue<MessageContext>>();
		this.timer = new Timer();
		this.timer.schedule(new TimeoutTask(), 10000L, 15000L);
	}

	/**
	 * 关闭服务器。
	 */
	public void shutdown() {
		this.timer.cancel();
		this.timer.purge();

		this.executor.shutdown();

		this.handshakeTaskQueue.clear();
		this.fireListenedTaskQueue.clear();
		this.fireListenedStreamTaskQueue.clear();
		this.fireSpokeTaskQueue.clear();
		this.fireAckTaskQueue.clear();
		this.hbTaskQueue.clear();
		this.hbAckTaskQueue.clear();
		this.receivedSpeakProtocol.clear();
		this.receivedStreamProtocol.clear();
		this.sentMessageContext.clear();

		this.sessionTimestampMap.clear();
	}

	/**
	 * 重置允许并发的最大线程数量。
	 *
	 * @param maxThreads 最大线程数量。
	 */
	public void resetMaxThreads(int maxThreads) {
		((CachedQueueExecutor) this.executor).resetMaxThreads(maxThreads);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionCreated(Session session) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionDestroyed(Session session) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionOpened(Session session) {
		if (Logger.isDebugLevel()) {
			Logger.d(this.getClass(), "sessionOpened");
		}

		this.sessionTimestampMap.put(session, System.currentTimeMillis());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sessionClosed(final Session session) {
		if (Logger.isDebugLevel()) {
			Logger.d(this.getClass(), "sessionClosed");
		}

		this.sessionTimestampMap.remove(session);

		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				server.fireQuitted(session);
			}
		});

		ConcurrentLinkedQueue<SpeakProtocol> sp = this.receivedSpeakProtocol.remove(session.getId());
		if (null != sp) {
			sp.clear();
			sp = null;
		}

		ConcurrentLinkedQueue<StreamProtocol> stp = this.receivedStreamProtocol.remove(session.getId());
		if (null != stp) {
			stp.clear();
			stp = null;
		}

		ConcurrentLinkedQueue<MessageContext> mc = this.sentMessageContext.remove(session.getId());
		if (null != mc) {
			mc.clear();
			mc = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void messageReceived(Session session, Message message) {
		byte[] data = message.getPayload();

		// 识别
		byte[] protocol = Protocol.recognize(data);
		if (Protocol.SpeakNoAck == protocol) {
			this.processSpeakNoAck(session, data);
		}
		else if (Protocol.Stream == protocol) {
			this.processStream(session, data);
		}
		else if (Protocol.SpeakAck == protocol) {
			this.processSpeakAck(session, data);
		}
		else if (Protocol.Ack == protocol) {
			this.processAck(session, data);
		}
		else if (Protocol.Heartbeat == protocol) {
			this.processHeartbeat(session, data, System.currentTimeMillis());
		}
		else if (Protocol.HeartbeatAck == protocol) {
			this.processHeartbeatAck(session, data);
		}
		else if (Protocol.Handshake == protocol) {
			this.processHandshake(session, data);
		}
		else {
			this.processUnknown(session);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void messageSent(Session session, Message message) {
		Object ctx = message.getContext();
		if (null != ctx && ctx instanceof MessageContext) {
			MessageContext context = (MessageContext) ctx;

			ConcurrentLinkedQueue<MessageContext> queue = this.sentMessageContext.get(session.getId());
			if (null == queue) {
				queue = new ConcurrentLinkedQueue<MessageContext>();
				this.sentMessageContext.put(session.getId(), queue);
			}
			queue.offer(context);

			FireSpokeTask task = this.borrowFireSpokeTask(session);

			this.executor.execute(task);

			message.setContext(null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void errorOccurred(final int errorCode, final Session session) {
		this.executor.execute(new Runnable() {
			@Override
			public void run() {
				TalkError fault = TalkError.transformError(errorCode);
				server.fireFault(fault, session);
			}
		});

	}

	/**
	 * 处理接收到的数据。
	 *
	 * @param session 会话上下文。
	 * @param data 接收到的数据。
	 */
	private void processSpeakAck(Session session, byte[] data) {
		SpeakProtocol protocol = Protocol.deserializeSpeak(data);
		// 标记为应答
		protocol.ack = true;

		ConcurrentLinkedQueue<SpeakProtocol> queue = this.receivedSpeakProtocol.get(session.getId());
		if (null == queue) {
			queue = new ConcurrentLinkedQueue<SpeakProtocol>();
			this.receivedSpeakProtocol.put(session.getId(), queue);
		}
		queue.offer(protocol);

		// 借对象
		FireListenedTask task = this.borrowFireListenedTask(session);
		// 执行任务
		this.executor.execute(task);
	}

	/**
	 * 处理接收到的无需应答的数据。
	 *
	 * @param session 会话上下文。
	 * @param data 接收到的数据。
	 */
	private void processSpeakNoAck(Session session, byte[] data) {
		SpeakProtocol protocol = Protocol.deserializeSpeak(data);
		// 标记为不应答
		protocol.ack = false;

		ConcurrentLinkedQueue<SpeakProtocol> queue = this.receivedSpeakProtocol.get(session.getId());
		if (null == queue) {
			queue = new ConcurrentLinkedQueue<SpeakProtocol>();
			this.receivedSpeakProtocol.put(session.getId(), queue);
		}
		queue.offer(protocol);

		// 借对象
		FireListenedTask task = this.borrowFireListenedTask(session);
		// 执行任务
		this.executor.execute(task);
	}

	/**
	 * 处理接收到的 ACK 应答数据。
	 *
	 * @param session 会话上下文。
	 * @param data 接收到的数据。
	 */
	private void processAck(Session session, byte[] data) {
		// 执行任务
		this.executor.execute(this.borrowFireAckTask(session, data));
	}

	/**
	 * 处理接收流数据。
	 *
	 * @param session 会话上下文。
	 * @param data 接收到数据。
	 */
	private void processStream(Session session, byte[] data) {
		StreamProtocol protocol = Protocol.deserializeStream(data);

		ConcurrentLinkedQueue<StreamProtocol> queue = this.receivedStreamProtocol.get(session.getId());
		if (null == queue) {
			queue = new ConcurrentLinkedQueue<StreamProtocol>();
			this.receivedStreamProtocol.put(session.getId(), queue);
		}
		queue.offer(protocol);

		// 借对象
		FireListenedStreamTask task = this.borrowFireListenedStreamTask(session);
		// 执行任务
		this.executor.execute(task);
	}

	/**
	 * 处理接收到的握手数据。
	 *
	 * @param session 会话上下文。
	 * @param data 接收到的数据。
	 */
	private void processHandshake(Session session, byte[] data) {
		if (Logger.isDebugLevel()) {
			Logger.d(this.getClass(), "processHandshake" + session.toString());
		}

		HandshakeProtocol handshake = Protocol.deserializeHandshake(data);
		if (null == handshake.key || null == handshake.tag) {
			Logger.w(this.getClass(),
					"Deserialize handshake protocol error " + session.toString() + "(" + session.getId() + ")");
			return;
		}

		// 借对象
		HandshakeTask task = this.borrowHandshakeTask(session, handshake);
		// 执行任务
		this.executor.execute(task);
	}

	/**
	 * 处理接收到心跳数据。
	 *
	 * @param session 会话上下文。
	 * @param data 接收到的数据。
	 * @param timestamp 接收数据时的时间戳。
	 */
	private void processHeartbeat(Session session, byte[] data, long timestamp) {
		// 执行任务
		this.executor.execute(this.borrowHeartbeatTask(session, data, timestamp));
	}

	/**
	 * 处理接收到心跳应答数据。
	 *
	 * @param session 会话上下问。
	 * @param data 接收到的数据。
	 */
	private void processHeartbeatAck(Session session, byte[] data) {
		// 执行任务
		this.executor.execute(this.borrowHeartbeatAckTask(session, data));
	}

	/**
	 * 处理未知数据。
	 *
	 * @param session 会话上下文。
	 */
	private void processUnknown(Session session) {
		Logger.e(this.getClass(), "Unknown protocol, session: " + session.toString());
	}

	/**
	 * 借出握手任务。
	 *
	 * @param session 会话上下文。
	 * @param protocol 握手协议。
	 * @return 返回握手任务实例。
	 */
	private HandshakeTask borrowHandshakeTask(Session session, HandshakeProtocol protocol) {
		HandshakeTask task = this.handshakeTaskQueue.poll();
		if (null == task) {
			return new HandshakeTask(session, protocol);
		}

		task.reset(session, protocol);
		return task;
	}

	/**
	 * 归还握手任务。
	 *
	 * @param task 握手任务实例。
	 */
	private void returnHandshakeTask(HandshakeTask task) {
		task.clean();
		this.handshakeTaskQueue.offer(task);
	}

	/**
	 * 借出触发 Listened 事件任务。
	 *
	 * @param session 会话上下文。
	 * @return 触发 Listened 的事件任务。
	 */
	private FireListenedTask borrowFireListenedTask(Session session) {
		FireListenedTask task = this.fireListenedTaskQueue.poll();
		if (null == task) {
			return new FireListenedTask(session);
		}

		task.reset(session);
		return task;
	}

	/**
	 * 归还 Listened 事件任务。
	 *
	 * @param task 触发 Listened 的事件任务。
	 */
	private void returnFireListenedTask(FireListenedTask task) {
		task.clean();
		this.fireListenedTaskQueue.offer(task);
	}

	/**
	 * 借出触发 Listened Stream 事件任务。
	 *
	 * @param session 会话上下文。
	 * @return 触发 Listened Stream 的事件任务。
	 */
	private FireListenedStreamTask borrowFireListenedStreamTask(Session session) {
		FireListenedStreamTask task = this.fireListenedStreamTaskQueue.poll();
		if (null == task) {
			return new FireListenedStreamTask(session);
		}

		task.reset(session);
		return task;
	}

	/**
	 * 归还 Listened Stream 事件任务。
	 *
	 * @param task 触发 Listened Stream 的事件任务。
	 */
	private void returnFireListenedStreamTask(FireListenedStreamTask task) {
		task.clean();
		this.fireListenedStreamTaskQueue.offer(task);
	}

	/**
	 * 借出 Spoke 事件任务。
	 *
	 * @param session 会话上下文。
	 * @return 触发 Spoke 的事件任务。
	 */
	private FireSpokeTask borrowFireSpokeTask(Session session) {
		FireSpokeTask task = this.fireSpokeTaskQueue.poll();
		if (null == task) {
			return new FireSpokeTask(session);
		}

		task.reset(session);
		return task;
	}

	/**
	 * 归还 Spoke 事件任务。
	 *
	 * @param task 触发 Spoke 的事件任务。
	 */
	private void returnFireSpokeTask(FireSpokeTask task) {
		task.clean();
		this.fireSpokeTaskQueue.offer(task);
	}

	/**
	 * 借出 Ack 事件任务。
	 *
	 * @param session 会话上下文。
	 * @param data 数据。
	 * @return 触发 Ack 的事件任务。
	 */
	private FireAckTask borrowFireAckTask(Session session, byte[] data) {
		FireAckTask task = this.fireAckTaskQueue.poll();
		if (null == task) {
			return new FireAckTask(session, data);
		}

		task.reset(session, data);
		return task;
	}

	/**
	 * 归还 Ack 事件任务。
	 *
	 * @param task 触发 Ack 的事件任务。
	 */
	private void returnFireAckTask(FireAckTask task) {
		task.clean();
		this.fireAckTaskQueue.offer(task);
	}

	/**
	 * 借出心跳事件任务。
	 *
	 * @param session 会话上下文。
	 * @param data 数据。
	 * @param timestamp 接收数据时的时间戳。
	 * @return 心跳事件任务。
	 */
	private HeartbeatTask borrowHeartbeatTask(Session session, byte[] data, long timestamp) {
		HeartbeatTask task = this.hbTaskQueue.poll();
		if (null == task) {
			return new HeartbeatTask(session, data, timestamp);
		}

		task.reset(session, data, timestamp);
		return task;
	}

	/**
	 * 归还心跳事件任务。
	 *
	 * @param task 心跳事件任务。
	 */
	private void returnHeartbeatTask(HeartbeatTask task) {
		task.clean();
		this.hbTaskQueue.offer(task);
	}

	/**
	 * 借出心跳应答事件任务。
	 *
	 * @param session 会话上下文。
	 * @param data 数据。
	 * @return 心跳应答事件任务。
	 */
	private HeartbeatAckTask borrowHeartbeatAckTask(Session session, byte[] data) {
		HeartbeatAckTask task = this.hbAckTaskQueue.poll();
		if (null == task) {
			return new HeartbeatAckTask(session, data);
		}

		task.reset(session, data);
		return task;
	}

	/**
	 * 归还心跳应答事件任务。
	 *
	 * @param task 心跳应答事件任务。
	 */
	private void returnHeartbeatAckTask(HeartbeatAckTask task) {
		task.clean();
		this.hbAckTaskQueue.offer(task);
	}

	/**
	 * 关闭超时的 Session 的定时任务。
	 */
	protected class TimeoutTask extends TimerTask {

		private final long timeout = 30L * 1000L;

		protected TimeoutTask() {
		}

		@Override
		public void run() {
			long time = System.currentTimeMillis();

			ArrayList<Session> sessionList = new ArrayList<Session>();

			Iterator<Map.Entry<Session, Long>> iter = sessionTimestampMap.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<Session, Long> e = iter.next();
				Session session = e.getKey();
				Long timestamp = e.getValue();
				if (null == session.getSecretKey()) {
					if ((time - timestamp.longValue()) > this.timeout) {
						sessionList.add(session);
						iter.remove();
					}
				} else {
					iter.remove();
				}
			}

			for (Session session : sessionList) {
				Logger.i(getClass(), "Session '" + session.toString() + "' timeout");
				server.processTimeout(session);
			}
		}

	}

	/**
	 * 执行握手任务。
	 */
	protected class HandshakeTask implements Runnable {

		private Session session;
		private HandshakeProtocol protocol;

		public HandshakeTask(Session session, HandshakeProtocol protocol) {
			this.session = session;
			this.protocol = protocol;
		}

		public void reset(Session session, HandshakeProtocol protocol) {
			this.session = session;
			this.protocol = protocol;
		}

		public void clean() {
			this.session = null;
			this.protocol = null;
		}

		@Override
		public void run() {
			server.processHandshake(this.session, this.protocol);

			// 还对象
			returnHandshakeTask(this);
		}
	}

	/**
	 * 回调接收到原语数据。
	 */
	protected class FireListenedTask implements Runnable {

		private Session session;

		public FireListenedTask(Session session) {
			this.session = session;
		}

		public void reset(Session session) {
			this.session = session;
		}

		public void clean() {
			this.session = null;
		}

		@Override
		public void run() {
			synchronized (this.session) {
				ConcurrentLinkedQueue<SpeakProtocol> queue = receivedSpeakProtocol.get(this.session.getId());
				if (null != queue) {
					SpeakProtocol protocol = queue.poll();
					if (null != protocol) {
						server.fireListened(this.session, protocol);
					}
				}
			}

			// 还对象
			returnFireListenedTask(this);
		}
	}

	/**
	 * 回调接收到原语流。
	 */
	protected class FireListenedStreamTask implements Runnable {

		private Session session;

		public FireListenedStreamTask(Session session) {
			this.session = session;
		}

		public void reset(Session session) {
			this.session = session;
		}

		public void clean() {
			this.session = null;
		}

		@Override
		public void run() {
			synchronized (this.session) {
				ConcurrentLinkedQueue<StreamProtocol> queue = receivedStreamProtocol.get(this.session.getId());
				if (null != queue) {
					StreamProtocol protocol = queue.poll();
					if (null != protocol) {
						server.fireListened(this.session, protocol);
					}
				}
			}

			// 还对象
			returnFireListenedStreamTask(this);
		}
	}

	/**
	 * 回调发出原语数据。
	 */
	protected class FireSpokeTask implements Runnable {

		private Session session;

		public FireSpokeTask(Session session) {
			this.session = session;
		}

		public void reset(Session session) {
			this.session = session;
		}

		public void clean() {
			this.session = null;
		}

		@Override
		public void run() {
			synchronized (this.session) {
				ConcurrentLinkedQueue<MessageContext> queue = sentMessageContext.get(session.getId());
				if (null != queue) {
					MessageContext context = queue.poll();
					if (null != context) {
						server.fireSpoke(this.session, context);
					}
				}
			}

			returnFireSpokeTask(this);
		}
	}

	/**
	 * 回调 ACK 应答。该任务无需按序执行。
	 */
	protected class FireAckTask implements Runnable {

		private Session session;
		private byte[] data;

		public FireAckTask(Session session, byte[] data) {
			this.session = session;
			this.data = data;
		}

		public void reset(Session session, byte[] data) {
			this.session = session;
			this.data = data;
		}

		public void clean() {
			this.session = null;
			this.data = null;
		}

		@Override
		public void run() {
			server.fireAck(this.session, this.data);

			returnFireAckTask(this);
		}
	}

	/**
	 * 执行心跳，该任务不需要按照顺序执行。
	 */
	protected class HeartbeatTask implements Runnable {

		private Session session;
		private byte[] data;
		private long timestamp;

		private HeartbeatTask(Session session, byte[] data, long timestamp) {
			this.session = session;
			this.data = data;
			this.timestamp = timestamp;
		}

		public void reset(Session session, byte[] data, long timestamp) {
			this.session = session;
			this.data = data;
			this.timestamp = timestamp;
		}

		public void clean() {
			this.session = null;
			this.data = null;
			this.timestamp = 0;
		}

		@Override
		public void run() {
			server.heartbeatMachine.processHeartbeat(this.session, this.data, this.timestamp);

			returnHeartbeatTask(this);
		}
	}

	/**
	 * 执行心跳应答任务。
	 */
	protected class HeartbeatAckTask implements Runnable {

		private Session session;
		private byte[] data;

		private HeartbeatAckTask(Session session, byte[] data) {
			this.session = session;
			this.data = data;
		}

		public void reset(Session session, byte[] data) {
			this.session = session;
			this.data = data;
		}

		public void clean() {
			this.session = null;
			this.data = null;
		}

		@Override
		public void run() {
			server.heartbeatMachine.processHeartbeatAck(this.session, this.data);

			returnHeartbeatAckTask(this);
		}
	}

}
