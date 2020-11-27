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

import android.content.Context;
import android.os.Build;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import cell.util.Cryptology;
import cell.util.Network;
import cell.util.Utils;
import cell.util.collection.FlexibleByteBuffer;
import cell.util.log.LogLevel;
import cell.util.log.Logger;

/**
 * 非阻塞式网络连接器。
 */
public class NonblockingConnector extends MessageService implements MessageConnector {

	/** 应用程序上下文。 */
	private Context androidContext;

	/** 缓冲块大小。 */
	private int block = 65536;
	/** 单次写数据大小限制。 */
	private int writeLimit = 32768;

	/** 连接器连接的地址。 */
	private InetSocketAddress address;
	/** 连接超时时间。 */
	private long connectTimeout;
	/** NIO socket channel */
	private SocketChannel channel;
	/** NIO selector */
	private Selector selector;

	/** 对应的会话对象。 */
	private NonblockingConnectorSession session;

	/** 数据处理线程。 */
	private Thread handleThread;
	/** 线程是否自旋。 */
	private boolean spinning = false;
	/** 线程是否正在运行。 */
	private boolean running = false;

	/** 线程睡眠间隔。 */
	private long sleepInterval = 20L;

	/** 待发送消息列表。 */
	private ConcurrentLinkedQueue<Message> backlogQueue;

	/** 是否关闭连接。 */
	private boolean closed = false;

	/** 动态缓存对象池。 */
	private ConcurrentLinkedQueue<FlexibleByteBuffer> byteBufferQueue;

	/**
	 * 构造函数。
	 */
	public NonblockingConnector(Context androidContext) {
		this.androidContext = androidContext;
		this.connectTimeout = 10000L;
		this.backlogQueue = new ConcurrentLinkedQueue<Message>();
		this.byteBufferQueue = new ConcurrentLinkedQueue<FlexibleByteBuffer>();
	}

	/**
	 * 获得连接地址。
	 * 
	 * @return 返回连接地址。
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean connect(InetSocketAddress address) {
		if (this.channel != null && this.channel.isConnected()) {
			Logger.w(NonblockingConnector.class, "Connector has connected to " + address.getAddress().getHostAddress());
			return false;
		}

		if (this.running && null != this.channel) {
			this.spinning = false;

			try {
				if (this.channel.isOpen()) {
					this.channel.close();
				}

				if (null != this.selector) {
					this.selector.close();
				}
			} catch (IOException e) {
				Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);
			}

			while (this.running) {
				try {
					Thread.sleep(10L);
				} catch (InterruptedException e) {
					Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);
					break;
				}
			}
		}

		// For Android 2.2
		System.setProperty("java.net.preferIPv6Addresses", "false");

		// 判断是否有网络连接
		if (!Network.isConnected(this.androidContext)) {
			this.fireErrorOccurred(MessageErrorCode.NO_NETWORK, null);
			return false;
		}

		// 状态初始化
		this.backlogQueue.clear();
		this.address = address;

		try {
			this.channel = SocketChannel.open();
			this.channel.configureBlocking(false);

			// 配置
			if (Build.VERSION.SDK_INT >= 24) {
				this.channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
				this.channel.setOption(StandardSocketOptions.SO_RCVBUF, this.block);
				this.channel.setOption(StandardSocketOptions.SO_SNDBUF, this.block);
			}
			else {
				this.channel.socket().setKeepAlive(true);
				this.channel.socket().setReceiveBufferSize(this.block);
				this.channel.socket().setSendBufferSize(this.block);
			}

			this.selector = Selector.open();
			// 注册事件
			this.channel.register(this.selector, SelectionKey.OP_CONNECT);

			// 连接
			this.channel.connect(this.address);
		} catch (IOException e) {
			Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);

			// 回调错误
			this.fireErrorOccurred(MessageErrorCode.SOCKET_FAILED, null);

			try {
				if (null != this.channel) {
					this.channel.close();
				}
			} catch (Exception ce) {
				// Nothing
			}
			try {
				if (null != this.selector) {
					this.selector.close();
				}
			} catch (Exception se) {
				// Nothing
			}

			return false;
		} catch (Exception e) {
			Logger.log(NonblockingConnector.class, e, LogLevel.WARNING);
			return false;
		}

		// 创建 Session
		this.session = new NonblockingConnectorSession(this.address);

		this.handleThread = new Thread() {
			@Override
			public void run() {
				running = true;

				// 通知 Session 创建。
				fireSessionCreated();

				try {
					loopDispatch();
				} catch (Exception e) {
					spinning = false;
					Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);
				}

				// 通知 Session 销毁。
				fireSessionDestroyed();

				running = false;

				try {
					if (null != selector && selector.isOpen())
						selector.close();
					if (null != channel && channel.isOpen())
						channel.close();
				} catch (IOException e) {
					// Nothing
				}
			}
		};
		this.handleThread.setName(new StringBuilder("NonblockingConnector[").append(this.handleThread).append("]@")
			.append(this.address.getAddress().getHostAddress()).append(":").append(this.address.getPort()).toString());
		// 启动线程
		this.handleThread.start();

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void disconnect() {
		this.spinning = false;

		if (null != this.channel) {
			if (this.channel.isConnected()) {
				fireSessionClosed();
			}

			try {
				if (null != this.channel && this.channel.isOpen()) {
					this.channel.close();
				}
			} catch (Exception e) {
				Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);
			}

			try {
				if (null != this.channel) {
					this.channel.socket().close();
				}
			} catch (Exception e) {
				//Logger.logException(e, LogLevel.DEBUG);
			}
		}

		if (null != this.selector && this.selector.isOpen()) {
			try {
				this.selector.wakeup();
				this.selector.close();
			} catch (Exception e) {
				Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);
			}
		}

		int count = 0;
		while (this.running) {
			try {
				Thread.sleep(10L);
			} catch (InterruptedException e) {
				Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);
			}

			if (++count >= 300) {
				this.handleThread.interrupt();
				this.running = false;
			}
		}

		this.channel = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setConnectTimeout(long timeout) {
		this.connectTimeout = timeout;
	}

	/**
	 * 获得连接超时时间。
	 * 
	 * @return 返回以毫秒为单位的时间长度。
	 */
	public long getConnectTimeout() {
		return this.connectTimeout;
	}

	/**
	 * 重置线程 sleep 间隔。
	 * 
	 * @param sleepInterval 指定以毫秒为单位的间隔。
	 */
	public void resetSleepInterval(long sleepInterval) {
		this.sleepInterval = sleepInterval;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBlockSize(int size) {
		if (size < 2048) {
			return;
		}

		if (this.block == size) {
			return;
		}

		this.block = size;
		this.writeLimit = Math.round(size * 0.5f);

		if (null != this.channel) {
			try {
				this.channel.socket().setReceiveBufferSize(this.block);
				this.channel.socket().setSendBufferSize(this.block);
			} catch (Exception e) {
				// ignore
			}
		}
	}

	/**
	 * 获得缓存快大小。
	 * 
	 * @return 返回缓存块大小。
	 */
	public int getBlockSize() {
		return this.block;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isConnected() {
		return (null != this.channel && this.channel.isConnected());
	}

	/**
	 * 积压消息数量，即在发送队列中尚未发送的消息数量。
	 * 
	 * @return 返回在发送队列中尚未发送的消息数量。
	 */
	public int backlogLength() {
		return this.backlogQueue.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Session getSession() {
		return this.session;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(Message message) throws IOException {
		if (message.length() > this.writeLimit) {
			this.fireErrorOccurred(MessageErrorCode.WRITE_OUTOFBOUNDS, message);
			return;
		}

		this.backlogQueue.offer(message);
	}

	/**
	 * 通知会话创建。
	 */
	private void fireSessionCreated() {
		try {
			if (null != this.handler) {
				this.handler.sessionCreated(this.session);
			}
		} catch (Exception e) {
			// Nothing
		}
	}

	/**
	 * 通知会话启用。
	 */
	private void fireSessionOpened() {
		try {
			if (null != this.handler) {
				this.closed = false;
				this.handler.sessionOpened(this.session);
			}
		} catch (Exception e) {
			// Nothing
		}
	}

	/**
	 * 通知会话停用。
	 */
	private void fireSessionClosed() {
		try {
			if (null != this.handler) {
				if (!this.closed) {
					this.closed = true;
					this.handler.sessionClosed(this.session);
				}
			}
		} catch (Exception e) {
			// Nothing
		}
	}

	/**
	 * 通知会话销毁。
	 */
	private void fireSessionDestroyed() {
		try {
			if (null != this.handler) {
				this.handler.sessionDestroyed(this.session);
			}
		} catch (Exception e) {
			// Nothing
		}
	}

	/**
	 * 通知发生连接错误。回调 {@link MessageHandler#errorOccurred(int, Session)} 方法。
	 *
	 * @param errorCode 错误码。
	 * @param message 发生错误时的消息。
	 */
	private void fireErrorOccurred(int errorCode, Message message) {
		try {
			if (null != this.handler) {
				this.handler.errorOccurred(errorCode, this.session);
			}
		} catch (Exception e) {
			// Nothing
		}
	}

	/**
	 * 循环事件分发处理。
	 * 
	 * @throws Exception
	 */
	private void loopDispatch() throws Exception {
		// 自旋
		this.spinning = true;

		while (this.spinning) {
			if (!this.selector.isOpen()) {
				try {
					Thread.sleep(1L);
				} catch (InterruptedException e) {
					Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);
				}
				continue;
			}

			if (this.selector.select(this.channel.isConnected() ? 10L : this.connectTimeout) > 0) {
				Set<SelectionKey> keys = this.selector.selectedKeys();
				Iterator<SelectionKey> it = keys.iterator();
				while (it.hasNext()) {
					SelectionKey key = (SelectionKey) it.next();
					it.remove();

					// 当前通道选择器产生连接已经准备就绪事件，并且客户端套接字通道尚未连接到服务端套接字通道
					if (key.isConnectable()) {
						if (!this.doConnect(key)) {
							this.spinning = false;
							return;
						}
						else {
							// 连接成功，打开 Session
							fireSessionOpened();
						}
					}
					if (key.isValid() && key.isReadable()) {
						receive(key);
					}
					if (key.isValid() && key.isWritable()) {
						send(key);

						try {
							Thread.sleep(this.sleepInterval);
						} catch (InterruptedException e) {
							// Nothing
						}
					}
				} //# while

				if (!this.spinning) {
					break;
				}

				try {
					Thread.sleep(this.sleepInterval);
				} catch (InterruptedException e) {
					// Nothing
				}
			}
		} // # while

		// 关闭会话
		this.fireSessionClosed();
	}

	/**
	 * 执行连接事件。
	 *
	 * @param key 选中的 Key 。
	 * @return 连接成功返回 {@code true} 。
	 */
	private boolean doConnect(SelectionKey key) {
		// 获取创建通道选择器事件键的套接字通道
		SocketChannel channel = (SocketChannel) key.channel();

		// 判断此通道上是否正在进行连接操作。  
        // 完成套接字通道的连接过程。
		if (channel.isConnectionPending()) {
			try {
				channel.finishConnect();
			} catch (IOException e) {
				Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);

				try {
					this.channel.close();
					this.selector.close();
				} catch (IOException ce) {
					Logger.log(NonblockingConnector.class, ce, LogLevel.DEBUG);
				}

				// 连接失败
				fireErrorOccurred(MessageErrorCode.CONNECT_TIMEOUT, null);
				return false;
			}
		}

		if (key.isValid()) {
			key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
			key.interestOps(key.interestOps() | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		}

		return true;
	}

	/**
	 * 执行数据接收事件。
	 * 
	 * @param key 选中的 Key 。
	 */
	private void receive(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();

		if (!channel.isConnected()) {
			return;
		}

		int read = 0;

		FlexibleByteBuffer readBuffer = this.borrowByteBuffer();
		int totalRead = 0;

		do {
			read = 0;
			FlexibleByteBuffer buf = this.borrowByteBuffer();

			try {
				read = channel.read(buf.getBuffer());
			} catch (IOException e) {
	//			Logger.log(NonblockingConnector.class, e, LogLevel.DEBUG);

				fireSessionClosed();

				try {
					if (null != this.channel)
						this.channel.close();
					if (null != this.selector)
						this.selector.close();
				} catch (IOException ce) {
					Logger.log(NonblockingConnector.class, ce, LogLevel.DEBUG);
				}

				// 不能继续进行数据接收
				this.spinning = false;

				this.returnByteBuffer(buf);
				this.returnByteBuffer(readBuffer);

				return;
			}

			if (read == 0) {
				this.returnByteBuffer(buf);
				break;
			}
			else if (read == -1) {
				fireSessionClosed();

				try {
					this.channel.close();
					this.selector.close();
				} catch (IOException ce) {
					Logger.log(NonblockingConnector.class, ce, LogLevel.DEBUG);
				}

				// 不能继续进行数据接收
				this.spinning = false;

				this.returnByteBuffer(buf);
				this.returnByteBuffer(readBuffer);

				return;
			}
			else {
				// 长度计算
				totalRead += read;

				// 合并
				if (buf.position() != 0) {
					buf.flip();
				}
				readBuffer.put(buf);

				this.returnByteBuffer(buf);
			}
		} while (read > 0);

		// 就绪
		readBuffer.flip();

		if (totalRead != readBuffer.limit()) {
			Logger.e(NonblockingConnector.class, "Read buffer is error, total: " + totalRead + " , buffer size: " + readBuffer.limit());
		}

		try {
			this.process(readBuffer);
		} catch (ArrayIndexOutOfBoundsException e) {
			this.session.readCache.clear();
			Logger.log(NonblockingConnector.class, e, LogLevel.WARNING);
		}

		this.returnByteBuffer(readBuffer);

		if (key.isValid()) {
			key.interestOps(key.interestOps() | SelectionKey.OP_READ);
		}
	}

	/**
	 * 执行数据发送事件。
	 * 
	 * @param key 选中的 Key 。
	 */
	private void send(SelectionKey key) {
		SocketChannel channel = (SocketChannel) key.channel();

		if (!channel.isConnected()) {
			fireSessionClosed();
			return;
		}

		try {
			if (!this.backlogQueue.isEmpty()) {
				// 有消息，进行发送

				Message message = null;
				while (!this.backlogQueue.isEmpty()) {
					message = this.backlogQueue.poll();
					if (null == message) {
						break;
					}

					// 加密
					byte[] plaintext = message.payload; 
					byte[] skey = this.session.secretKey;
					if (null != skey) {
						this.encryptMessage(message, skey);
					}

					// 压缩
					if (message.needCompressible()) {
						try {
							byte[] cd = Utils.compress(message.payload);
							message.setPayload(cd);
						} catch (IOException e) {
							Logger.log(this.getClass(), e, LogLevel.WARNING);
						}
					}

					// 创建写缓存
					FlexibleByteBuffer writeBuffer = this.borrowByteBuffer();
					Message.pack(writeBuffer, message);
					writeBuffer.flip();

					channel.write(writeBuffer.getBuffer());

					this.returnByteBuffer(writeBuffer);

					if (null != this.handler) {
						message.setPayload(plaintext);
						this.handler.messageSent(this.session, message);
					}
				}
			}
		} catch (IOException e) {
			Logger.log(NonblockingConnector.class, e, LogLevel.WARNING);
		} catch (Exception e) {
			Logger.log(NonblockingConnector.class, e, LogLevel.WARNING);
		}

		if (key.isValid()) {
			key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
		}
	}

	/**
	 * 解析并处理消息。
	 * 
	 * @param data 接收到的数据数组。
	 */
	private void process(FlexibleByteBuffer data) {
		ArrayList<Message> output = new ArrayList<Message>(2);
		FlexibleByteBuffer remains = this.borrowByteBuffer();

		// 拼接数据
		this.session.readCache.put(data);

		// 整理数据
		this.session.readCache.flip();

		// 提取数据
		Message.extract(this.session.readCache, remains, output);

		// 清空
		this.session.readCache.clear();

		if (remains.limit() > 0) {
			this.session.readCache.put(remains);
		}

		if (!output.isEmpty()) {
			for (Message message : output) {
				// 解压
				if (message.verifyCompression()) {
					try {
						byte[] cd = Utils.uncompress(message.payload);
						message.setPayload(cd);
					} catch (IOException e) {
						Logger.log(this.getClass(), e, LogLevel.WARNING);
					}
				}

				// 解密
				byte[] skey = session.secretKey;
				if (null != skey) {
					decryptMessage(message, skey);
				}

				if (null != handler) {
					handler.messageReceived(session, message);
				}
			}
			output.clear();
		}

		this.returnByteBuffer(remains);
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
	 * @param buffer 指定 FlexibleByteBuffer 实例。
	 */
	private void returnByteBuffer(FlexibleByteBuffer buffer) {
		buffer.clear();
		this.byteBufferQueue.offer(buffer);
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
