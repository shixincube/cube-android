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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import cell.util.Cryptology;
import cell.util.Utils;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.LogLevel;
import cell.util.log.Logger;

/**
 * 非阻塞网络工作器线程。
 * 
 * 此线程负责处理其关联的会话的数据读、写操作。
 *
 */
public final class NonblockingAcceptorWorker extends Thread {

	/** 控制线程生命周期的条件变量。 */
	private Object mutex = new Object();
	/** 是否处于自旋。 */
	private boolean spinning = false;
	/** 是否正在工作。 */
	private boolean working = false;

	/** 关联的接收器。 */
	private NonblockingAcceptor acceptor;

	/** 需要执行接收数据任务的 Session 列表。 */
	private ConcurrentLinkedQueue<NonblockingAcceptorSession> receiveSessions = new ConcurrentLinkedQueue<NonblockingAcceptorSession>();
	/** 需要执行发送数据任务的 Session 列表。 */
	private ConcurrentLinkedQueue<NonblockingAcceptorSession> sendSessions = new ConcurrentLinkedQueue<NonblockingAcceptorSession>();

	/** 发送数据流量统计。 */
	private long tx = 0;
	/** 接收数据流量统计。 */
	private long rx = 0;

	/** 接收消息时的动态缓存对象池。 */
	private ConcurrentLinkedQueue<FlexibleByteBuffer> byteBufferQueue = new ConcurrentLinkedQueue<FlexibleByteBuffer>();

	/** 接收消息时的数组缓存池。 */
	private ConcurrentLinkedQueue<ArrayList<Message>> msgListQueue = new ConcurrentLinkedQueue<ArrayList<Message>>();

	/**
	 * 构造函数。
	 *
	 * @param acceptor 消息接收器。
	 */
	public NonblockingAcceptorWorker(NonblockingAcceptor acceptor) {
		this.acceptor = acceptor;
		this.setName("NonblockingAcceptorWorker@" + this.toString());
	}

	@Override
	public void run() {
		this.working = true;
		this.spinning = true;
		this.tx = 0;
		this.rx = 0;
		NonblockingAcceptorSession session = null;

		// 虚拟计数
		long virtualTick = 0;

		while (this.spinning) {
			// 如果没有任务，则线程 wait
			if (this.receiveSessions.isEmpty()
				&& this.sendSessions.isEmpty()
				&& this.spinning) {
				synchronized (this.mutex) {
					try {
						this.mutex.wait();
					} catch (InterruptedException e) {
						Logger.log(NonblockingAcceptorWorker.class, e, LogLevel.DEBUG);
					}
				}
			}

			try {
				if (!this.receiveSessions.isEmpty()) {
					// 执行接收数据任务，并移除已执行的 Session
					session = this.receiveSessions.poll();
					long tick = virtualTick - session.readTick;
					if (session.readTick == 0 || tick > 2 || tick < 0) {
						if (null != session.socketChannel) {
							processReceive(session);
							session.readTick = virtualTick;
						}
					}
					else {
						if (!this.receiveSessions.contains(session)) {
							this.receiveSessions.add(session);
						}
					}
				}

				if (!this.sendSessions.isEmpty()) {
					// 执行发送数据任务，并移除已执行的 Session
					session = this.sendSessions.poll();
					long tick = virtualTick - session.writeTick;
					if (session.writeTick == 0 || tick > 2 || tick < 0) {
						if (null != session.socketChannel) {
							int n = processSend(session, 100);
							if (n > 0) {
								if (!this.sendSessions.contains(session)) {
									this.sendSessions.add(session);
								}
							}
							session.writeTick = virtualTick;
						}
					}
					else {
						if (!this.sendSessions.contains(session)) {
							this.sendSessions.add(session);
						}
					}
				}
			} catch (Exception e) {
				Logger.log(this.getClass(), e, LogLevel.WARNING);
			}

			// 虚拟时钟计数
			++virtualTick;
			if (virtualTick >= Long.MAX_VALUE) {
				virtualTick = 0;
			}
		} //#while

		this.working = false;
		this.msgListQueue.clear();
	}

	/**
	 * 获得自系统启动开始的发送流量统计。
	 *
	 * @return 返回以字节为单位的流量统计。
	 */
	protected long getTX() {
		return this.tx;
	}

	/**
	 * 获得自系统启动开始的接收流量统计。
	 *
	 * @return 返回以字节为单位的流量统计。
	 */
	protected long getRX() {
		return this.rx;
	}

	/**
	 * 停止工作线程自旋。
	 *
	 * @param blockingCheck 是否以阻塞方式等待自旋结束。
	 */
	protected void stopSpinning(boolean blockingCheck) {
		this.spinning = false;

		synchronized (this.mutex) {
			this.mutex.notifyAll();
		}

		if (blockingCheck) {
			while (this.working) {
				try {
					Thread.sleep(10L);
				} catch (InterruptedException e) {
					Logger.log(NonblockingAcceptorWorker.class, e, LogLevel.DEBUG);
				}
			}
		}
	}

	/**
	 * 返回线程是否正在工作。
	 *
	 * @return 返回线程是否正在工作。
	 */
	protected boolean isWorking() {
		return this.working;
	}

	/**
	 * 返回当前未处理的接收任务 Session 数量。
	 *
	 * @return 返回当前未处理的接收任务 Session 数量。
	 */
	protected int getReceiveSessionNum() {
		return this.receiveSessions.size();
	}

	/**
	 * 返回当前未处理的发送任务 Session 数量。
	 *
	 * @return 返回当前未处理的发送任务 Session 数量。
	 */
	protected int getSendSessionNum() {
		return this.sendSessions.size();
	}

	/**
	 * 添加执行接收数据的 Session 。
	 *
	 * @param session 指定 Session 对象。
	 */
	protected void pushReceiveSession(NonblockingAcceptorSession session) {
		if (!this.spinning) {
			return;
		}

		if (!this.receiveSessions.contains(session)) {
			this.receiveSessions.add(session);
		}

		synchronized (this.mutex) {
			this.mutex.notify();
		}
	}

	/**
	 * 添加执行发送数据的 Session 。
	 * 
	 * @param session 指定 Session 对象。
	 */
	protected void pushSendSession(NonblockingAcceptorSession session) {
		if (!this.spinning) {
			return;
		}

		// 必须进行此判断
		if (session.isEmptyMessage()) {
			return;
		}

		if (!this.sendSessions.contains(session)) {
			this.sendSessions.add(session);
		}

		synchronized (this.mutex) {
			this.mutex.notify();
		}
	}

	/**
	 * 从所有列表中移除指定的 Session 。
	 * 
	 * @param session 指定 Session 对象。
	 */
	protected void removeSession(NonblockingAcceptorSession session) {
		try {
			boolean exist = this.receiveSessions.remove(session);
			while (exist) {
				exist = this.receiveSessions.remove(session);
			}

			exist = this.sendSessions.remove(session);
			while (exist) {
				exist = this.sendSessions.remove(session);
			}
		} catch (Exception e) {
			Logger.log(this.getClass(), e, LogLevel.WARNING);
		}
	}

	/**
	 * 处理数据接收。
	 *
	 * @param session 关联的 Session 对象。
	 */
	private void processReceive(NonblockingAcceptorSession session) {
		SocketChannel channel = (SocketChannel) session.selectionKey.channel();

		if (!channel.isConnected()) {
			return;
		}

		int totalReaded = 0;
		FlexibleByteBuffer buffer = this.borrowByteBuffer();

		int read = 0;
		do {
			read = 0;

			// 创建读缓存
			FlexibleByteBuffer readbuf = this.borrowByteBuffer();

			synchronized (session) {
				try {
					if (channel.isOpen()) {
						read = channel.read(readbuf.getBuffer());
					}
					else {
						read = -1;
					}
				} catch (IOException e) {
					if (Logger.isDebugLevel()) {
						Logger.d(this.getClass(), "Remote host has closed the connection.");
					}

					if (null != session.socketChannel) {
						this.acceptor.fireSessionClosed(session);
					}

					// 移除 Session
					this.acceptor.eraseSession(session);

					try {
						if (channel.isOpen()) {
							channel.close();
						}
					} catch (IOException ioe) {
						Logger.log(NonblockingAcceptorWorker.class, ioe, LogLevel.DEBUG);
					}

					this.removeSession(session);

					session.selectionKey.cancel();

					this.returnByteBuffer(readbuf);
					this.returnByteBuffer(buffer);

					return;
				}

				if (read == 0) {
					this.returnByteBuffer(readbuf);
					break;
				}
				else if (read == -1) {
					if (null != session.socketChannel) {
						this.acceptor.fireSessionClosed(session);
					}

					// 移除 Session
					this.acceptor.eraseSession(session);

					try {
						if (channel.isOpen()) {
							channel.close();
						}
					} catch (IOException ioe) {
						Logger.log(NonblockingAcceptorWorker.class, ioe, LogLevel.DEBUG);
					}

					this.removeSession(session);

					session.selectionKey.cancel();

					this.returnByteBuffer(readbuf);
					this.returnByteBuffer(buffer);

					return;
				}
			} // #synchronized

			// 计算长度
			totalReaded += read;

			if (readbuf.position() != 0) {
				readbuf.flip();
			}
			// 合并
			buffer.put(readbuf);

			this.returnByteBuffer(readbuf);
		} while (read > 0);

		if (0 == totalReaded) {
			// 没有读取到数据
			this.returnByteBuffer(buffer);
			return;
		}

		// 统计流量
		if (this.rx > Long.MAX_VALUE - totalReaded) {
			this.rx = 0;
		}
		this.rx += totalReaded;

		buffer.flip();

		byte[] array = new byte[totalReaded];
		buffer.get(array);

		// 解析数据
		this.parse(session, array);

		this.returnByteBuffer(buffer);
	}

	/**
	 * 处理数据发送。
	 *
	 * @param session 关联的 Session 对象。
	 * @param total 期望连续发送的消息条目数。
	 * @return 返回在队列里未发出的消息条目数。
	 */
	private int processSend(NonblockingAcceptorSession session, int total) {
		SocketChannel channel = (SocketChannel) session.selectionKey.channel();

		if (!channel.isConnected()) {
			return -1;
		}

		if (!session.isEmptyMessage()) {
			// 有消息，进行发送
			Message message = null;

			int count = total;

			synchronized (session) {
				// 遍历待发信息
				while (!session.isEmptyMessage() && count > 0) {
					message = session.pollMessage();
					if (null == message) {
						break;
					}

					// 是否进行消息加密
					byte[] plaintext = message.payload; 
					byte[] key = session.secretKey;
					if (null != key) {
						this.encryptMessage(message, key);
					}

					// 是否进行压缩
					if (message.needCompressible()) {
						try {
							byte[] cd = Utils.compress(message.payload);
							message.setPayload(cd);
						} catch (IOException e) {
							Logger.log(this.getClass(), e, LogLevel.WARNING);
						}
					}

					// 创建写缓存
					FlexibleByteBuffer buf = this.borrowByteBuffer();
					Message.pack(buf, message);
					buf.flip();

					// 计数
					--count;

					try {
						ByteBuffer bbuf = buf.getBuffer();
						while (bbuf.remaining() > 0) {
							// 写入 Socket
							int size = channel.write(bbuf);

							// 统计流量
							if (size > 0) {
								if (this.tx > Long.MAX_VALUE - size) {
									this.tx = 0;
								}

								this.tx += size;
							}

							if (bbuf.remaining() > 0) {
								try {
									Thread.sleep(1L);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
					} catch (IOException e) {
						Logger.log(NonblockingAcceptorWorker.class, e, LogLevel.WARNING);

						if (null != session.socketChannel) {
							this.acceptor.fireSessionClosed(session);
						}

						// 移除 Session
						this.acceptor.eraseSession(session);

						try {
							if (channel.isOpen()) {
								channel.close();
							}
						} catch (IOException ioe) {
							Logger.log(NonblockingAcceptorWorker.class, ioe, LogLevel.DEBUG);
						}

						this.removeSession(session);

						session.selectionKey.cancel();

						return -1;
					} finally {
						this.returnByteBuffer(buf);
					}

					// 还原明文
					message.setPayload(plaintext);
					// 回调事件
					this.acceptor.fireMessageSent(session, message);
				}
			} // #synchronized
		}

		return session.numMessages();
	}

	/**
	 * 解析并通知数据接收。
	 *
	 * @param session 关联的 Session 会话。
	 * @param data 原始字节数据。
	 */
	private void parse(NonblockingAcceptorSession session, byte[] data) {
		ArrayList<Message> output = this.borrowList();
		FlexibleByteBuffer remains = this.borrowByteBuffer();

		try {
			// 拼接数据
			session.readCache.put(data);

			// 整理数据
			if (session.readCache.position() != 0) {
				session.readCache.flip();
			}

			// 提取数据
			Message.extract(session.readCache, remains, output);

			// 清空
			session.readCache.clear();

			if (remains.limit() > 0) {
				session.readCache.put(remains);
			}

			if (!output.isEmpty()) {
				for (Message message : output) {
					// 是否采用压缩方式，如果是则进行解压
					if (message.verifyCompression()) {
						try {
							byte[] cd = Utils.uncompress(message.payload);
							message.setPayload(cd);
						} catch (IOException e) {
							Logger.log(this.getClass(), e, LogLevel.WARNING);
						}
					}

					// 是否是加密会话，如果是则进行解密
					byte[] key = session.secretKey;
					if (null != key) {
						this.decryptMessage(message, key);
					}

					this.acceptor.fireMessageReceived(session, message);
				}
			}

			// 防止内存溢出
			if (session.readCache.capacity() >= 1048576) {
				Logger.w(this.getClass(), "Session read cache overflow : " + session.toString());
				session.readCache.clear();
				session.readCache = null;
				session.readCache = new FlexibleByteBuffer(8192);
			}
		} catch (Exception e) {
			Logger.log(this.getClass(), e, LogLevel.ERROR);
		} finally {
			this.returnList(output);
			this.returnByteBuffer(remains);
		}
	}

	/**
	 * 借出 FlexibleByteBuffer 。
	 *
	 * @return 返回 FlexibleByteBuffer 实例。
	 */
	private FlexibleByteBuffer borrowByteBuffer() {
		FlexibleByteBuffer buffer = this.byteBufferQueue.poll();
		if (null == buffer) {
			return new FlexibleByteBuffer(8192);
		}
		return buffer;
	}

	/**
	 * 归还 FlexibleByteBuffer 。
	 *
	 * @param buffer FlexibleByteBuffer 实例。
	 */
	private void returnByteBuffer(FlexibleByteBuffer buffer) {
		buffer.clear();
		this.byteBufferQueue.offer(buffer);
	}

	/**
	 * 借出消息对象数组。
	 *
	 * @return 返回消息对象数组。
	 */
	private ArrayList<Message> borrowList() {
		ArrayList<Message> result = this.msgListQueue.poll();
		if (null == result) {
			return new ArrayList<Message>(2);
		}
		return result;
	}

	/**
	 * 归还消息对象数组。
	 *
	 * @param list 消息对象数组。
	 */
	private void returnList(ArrayList<Message> list) {
		list.clear();
		this.msgListQueue.offer(list);
	}

	/**
	 * 加密消息。
	 * 
	 * @param message 指定待加密消息。
	 * @param key 指定加密密钥。
	 */
	private void encryptMessage(Message message, byte[] key) {
		byte[] plaintext = message.getPayload();
		byte[] ciphertext = Cryptology.getInstance().simpleEncrypt(plaintext, key);
		message.setPayload(ciphertext);
	}

	/**
	 * 解密消息。
	 * 
	 * @param message 指定待解密消息。
	 * @param key 指定解密密钥。
	 */
	private void decryptMessage(Message message, byte[] key) {
		byte[] ciphertext = message.getPayload();
		byte[] plaintext = Cryptology.getInstance().simpleDecrypt(ciphertext, key);
		message.setPayload(plaintext);
	}

}
