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

import java.util.concurrent.ConcurrentLinkedQueue;

import cell.core.net.Message;
import cell.core.net.Session;

/**
 * 用于优化 Speaker 内存管理的缓存数据对象。
 */
public class SpeakerRes {

	/** 对应的 Speaker 实例。 */
	private Speaker speaker;

	/** 缓存接收原语任务。 */
	private ConcurrentLinkedQueue<SpeakTask> speakTaskQueue;

	/** 缓存接收流任务。 */
	private ConcurrentLinkedQueue<StreamTask> streamTaskQueue;

	/** 缓存接收原语应答任务。 */
	private ConcurrentLinkedQueue<AckTask> ackTaskQueue;

	/** 缓存心跳任务。 */
	private ConcurrentLinkedQueue<HeartbeatTask> heartbeatTaskQueue;

	/** 缓存心跳应答任务。 */
	private ConcurrentLinkedQueue<HeartbeatAckTask> heartbeatAckTaskQueue;

	/** 缓存触发 Spoke 事件任务。 */
	private ConcurrentLinkedQueue<FireSpokeTask> fireSpokeTaskQueue;

	/**
	 * 构造函数。
	 *
	 * @param speaker 关联的客户端。
	 */
	public SpeakerRes(Speaker speaker) {
		this.speaker = speaker;
		this.speakTaskQueue = new ConcurrentLinkedQueue<SpeakTask>();
		this.streamTaskQueue = new ConcurrentLinkedQueue<StreamTask>();
		this.ackTaskQueue = new ConcurrentLinkedQueue<AckTask>();
		this.heartbeatTaskQueue = new ConcurrentLinkedQueue<HeartbeatTask>();
		this.heartbeatAckTaskQueue = new ConcurrentLinkedQueue<HeartbeatAckTask>();
		this.fireSpokeTaskQueue = new ConcurrentLinkedQueue<FireSpokeTask>();
	}

	/**
	 * 借出接收原语任务。
	 *
	 * @param ack
	 * @return
	 */
	public SpeakTask borrowSpeakTask(boolean ack) {
		SpeakTask task = this.speakTaskQueue.poll();

		if (null == task) {
			return new SpeakTask(ack);
		}
		else {
			task.ack = ack;
		}

		return task;
	}

	/**
	 * 归还接收原语任务。
	 *
	 * @param task
	 */
	private void returnSpeakTask(SpeakTask task) {
		this.speakTaskQueue.offer(task);
	}

	/**
	 * 借出接收流任务。
	 *
	 * @return
	 */
	public StreamTask borrowStreamTask() {
		StreamTask task = this.streamTaskQueue.poll();
		if (null == task) {
			return new StreamTask();
		}

		return task;
	}

	/**
	 * 归还接收流任务。
	 * 
	 * @param task
	 */
	private void returnStreamTask(StreamTask task) {
		this.streamTaskQueue.offer(task);
	}

	/**
	 * 借出接收原语应答任务。
	 * 
	 * @param session
	 * @param message
	 * @return
	 */
	public AckTask borrowAckTask(Session session, Message message) {
		AckTask task = this.ackTaskQueue.poll();
		if (null == task) {
			return new AckTask(session, message);
		}

		task.session = session;
		task.message = message;
		return task;
	}

	/**
	 * 归还接收原语应答任务。
	 * 
	 * @param task
	 */
	private void returnAckTask(AckTask task) {
		task.session = null;
		task.message = null;
		this.ackTaskQueue.offer(task);
	}

	/**
	 * 借出接收心跳任务。
	 *
	 * @param hm
	 * @param session
	 * @param data
	 * @param timestamp
	 * @return
	 */
	public HeartbeatTask borrowHeartbeatTask(HeartbeatMachine hm, Session session, byte[] data, long timestamp) {
		HeartbeatTask task = this.heartbeatTaskQueue.poll();
		if (null == task) {
			return new HeartbeatTask(hm, session, data, timestamp);
		}

		task.hm = hm;
		task.session = session;
		task.data = data;
		task.timestamp = timestamp;
		return task;
	}

	/**
	 * 归还接收心跳任务。
	 * 
	 * @param task
	 */
	private void returnHeartbeatTask(HeartbeatTask task) {
		task.hm = null;
		task.session = null;
		task.data = null;
		task.timestamp = 0;
		this.heartbeatTaskQueue.offer(task);
	}

	/**
	 * 借出接收心跳应答任务。
	 * 
	 * @param hm
	 * @param session
	 * @param data
	 * @return
	 */
	public HeartbeatAckTask borrowHeartbeatAckTask(HeartbeatMachine hm, Session session, byte[] data) {
		HeartbeatAckTask task = this.heartbeatAckTaskQueue.poll();
		if (null == task) {
			return new HeartbeatAckTask(hm, session, data);
		}

		task.hm = hm;
		task.session = session;
		task.data = data;
		return task;
	}

	/**
	 * 归还接收心跳应答任务。
	 * 
	 * @param task
	 */
	private void returnHeartbeatAckTask(HeartbeatAckTask task) {
		task.hm = null;
		task.session = null;
		task.data = null;
		this.heartbeatAckTaskQueue.offer(task);
	}

	/**
	 * 借出 Spoke 事件任务。
	 * 
	 * @return
	 */
	public FireSpokeTask borrowFireSpokeTask() {
		FireSpokeTask task = this.fireSpokeTaskQueue.poll();
		if (null == task) {
			return new FireSpokeTask();
		}

		return task;
	}

	/**
	 * 归还 Spoke 事件任务。
	 * 
	 * @param task
	 */
	private void returnFireSpokeTask(FireSpokeTask task) {
		this.fireSpokeTaskQueue.offer(task);
	}


	/**
	 * 接收处理原语任务。
	 */
	public class SpeakTask implements Runnable {

		protected boolean ack;

		public SpeakTask(boolean ack) {
			this.ack = ack;
		}

		@Override
		public void run() {
			// 执行 Speaker 处理
			speaker.processSpeak(this.ack);

			returnSpeakTask(this);
		}
	}

	/**
	 * 接收处理流任务。
	 */
	public class StreamTask implements Runnable {

		public StreamTask() {
		}

		@Override
		public void run() {
			// 执行处理流
			speaker.processSpeakStream();

			returnStreamTask(this);
		}
	}


	/**
	 * 接收处理原语应答任务。
	 */
	public class AckTask implements Runnable {
		protected Session session;
		protected Message message;

		public AckTask(Session session, Message message) {
			this.session = session;
			this.message = message;
		}

		@Override
		public void run() {
			speaker.processAck(this.session, this.message);

			returnAckTask(this);
		}
	}


	/**
	 * 接收处理心跳任务。
	 */
	public class HeartbeatTask implements Runnable {
		protected HeartbeatMachine hm;
		protected Session session;
		protected byte[] data;
		protected long timestamp;

		public HeartbeatTask(HeartbeatMachine hm, Session session, byte[] data, long timestamp) {
			this.hm = hm;
			this.session = session;
			this.data = data;
			this.timestamp = timestamp;
		}

		@Override
		public void run() {
			this.hm.processHeartbeat(this.session, this.data, this.timestamp);

			returnHeartbeatTask(this);
		}
	}

	/**
	 * 接收处理心跳应答任务。
	 */
	public class HeartbeatAckTask implements Runnable {
		protected HeartbeatMachine hm;
		protected Session session;
		protected byte[] data;

		public HeartbeatAckTask(HeartbeatMachine hm, Session session, byte[] data) {
			this.hm = hm;
			this.session = session;
			this.data = data;
		}

		@Override
		public void run() {
			this.hm.processHeartbeatAck(this.session, this.data);

			returnHeartbeatAckTask(this);
		}
	}

	/**
	 * 触发 Spoke 事件。
	 */
	public class FireSpokeTask implements Runnable {
		public FireSpokeTask() {
		}

		@Override
		public void run() {
			speaker.processSpoke();

			returnFireSpokeTask(this);
		}
	}

}
