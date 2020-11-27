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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import cell.core.net.Message;
import cell.core.net.MessageAcceptor;
import cell.core.net.MessageConnector;
import cell.core.net.Session;
import cell.core.talk.Protocol.HeartbeatProtocol;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.LogLevel;
import cell.util.log.Logger;

/**
 * 心跳协议管理器。
 */
public class HeartbeatMachine {

	/** 执行心跳任务的时间间隔。 */
	private long hbInterval = 3L * 60L * 1000L;

	/** 每个心跳的有效时长，超过该时长认为对方无应答。 */
	private long hbDuration = 10L * 1000L;

	/** 当前批次定时任务是否正在运行。 */
	private AtomicBoolean timerRunning = new AtomicBoolean(false);

	/** 任务定时器。 */
	private Timer daemon;

	/** 消息连接器。 */
	private MessageConnector connector;

	/** 消息接收器。 */
	private MessageAcceptor acceptor;

	/** Session ID 上下文。 */
	private ConcurrentHashMap<Long, HeartbeatContext> contextMap;

	/** 存储当前正在执行心跳的上下文。 */
	private ConcurrentLinkedQueue<HeartbeatContext> currentQueue;

	/** 允许的最大队列长度。 */
	private int maxQueueSize = 300;

	/** 监听器。 */
	private HeartbeatMachineListener listener;

	/**
	 * 构造函数。
	 *
	 * @param connector 用于客户端的消息连接器。
	 */
	public HeartbeatMachine(MessageConnector connector) {
		this.connector = connector;
		this.contextMap = new ConcurrentHashMap<Long, HeartbeatContext>();
		this.currentQueue = new ConcurrentLinkedQueue<HeartbeatContext>();
	}

	/**
	 * 构造函数。
	 *
	 * @param acceptor 用于服务器的消息接收器。
	 */
	public HeartbeatMachine(MessageAcceptor acceptor) {
		this.acceptor = acceptor;
		this.contextMap = new ConcurrentHashMap<Long, HeartbeatContext>();
		this.currentQueue = new ConcurrentLinkedQueue<HeartbeatContext>();
	}

	/**
	 * 重置连接器。
	 *
	 * @param connector 指定新的连接器。
	 */
	public void resetConnector(MessageConnector connector) {
		this.connector = connector;
	}

	/**
	 * 启动。
	 */
	public void startup() {
		if (null == this.daemon) {
			this.daemon = new Timer();
			this.daemon.schedule(new Daemon(), 30L * 1000L, 10L * 1000L);
		}
	}

	/**
	 * 关闭。
	 */
	public void shutdown() {
		if (null != this.daemon) {
			this.daemon.cancel();
			this.daemon.purge();
			this.daemon = null;
		}

		this.contextMap.clear();
		this.currentQueue.clear();
	}

	/**
	 * 设置心跳间隔。
	 *
	 * @param interval 心跳间隔，单位：毫秒。
	 */
	public void setInterval(long interval) {
		this.hbInterval = interval;
	}

	/**
	 * 设置监听器。
	 *
	 * @param listener 心跳状态监听器。
	 */
	public void setListener(HeartbeatMachineListener listener) {
		this.listener = listener;
	}

	/**
	 * 添加会话。
	 *
	 * @param session 会话上下文。
	 * @param tag 对端标签。
	 */
	public void addSession(Session session, String tag) {
		if (this.contextMap.containsKey(session.getId())) {
			return;
		}

		this.contextMap.put(session.getId(), new HeartbeatContext(session, tag));
	}

	/**
	 * 移除会话。
	 *
	 * @param session 会话上下文。
	 */
	public void removeSession(Session session) {
		this.contextMap.remove(session.getId());
	}

	/**
	 * 获取指定会话的心跳上下文。
	 *
	 * @param session 会话上下文。
	 * @return 返回会话对应的心跳上下文。
	 */
	public HeartbeatContext getContext(Session session) {
		return this.contextMap.get(session.getId());
	}

	/**
	 * 处理来自对端的心跳数据。
	 *
	 * @param session 消息层的会话上下文。
	 * @param packet 原始报文。
	 * @param time 接收到数据包时的时间。
	 */
	public void processHeartbeat(Session session, byte[] packet, long time) {
		HeartbeatContext ctx = this.contextMap.get(session.getId());
		if (null == ctx) {
			// 错误日志
			Logger.e(this.getClass(), "Process heartbeat error, can not find session (" + session.getId() + "): " + session.toString());
			return;
		}

		// 解析协议
		HeartbeatProtocol protocol = Protocol.deserializeHeartbeat(packet);
		// 进行应答
		FlexibleByteBuffer buf = Protocol.serializeHeartbeatAck(protocol.tag, protocol.originateTimestamp,
				time, System.currentTimeMillis());
		Message message = new Message(buf.array(), 0, buf.limit());
		if (null != this.acceptor) {
			try {
				this.acceptor.write(session, message);
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.WARNING);
			}
		}
		else if (null != this.connector) {
			try {
				this.connector.write(message);
			} catch (IOException e) {
				Logger.log(this.getClass(), e, LogLevel.WARNING);
			}
		}
		Protocol.recovery(buf);

		if (Logger.isDebugLevel()) {
			Logger.d(this.getClass(), "Ack heartbeat to : " + session.toString());
		}
	}

	/**
	 * 处理对端返回的心跳应答。
	 *
	 * @param session 消息层的会话上下文。
	 * @param packet 原始报文。
	 */
	public void processHeartbeatAck(Session session, byte[] packet) {
		HeartbeatContext ctx = this.contextMap.get(session.getId());
		if (null == ctx) {
			Logger.w(this.getClass(), "Can NOT find context: " + session.toString());
			return;
		}

		HeartbeatProtocol protocol = Protocol.deserializeHeartbeatAck(packet);
		if (protocol.originateTimestamp == ctx.originateTimestamp) {
			// 记录时间
			ctx.receiveTimestamp = protocol.receiveTimestamp;
			ctx.transmitTimestamp = protocol.transmitTimestamp;
			ctx.localTimestamp = System.currentTimeMillis();
		}
		else {
			Logger.w(this.getClass(), "Heartbeat context timestamp error: " + session.toString());
		}
	}


	/**
	 * 心跳管理用的上下文。
	 */
	public class HeartbeatContext {

		private Session session;
		private String tag;

		public long originateTimestamp = 0;
		public long receiveTimestamp = 0;
		public long transmitTimestamp = 0;
		public long localTimestamp = 0;

		private HeartbeatContext(Session session, String tag) {
			this.session = session;
			this.tag = tag;
		}
	}


	/**
	 * 检测超时的任务。
	 */
	protected class CheckTask implements Runnable {

		protected CheckTask() {
		}

		@Override
		public void run() {
			try {
				// 延迟执行
				Thread.sleep(hbDuration);
			} catch (InterruptedException e) {
				Logger.log(this.getClass(), e, LogLevel.ERROR);
			}

			// 存储超时的会话
			ArrayList<HeartbeatContext> list = new ArrayList<HeartbeatContext>();

			while (!currentQueue.isEmpty()) {
				HeartbeatContext ctx = currentQueue.poll();
				if (null == ctx) {
					break;
				}

				// 判断接收时间
				if (ctx.localTimestamp == 0) {
					list.add(ctx);
				}
				else if (ctx.localTimestamp < ctx.originateTimestamp) {
					list.add(ctx);
				}
				else if (ctx.localTimestamp - ctx.originateTimestamp > hbDuration) {
					list.add(ctx);
				}
			}

			for (HeartbeatContext ctx : list) {
				long duration = ctx.localTimestamp - ctx.originateTimestamp;
				listener.onSessionTimeout(ctx.session, duration);
			}

			list.clear();
			list = null;

			// 标记运行结束
			timerRunning.set(false);
		}
	}


	/**
	 * 定时执行心跳的任务。
	 */
	protected class Daemon extends TimerTask {

		protected Daemon() {
		}

		@Override
		public void run() {
			if (timerRunning.get()) {
				Logger.d(this.getClass(), "Daemon can not running");
				return;
			}

			if (Logger.isDebugLevel()) {
				Logger.d(this.getClass(), "Daemon is running");
			}

			timerRunning.set(true);

			final long time = System.currentTimeMillis();

			Iterator<HeartbeatContext> iter = contextMap.values().iterator();
			while (iter.hasNext()) {
				HeartbeatContext ctx = iter.next();
				if (ctx.originateTimestamp == 0) {
					currentQueue.offer(ctx);
				}
				else if (time - ctx.originateTimestamp > hbInterval) {
					currentQueue.offer(ctx);
				}

				if (currentQueue.size() >= maxQueueSize) {
					// 达到队列的最大长度
                    break;
                }
			}

			if (currentQueue.isEmpty()) {
				// 没有可执行会话，返回
				timerRunning.set(false);
				return;
			}

			// 发送数据
			Thread task = new Thread() {
				@Override
				public void run() {
					Iterator<HeartbeatContext> iter = currentQueue.iterator();
					while (iter.hasNext()) {
						HeartbeatContext ctx = iter.next();
						if (null == ctx) {
							break;
						}

						// 设置时间
						ctx.originateTimestamp = time;

						FlexibleByteBuffer buf = Protocol.serializeHeartbeat(ctx.tag, ctx.originateTimestamp);
						Message message = new Message(buf.array(), 0, buf.limit());
						if (null != acceptor) {
							try {
								acceptor.write(ctx.session, message);
							} catch (IOException e) {
								// 从当前上下文中移除
								removeSession(ctx.session);
								iter.remove();

								Logger.log(this.getClass(), e, LogLevel.WARNING);
							}
						}
						else if (null != connector) {
							try {
								connector.write(message);
							} catch (IOException e) {
								// 从当前上下文中移除
								removeSession(ctx.session);
								iter.remove();

								Logger.log(this.getClass(), e, LogLevel.WARNING);
							}
						}

						Protocol.recovery(buf);
					}

					// 启动检测任务
					Thread checkTask = new Thread(new CheckTask());
					checkTask.start();
				}
			};
			task.start();
		}
	}

}
