package cell.util.rudp;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import cell.util.rudp.impl.ACKSegment;
import cell.util.rudp.impl.DATSegment;
import cell.util.rudp.impl.EAKSegment;
import cell.util.rudp.impl.FINSegment;
import cell.util.rudp.impl.NULSegment;
import cell.util.rudp.impl.RSTSegment;
import cell.util.rudp.impl.SYNSegment;
import cell.util.rudp.impl.Segment;
import cell.util.rudp.impl.Timer;

/**
 * 实现基于 RUDP 协议的客户端套接字。
 *
 * @see java.net.Socket
 */
public class ReliableSocket extends Socket {

	protected DatagramSocket _sock;
	protected SocketAddress _endpoint;
	protected cell.util.rudp.ReliableSocketInputStream _in;
	protected cell.util.rudp.ReliableSocketOutputStream _out;

	private byte[] _recvbuffer = new byte[65535];

	private boolean _closed = false;
	private boolean _connected = false;
	private boolean _reset = false;
	private boolean _keepAlive = true;
	private int _state = CLOSED;
	private int _timeout = 0;	// ms
	private boolean _shutIn = false;
	private boolean _shutOut = false;

	private Object _closeLock = new Object();
	private Object _resetLock = new Object();

	private ArrayList<cell.util.rudp.ReliableSocketListener> _listeners = new ArrayList<cell.util.rudp.ReliableSocketListener>();
	private ArrayList<cell.util.rudp.ReliableSocketStateListener> _stateListeners = new ArrayList<cell.util.rudp.ReliableSocketStateListener>();

	private ShutdownHook _shutdownHook;

	/* RUDP 连接参数。 */
	private cell.util.rudp.ReliableSocketProfile _profile = new cell.util.rudp.ReliableSocketProfile();

	/*
	 * 未确认（Unacknowledged）包发送队列。
	 */
	private ArrayList<Segment> _unackedSentQueue = new ArrayList<Segment>();
	/*
	 * 无序的接收队列（out-of-sequence）
	 */
	private ArrayList<Segment> _outSeqRecvQueue = new ArrayList<Segment>();
	/*
	 * 有序的接收队列（in-sequence）
	 */
	private ArrayList<Segment> _inSeqRecvQueue = new ArrayList<Segment>();

	// 接收队列锁。
	private Object _recvQueueLock = new Object();

	/*
	 * 序号计数器。
	 */
	private Counters _counters = new Counters();

	private Thread _sockThread = new ReliableSocketThread();

	// 发送队列的最大长度。
	private int _sendQueueSize = 32;

	// 接收队列的最大长度。
	private int _recvQueueSize = 32;

	private int _sendBufferSize;
	private int _recvBufferSize;

	/*
	 * 该定时器在连接开启是启动，在每次包段发出时重置。
	 * 如果客户端的空包段计时器过期，客服端向服务器发送一个空包段。
	 */
	private Timer _nullSegmentTimer = new Timer("ReliableSocket-NullSegmentTimer", new NullSegmentTimerTask());

	/*
	 * 每次发送数据包段、空包段或重置包段时，并且没有包段超时都重启该定时器。
	 * 如果在计时器过期时未收到此数据段的确认，则会重新传输已发送但未确认的所有包段。
	 * 如果仍有一个或多个数据包已发送但未确认，则在接收到定时段时重新启动重传计时器。
	 */
	private Timer _retransmissionTimer = new Timer("ReliableSocket-RetransmissionTimer", new RetransmissionTimerTask());

	/*
	 * 当该定时器超期时，如果无序队列里有包段，则发送扩展确认包段。
	 * 否则，如果任意包段未确认，发送单个确认包段。
	 * 每当在数据包段、空包段或者重置包段发送确认时，只要无序队列里没有包段，就重启累计确认定时器。
	 * 如果无序队列里有包段，则定时器不重启，以便在超期时发送另一个扩展确认包段。
	 */
	private Timer _cumulativeAckTimer = new Timer("ReliableSocket-CumulativeAckTimer", new CumulativeAckTimerTask());

	/*
	 * 当该定时器过期时，连接被认为断开。
	 */
	private Timer _keepAliveTimer = new Timer("ReliableSocket-KeepAliveTimer", new KeepAliveTimerTask());

	private static final int MAX_SEQUENCE_NUMBER = 255;

	/**
	 * 没有活跃或者加入的连接。
	 */
	private static final int CLOSED = 0;
	/**
	 * 已收到连接请求，正在等待确认。
	 */
	private static final int SYN_RCVD = 1;
	/**
	 * 已发送连接请求。
	 */
	private static final int SYN_SENT = 2;
	/**
	 * 数据传输状态。
	 */
	private static final int ESTABLISHED = 3;
	/**
	 * 请求关闭连接。
	 */
	private static final int CLOSE_WAIT = 4;

	private static final boolean DEBUG = Boolean.getBoolean("rudp.debug");

	/**
	 * 创建使用默认参数的 RUDP 套接字。
	 * 
	 * @throws IOException 如果打开底层 UDP 协议套接字失败抛出 I/O 异常。
	 */
	public ReliableSocket() throws IOException {
		this(new cell.util.rudp.ReliableSocketProfile());
	}

	/**
	 * 创建指定配置信息的未连接的 RUDP 套接字。
	 * 
	 * @throws IOException 如果打开底层 UDP 协议套接字失败抛出 I/O 异常。
	 */
	public ReliableSocket(cell.util.rudp.ReliableSocketProfile profile) throws IOException {
		this(new DatagramSocket(), profile);
	}

	/**
	 * 创建指定连接的主机名和端口号的 RUDP 套接字。
	 * 
	 * @param host 指定主机名，如果为 <code>null</code> 则使用回环地址。
	 * @param port 指定端口号。
	 * 
	 * @throws UnknownHostException 如果主机的 IP 地址无法确认抛出该异常。
	 * @throws IOException 如果创建底层套接字时发生 I/O 错误抛出该异常。
	 * @throws IllegalArgumentException 如果指定的端口号超出 0 到 65535 的取值范围时，抛出该异常。
	 * @see java.net.Socket#Socket(String, int)
	 */
	public ReliableSocket(String host, int port) throws UnknownHostException,
			IOException {
		this(new InetSocketAddress(host, port), null);
	}

	/**
	 * 创建 RUDP 套接字，并使用指定的本地地址和端口连接到指定的远程地址和端口上。
	 * <p>
	 * 如果指定的本地地址为 <code>null</code>，则使用通配符地址。
	 * （详看 <tt>{@link java.net.InetAddress#isAnyLocalAddress InetAddress.isAnyLocalAddress}()</tt>）
	 * <p>
	 * 如果指定本地端口为 <code>0</code>，则系统自动分配一个空闲端口号。
	 * 
	 * @param address 指定远程主机地址。
	 * @param port 指定远程主机端口。
	 * @param localAddr 指定本地绑定地址。
	 * @param localPort 指定本地绑定端口。
	 * @throws IOException 如果创建底层套接字时发生 I/O 错误抛出该异常。
	 * @throws IllegalArgumentException 如果指定的端口号超出 0 到 65535 的取值范围时，抛出该异常。
	 */
	public ReliableSocket(InetAddress address, int port,
			InetAddress localAddr, int localPort) throws IOException {
		this(new InetSocketAddress(address, port),
				new InetSocketAddress(localAddr, localPort));
	}

	/**
	 * 创建 RUDP 套接字，并使用指定的本地地址和端口连接到指定的远程地址和端口上。
	 * <p>
	 * 如果指定的本地地址为 <code>null</code>，则使用 
	 * <tt>{@link java.net.InetAddress#getByName InetAddress.getByName}(null)</tt>
	 * 获取地址。
	 * <p>
	 * 如果指定本地端口为 <code>0</code>，则系统自动分配一个空闲端口号。
	 * 
	 * @param host 指定远程主机名。
	 * @param port 指定远程主机端口。
	 * @param localAddr 指定本地绑定地址。
	 * @param localPort 指定本地绑定端口。
	 * @throws IOException 如果创建底层套接字时发生 I/O 错误抛出该异常。
	 * @throws IllegalArgumentException 如果指定的端口号超出 0 到 65535 的取值范围时，抛出该异常。
	 */
	public ReliableSocket(String host, int port, InetAddress localAddr,
			int localPort) throws IOException {
		this(new InetSocketAddress(host, port), new InetSocketAddress(localAddr, localPort));
	}

	/**
	 * 创建指定连接远程主机地址和本地地址的 RUDP 套接字。
	 * 
	 * @param inetAddr 指定远程主机地址。
	 * @param localAddr 指定本地地址。
	 * @throws IOException 如果创建底层套接字时发生 I/O 错误抛出该异常。
	 */
	protected ReliableSocket(InetSocketAddress inetAddr,
			InetSocketAddress localAddr) throws IOException {
		this(new DatagramSocket(localAddr), new cell.util.rudp.ReliableSocketProfile());
		connect(inetAddr);
	}

	/**
	 * 创建指定底层 UDP 套接字的 RUDP 套接字。
	 * 
	 * @param sock 指定 UDP 套接字。
	 */
	public ReliableSocket(DatagramSocket sock) {
		this(sock, new cell.util.rudp.ReliableSocketProfile());
	}

	/**
	 * 创建指定底层 UDP 套接字和配置参数的 RUDP 套接字。
	 * 
	 * @param sock 指定 UDP 套接字。
	 * @param profile 指定套接字配置文件。
	 */
	protected ReliableSocket(DatagramSocket sock, cell.util.rudp.ReliableSocketProfile profile) {
		if (sock == null) {
			throw new NullPointerException("sock");
		}

		init(sock, profile);
	}

	/**
	 * 初始化套接字，并设置接收数据流。
	 * 
	 * @param sock 数据包套接字。
	 * @param profile 配置信息。
	 */
	protected void init(DatagramSocket sock, cell.util.rudp.ReliableSocketProfile profile) {
		_sock = sock;
		_profile = profile;
		_shutdownHook = new ShutdownHook();

		_sendBufferSize = (_profile.maxSegmentSize() - Segment.RUDP_HEADER_LEN) * 32;
		_recvBufferSize = (_profile.maxSegmentSize() - Segment.RUDP_HEADER_LEN) * 32;

		/* Register shutdown hook */
		try {
			Runtime.getRuntime().addShutdownHook(_shutdownHook);
		} catch (IllegalStateException xcp) {
			if (DEBUG) {
				xcp.printStackTrace();
			}
		}

		_sockThread.start();
	}

	@Override
	public void bind(SocketAddress bindpoint) throws IOException {
		_sock.bind(bindpoint);
	}

	@Override
	public void connect(SocketAddress endpoint) throws IOException {
		connect(endpoint, 0);
	}

	@Override
	public void connect(SocketAddress endpoint, int timeout) throws IOException {
		if (endpoint == null) {
			throw new IllegalArgumentException("connect: The address can't be null");
		}

		if (timeout < 0) {
			throw new IllegalArgumentException("connect: timeout can't be negative");
		}

		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		if (isConnected()) {
			throw new SocketException("already connected");
		}

		if (!(endpoint instanceof InetSocketAddress)) {
			throw new IllegalArgumentException("Unsupported address type");
		}

		_endpoint = (InetSocketAddress) endpoint;

		// Synchronize sequence numbers
		_state = SYN_SENT;
		Random rand = new Random(System.currentTimeMillis());
		Segment syn = new SYNSegment(_counters.setSequenceNumber(rand.nextInt(MAX_SEQUENCE_NUMBER)),
				_profile.maxOutstandingSegs(),
				_profile.maxSegmentSize(),
				_profile.retransmissionTimeout(),
				_profile.cumulativeAckTimeout(),
				_profile.nullSegmentTimeout(),
				_profile.maxRetrans(),
				_profile.maxCumulativeAcks(),
				_profile.maxOutOfSequence(),
				_profile.maxAutoReset());

		sendAndQueueSegment(syn);

		// Wait for connection establishment (or timeout)
		boolean timedout = false;
		synchronized (this) {
			if (!isConnected()) {
				try {
					if (timeout == 0) {
						wait();
					} else {
						long startTime = System.currentTimeMillis();
						wait(timeout);
						if (System.currentTimeMillis() - startTime >= timeout) {
							timedout = true;
						}
					}
				} catch (InterruptedException xcp) {
					xcp.printStackTrace();
				}
			}
		}

		if (_state == ESTABLISHED) {
			return;
		}

		synchronized (_unackedSentQueue) {
			_unackedSentQueue.clear();
			_unackedSentQueue.notifyAll();
		}

		_counters.reset();
		_retransmissionTimer.cancel();

		switch (_state) {
		case SYN_SENT:
			connectionRefused();
			_state = CLOSED;
			if (timedout) {
				throw new SocketTimeoutException();
			}
			throw new SocketException("Connection refused");
		case CLOSED:
		case CLOSE_WAIT:
			_state = CLOSED;
			throw new SocketException("Socket closed");
		}
	}

	@Override
	public SocketChannel getChannel() {
		return null;
	}

	@Override
	public InetAddress getInetAddress() {
		if (!isConnected()) {
			return null;
		}

		return ((InetSocketAddress) _endpoint).getAddress();
	}

	@Override
	public int getPort() {
		if (!isConnected()) {
			return 0;
		}

		return ((InetSocketAddress) _endpoint).getPort();

	}

	@Override
	public SocketAddress getRemoteSocketAddress() {
		if (!isConnected()) {
			return null;
		}

		return new InetSocketAddress(getInetAddress(), getPort());
	}

	@Override
	public InetAddress getLocalAddress() {
		return _sock.getLocalAddress();
	}

	@Override
	public int getLocalPort() {
		return _sock.getLocalPort();
	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		return _sock.getLocalSocketAddress();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		if (!isConnected()) {
			throw new SocketException("Socket is not connected");
		}

		if (isInputShutdown()) {
			throw new SocketException("Socket input is shutdown");
		}

		return _in;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		if (!isConnected()) {
			throw new SocketException("Socket is not connected");
		}

		if (isOutputShutdown()) {
			throw new SocketException("Socket output is shutdown");
		}

		return _out;
	}

	@Override
	public synchronized void close() throws IOException {
		synchronized (_closeLock) {

			if (isClosed()) {
				return;
			}

			try {
				Runtime.getRuntime().removeShutdownHook(_shutdownHook);
			} catch (IllegalStateException xcp) {
				if (DEBUG) {
					xcp.printStackTrace();
				}
			}

			switch (_state) {
			case SYN_SENT:
				synchronized (this) {
					notify();
				}
				break;
			case CLOSE_WAIT:
			case SYN_RCVD:
			case ESTABLISHED:
				sendSegment(new FINSegment(_counters.nextSequenceNumber()));
				closeImpl();
				break;
			case CLOSED:
				_retransmissionTimer.destroy();
				_cumulativeAckTimer.destroy();
				_keepAliveTimer.destroy();
				_nullSegmentTimer.destroy();
				_sock.close();
				break;
			}

			_closed = true;
			_state = CLOSED;

			synchronized (_unackedSentQueue) {
				_unackedSentQueue.notify();
			}

			synchronized (_inSeqRecvQueue) {
				_inSeqRecvQueue.notify();
			}
		}
	}

	@Override
	public boolean isBound() {
		return _sock.isBound();
	}

	@Override
	public boolean isConnected() {
		return _connected;
	}

	@Override
	public boolean isClosed() {
		synchronized (_closeLock) {
			return _closed;
		}
	}

	@Override
	public void setSoTimeout(int timeout) throws SocketException {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout < 0");
		}

		_timeout = timeout;
	}

	@Override
	public synchronized void setSendBufferSize(int size) throws SocketException {
		if (!(size > 0)) {
			throw new IllegalArgumentException("negative receive size");
		}

		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		if (isConnected()) {
			return;
		}

		_sendBufferSize = size;
	}

	@Override
	public synchronized int getSendBufferSize() throws SocketException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		return _sendBufferSize;
	}

	@Override
	public synchronized void setReceiveBufferSize(int size)
			throws SocketException {
		if (!(size > 0)) {
			throw new IllegalArgumentException("negative send size");
		}

		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		if (isConnected()) {
			return;
		}

		_recvBufferSize = size;
	}

	@Override
	public synchronized int getReceiveBufferSize() throws SocketException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		return _recvBufferSize;
	}

	@Override
	public void setTcpNoDelay(boolean on) throws SocketException {
		throw new SocketException("Socket option not supported");
	}

	@Override
	public boolean getTcpNoDelay() {
		return false;
	}

	@Override
	public synchronized void setKeepAlive(boolean on) throws SocketException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		if (!(_keepAlive ^ on)) {
			return;
		}

		_keepAlive = on;

		if (isConnected()) {
			if (_keepAlive) {
				_keepAliveTimer.schedule(_profile.nullSegmentTimeout() * 6,
						_profile.nullSegmentTimeout() * 6);
			} else {
				_keepAliveTimer.cancel();
			}
		}
	}

	@Override
	public synchronized boolean getKeepAlive() throws SocketException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		return _keepAlive;
	}

	@Override
	public void shutdownInput() throws IOException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		if (!isConnected()) {
			throw new SocketException("Socket is not connected");
		}

		if (isInputShutdown()) {
			throw new SocketException("Socket input is already shutdown");
		}

		_shutIn = true;

		synchronized (_recvQueueLock) {
			_recvQueueLock.notify();
		}
	}

	@Override
	public void shutdownOutput() throws IOException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		if (!isConnected()) {
			throw new SocketException("Socket is not connected");
		}

		if (isOutputShutdown()) {
			throw new SocketException("Socket output is already shutdown");
		}

		_shutOut = true;

		synchronized (_unackedSentQueue) {
			_unackedSentQueue.notifyAll();
		}
	}

	@Override
	public boolean isInputShutdown() {
		return _shutIn;
	}

	@Override
	public boolean isOutputShutdown() {
		return _shutOut;
	}

	/**
	 * 重置套接字状态。
	 * <p>
	 * 套接字将处理所有待发的字节到远程终端。然后重新协商连接参数。
	 * 在重新协商结束后恢复传输并同步连接。
	 * 
	 * @throws IOException 发生 I/O 异常时。
	 */
	public void reset() throws IOException {
		reset(null);
	}

	/**
	 * 重置套接字状态和配置信息。
	 * <p>
	 * 套接字将处理所有待发的字节到远程终端。然后使用指定配置信息重新协商连接参数。
	 * 在重新协商结束后恢复传输并同步连接。
	 * 
	 * @param profile 指定套接字配置信息。
	 * 
	 * @throws IOException 发生 I/O 异常时。
	 */
	public void reset(cell.util.rudp.ReliableSocketProfile profile) throws IOException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		if (!isConnected()) {
			throw new SocketException("Socket is not connected");
		}

		synchronized (_resetLock) {
			_reset = true;

			sendAndQueueSegment(new RSTSegment(_counters.nextSequenceNumber()));

			// Wait to flush all outstanding segments (including last RST segment).
			synchronized (_unackedSentQueue) {
				while (!_unackedSentQueue.isEmpty()) {
					try {
						_unackedSentQueue.wait();
					} catch (InterruptedException xcp) {
						xcp.printStackTrace();
					}
				}
			}
		}

		connectionReset();

		// Set new profile
		if (profile != null) {
			_profile = profile;
		}

		// Synchronize sequence numbers
		_state = SYN_SENT;
		Random rand = new Random(System.currentTimeMillis());
		Segment syn = new SYNSegment(_counters.setSequenceNumber(rand.nextInt(MAX_SEQUENCE_NUMBER)),
				_profile.maxOutstandingSegs(),
				_profile.maxSegmentSize(),
				_profile.retransmissionTimeout(),
				_profile.cumulativeAckTimeout(),
				_profile.nullSegmentTimeout(),
				_profile.maxRetrans(),
				_profile.maxCumulativeAcks(),
				_profile.maxOutOfSequence(),
				_profile.maxAutoReset());

		sendAndQueueSegment(syn);
	}

	/**
	 * 将指定长度和偏移量的字节数组内的数据写入发送队列。
	 * 
	 * @param b 存储数据的字节数组。
	 * @param off 数据的起始偏移。
	 * @param len 写入的字节长度。
	 * @throws IOException 发生 I/O 异常时。
	 */
	protected void write(byte[] b, int off, int len) throws IOException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		if (isOutputShutdown()) {
			throw new IOException("Socket output is shutdown");
		}

		if (!isConnected()) {
			throw new SocketException("Connection reset");
		}

		int totalBytes = 0;
		while (totalBytes < len) {
			synchronized (_resetLock) {
				while (_reset) {
					try {
						_resetLock.wait();
					} catch (InterruptedException xcp) {
						xcp.printStackTrace();
					}
				}

				int writeBytes = Math.min(_profile.maxSegmentSize() - Segment.RUDP_HEADER_LEN, len - totalBytes);

				sendAndQueueSegment(new DATSegment(_counters.nextSequenceNumber(),
						_counters.getLastInSequence(),
						b, off + totalBytes,
						writeBytes));
				totalBytes += writeBytes;
			}
		}
	}

	/**
	 * 从接收缓冲区读取数据到指定数组。
	 * 该方法尝试读取 <code>len</code> 长度的数据，但是实际读取的长度可能小于该长度。
	 * 该方法返回实际读取长度。
	 * <p>
	 * 该方法为阻塞调用，直到输入数据可用，读取到句柄末尾或者遇到异常是结束。
	 * 
	 * @param b 用户接收读取数据的缓冲区。
	 * @param off 缓冲区数组的起始偏移量。
	 * @param len 读取的最大长度。
	 * @return 读入缓冲区数组的字节总数。如果返回 <code>-1</code> 表示未读取到任何数据。
	 * @throws IOException 发生 I/O 异常时。
	 */
	protected int read(byte[] b, int off, int len) throws IOException {

		int totalBytes = 0;

		synchronized (_recvQueueLock) {

			while (true) {
				while (_inSeqRecvQueue.isEmpty()) {

					if (isClosed()) {
						throw new SocketException("Socket is closed");
					}

					if (isInputShutdown()) {
						throw new EOFException();
					}

					if (!isConnected()) {
						throw new SocketException("Connection reset");
					}

					try {
						if (_timeout == 0) {
							_recvQueueLock.wait();
						} else {
							long startTime = System.currentTimeMillis();
							_recvQueueLock.wait(_timeout);
							if ((System.currentTimeMillis() - startTime) >= _timeout) {
								throw new SocketTimeoutException();
							}
						}
					} catch (InterruptedException xcp) {
						if (!_closed)
							throw new InterruptedIOException(xcp.getMessage());
					}
				}

				for (Iterator<Segment> it = _inSeqRecvQueue.iterator(); it.hasNext();) {
					Segment s = (Segment) it.next();

					if (s instanceof RSTSegment) {
						it.remove();
						break;
					} else if (s instanceof FINSegment) {
						if (totalBytes <= 0) {
							it.remove();
							return -1; /* EOF */
						}
						break;
					} else if (s instanceof DATSegment) {
						byte[] data = ((DATSegment) s).getData();
						if (data.length + totalBytes > len) {
							if (totalBytes <= 0) {
								throw new IOException("insufficient buffer space");
							}
							break;
						}

						System.arraycopy(data, 0, b, off + totalBytes, data.length);
						totalBytes += data.length;
						it.remove();
					}
				}

				if (totalBytes > 0) {
					return totalBytes;
				}
			}
		}
	}

	/**
	 * 添加指定的监听器到该套接字。
	 * 
	 * @param listener 待添加的监听器。
	 */
	public void addListener(cell.util.rudp.ReliableSocketListener listener) {
		if (listener == null) {
			throw new NullPointerException("listener");
		}

		synchronized (_listeners) {
			if (!_listeners.contains(listener)) {
				_listeners.add(listener);
			}
		}
	}

	/**
	 * 移除已添加的监听器。
	 * 
	 * @param listener 待移除的监听器。
	 */
	public void removeListener(cell.util.rudp.ReliableSocketListener listener) {
		if (listener == null) {
			throw new NullPointerException("listener");
		}

		synchronized (_listeners) {
			_listeners.remove(listener);
		}
	}

	/**
	 * 添加指定的状态监听器到该套接字。
	 * 
	 * @param stateListener 待添加的状态监听器。
	 */
	public void addStateListener(cell.util.rudp.ReliableSocketStateListener stateListener) {
		if (stateListener == null) {
			throw new NullPointerException("stateListener");
		}

		synchronized (_stateListeners) {
			if (!_stateListeners.contains(stateListener)) {
				_stateListeners.add(stateListener);
			}
		}
	}

	/**
	 * 移除已添加的状态监听器。
	 * 
	 * @param stateListener 待移除的监听器。
	 */
	public void removeStateListener(cell.util.rudp.ReliableSocketStateListener stateListener) {
		if (stateListener == null) {
			throw new NullPointerException("stateListener");
		}

		synchronized (_stateListeners) {
			_stateListeners.remove(stateListener);
		}
	}

	/**
	 * 发送需要进行确认应答的数据段。
	 * 
	 * @param s 数据段。
	 * @throws IOException 底层套接字发生错误。
	 */
	private void sendSegment(Segment s) throws IOException {
		// Piggyback any pending acknowledgments
		if (s instanceof DATSegment || s instanceof RSTSegment
				|| s instanceof FINSegment || s instanceof NULSegment) {
			checkAndSetAck(s);
		}

		// Reset null segment timer
		if (s instanceof DATSegment || s instanceof RSTSegment
				|| s instanceof FINSegment) {
			_nullSegmentTimer.reset();
		}

		if (DEBUG) {
			log("sent " + s);
		}

		sendSegmentImpl(s);
	}

	/**
	 * 接收数据段并增加累计确认计数。
	 * 
	 * @return 接收到的数据段。
	 * @throws IOException 底层套接字发生错误。
	 */
	private Segment receiveSegment() throws IOException {
		Segment s;
		if ((s = receiveSegmentImpl()) != null) {

			if (DEBUG) {
				log("recv " + s);
			}

			if (s instanceof DATSegment || s instanceof NULSegment
					|| s instanceof RSTSegment || s instanceof FINSegment
					|| s instanceof SYNSegment) {
				_counters.incCumulativeAckCounter();
			}

			if (_keepAlive) {
				_keepAliveTimer.reset();
			}
		}

		return s;
	}

	/**
	 * 发送一个数据段并在未应答队列里将其入队。
	 * 
	 * @param segment 数据段。
	 * @throws IOException 底层套接字发生错误。
	 */
	private void sendAndQueueSegment(Segment segment) throws IOException {
		synchronized (_unackedSentQueue) {
			while ((_unackedSentQueue.size() >= _sendQueueSize)
					|| (_counters.getOutstandingSegsCounter() > _profile.maxOutstandingSegs())) {
				try {
					_unackedSentQueue.wait();
				} catch (InterruptedException xcp) {
					xcp.printStackTrace();
				}
			}

			_counters.incOutstandingSegsCounter();
			_unackedSentQueue.add(segment);
		}

		if (_closed) {
			throw new SocketException("Socket is closed");
		}

		// Re-start retransmission timer
		if (!(segment instanceof EAKSegment) && !(segment instanceof ACKSegment)) {
			synchronized (_retransmissionTimer) {
				if (_retransmissionTimer.isIdle()) {
					_retransmissionTimer.schedule(
							_profile.retransmissionTimeout(),
							_profile.retransmissionTimeout());
				}
			}
		}

		sendSegment(segment);

		if (segment instanceof DATSegment) {
			synchronized (_listeners) {
				Iterator<cell.util.rudp.ReliableSocketListener> it = _listeners.iterator();
				while (it.hasNext()) {
					cell.util.rudp.ReliableSocketListener l = (cell.util.rudp.ReliableSocketListener) it.next();
					l.packetSent();
				}
			}
		}
	}

	/**
	 * 发送数据段并累加重传计数。
	 * 
	 * @param segment 重传数据段。
	 * @throws IOException 底层套接字发生错误。
	 */
	private void retransmitSegment(Segment segment) throws IOException {
		if (_profile.maxRetrans() > 0) {
			segment.setRetxCounter(segment.getRetxCounter() + 1);
		}

		if (_profile.maxRetrans() != 0
				&& segment.getRetxCounter() > _profile.maxRetrans()) {
			connectionFailure();
			return;
		}

		sendSegment(segment);

		if (segment instanceof DATSegment) {
			synchronized (_listeners) {
				Iterator<cell.util.rudp.ReliableSocketListener> it = _listeners.iterator();
				while (it.hasNext()) {
					cell.util.rudp.ReliableSocketListener l = (cell.util.rudp.ReliableSocketListener) it.next();
					l.packetRetransmitted();
				}
			}
		}
	}

	/**
	 * 将连接状态设置为开启状态，并通知所有状态监听器连接已开启。
	 */
	private void connectionOpened() {
		if (isConnected()) {

			_nullSegmentTimer.cancel();

			if (_keepAlive) {
				_keepAliveTimer.cancel();
			}

			synchronized (_resetLock) {
				_reset = false;
				_resetLock.notify();
			}
		} else {
			synchronized (this) {
				try {
					_in = new cell.util.rudp.ReliableSocketInputStream(this);
					_out = new cell.util.rudp.ReliableSocketOutputStream(this);
					_connected = true;
					_state = ESTABLISHED;
				} catch (IOException xcp) {
					xcp.printStackTrace();
				}

				notify();
			}

			synchronized (_stateListeners) {
				Iterator<cell.util.rudp.ReliableSocketStateListener> it = _stateListeners.iterator();
				while (it.hasNext()) {
					cell.util.rudp.ReliableSocketStateListener l = (cell.util.rudp.ReliableSocketStateListener) it.next();
					l.connectionOpened(this);
				}
			}
		}

		_nullSegmentTimer.schedule(0, _profile.nullSegmentTimeout());

		if (_keepAlive) {
			_keepAliveTimer.schedule(_profile.nullSegmentTimeout() * 6,
					_profile.nullSegmentTimeout() * 6);
		}
	}

	/**
	 * 通知所有状态监听器连接被拒绝。
	 */
	private void connectionRefused() {
		synchronized (_stateListeners) {
			Iterator<cell.util.rudp.ReliableSocketStateListener> it = _stateListeners.iterator();
			while (it.hasNext()) {
				cell.util.rudp.ReliableSocketStateListener l = (cell.util.rudp.ReliableSocketStateListener) it.next();
				l.connectionRefused(this);
			}
		}
	}

	/**
	 * 通知所有状态监听器连接已关闭。
	 */
	private void connectionClosed() {
		synchronized (_stateListeners) {
			Iterator<cell.util.rudp.ReliableSocketStateListener> it = _stateListeners.iterator();
			while (it.hasNext()) {
				cell.util.rudp.ReliableSocketStateListener l = (cell.util.rudp.ReliableSocketStateListener) it.next();
				l.connectionClosed(this);
			}
		}
	}

	/**
	 * 将连接状态设置为已关闭，并通知所有状态监听器连接失败。
	 */
	private void connectionFailure() {
		synchronized (_closeLock) {

			if (isClosed()) {
				return;
			}

			switch (_state) {
			case SYN_SENT:
				synchronized (this) {
					notify();
				}
				break;
			case CLOSE_WAIT:
			case SYN_RCVD:
			case ESTABLISHED:
				_connected = false;
				synchronized (_unackedSentQueue) {
					_unackedSentQueue.notifyAll();
				}

				synchronized (_recvQueueLock) {
					_recvQueueLock.notify();
				}

				closeImpl();
				break;
			}

			_state = CLOSED;
			_closed = true;
		}

		synchronized (_stateListeners) {
			Iterator<cell.util.rudp.ReliableSocketStateListener> it = _stateListeners.iterator();
			while (it.hasNext()) {
				cell.util.rudp.ReliableSocketStateListener l = (cell.util.rudp.ReliableSocketStateListener) it.next();
				l.connectionFailure(this);
			}
		}
	}

	/**
	 * 通知所有状态监听器连接被重置。
	 */
	private void connectionReset() {
		synchronized (_stateListeners) {
			Iterator<cell.util.rudp.ReliableSocketStateListener> it = _stateListeners.iterator();
			while (it.hasNext()) {
				cell.util.rudp.ReliableSocketStateListener l = (cell.util.rudp.ReliableSocketStateListener) it.next();
				l.connectionReset(this);
			}
		}
	}

	/**
	 * 处理接收到的 SYN 数据包。
	 * <p>
	 * 当客户端初始化一个连接时通过发送 SYN 数据包来协商上层协议参数。
	 * 服务器可以同意这些参数协商并且使用 ACK 应答这个 SYN 协商，或者使用 ACK 应答提出不同的参数。
	 * 客户端可以选择同意来自服务器的参数并发送 ACK 来建立连接，或者发送 FIN 来拒绝连接。
	 * 
	 * @param segment SYN 数据包。
	 * 
	 */
	private void handleSYNSegment(SYNSegment segment) {
		try {
			switch (_state) {
			case CLOSED:
				_counters.setLastInSequence(segment.seq());
				_state = SYN_RCVD;

				Random rand = new Random(System.currentTimeMillis());
				_profile = new cell.util.rudp.ReliableSocketProfile(_sendQueueSize, _recvQueueSize,
						segment.getMaxSegmentSize(),
						segment.getMaxOutstandingSegments(),
						segment.getMaxRetransmissions(),
						segment.getMaxCumulativeAcks(),
						segment.getMaxOutOfSequence(),
						segment.getMaxAutoReset(),
						segment.getNulSegmentTimeout(),
						segment.getRetransmissionTimeout(),
						segment.getCummulativeAckTimeout());

				Segment syn = new SYNSegment(_counters.setSequenceNumber(rand.nextInt(MAX_SEQUENCE_NUMBER)),
						_profile.maxOutstandingSegs(),
						_profile.maxSegmentSize(),
						_profile.retransmissionTimeout(),
						_profile.cumulativeAckTimeout(),
						_profile.nullSegmentTimeout(), _profile.maxRetrans(),
						_profile.maxCumulativeAcks(),
						_profile.maxOutOfSequence(), _profile.maxAutoReset());

				syn.setAck(segment.seq());
				sendAndQueueSegment(syn);
				break;
			case SYN_SENT:
				_counters.setLastInSequence(segment.seq());
				_state = ESTABLISHED;

				// 这里客户端可以接受或者拒绝参数，这里我们接受参数。
				sendAck();
				connectionOpened();
				break;
			}
		} catch (IOException xcp) {
			xcp.printStackTrace();
		}
	}

	/**
	 * 处理接收到的 EAK 数据包。
	 * <p>
	 * 当接收到 EAK 数据时，其消息中指定的数据包就从未确认发送队列中移除。
	 * 数据包的重传是通过检查 EAK 数据包里的 ACK 序号和队列里的最后一个 ACK 序号来确定的。
	 * 在未确认队列里不包含在这两个序号之间的所有数据都将被重传。
	 * 
	 * @param segment EAK 数据包。
	 */
	private void handleEAKSegment(EAKSegment segment) {
		Iterator<Segment> it;
		int[] acks = segment.getACKs();

		int lastInSequence = segment.getAck();
		int lastOutSequence = acks[acks.length - 1];

		synchronized (_unackedSentQueue) {

			// 从已发送队列移除已确认的数据包
			for (it = _unackedSentQueue.iterator(); it.hasNext();) {
				Segment s = (Segment) it.next();
				if ((compareSequenceNumbers(s.seq(), lastInSequence) <= 0)) {
					it.remove();
					continue;
				}

				for (int i = 0; i < acks.length; i++) {
					if ((compareSequenceNumbers(s.seq(), acks[i]) == 0)) {
						it.remove();
						break;
					}
				}
			}

			// 重传
			it = _unackedSentQueue.iterator();
			while (it.hasNext()) {
				Segment s = (Segment) it.next();
				if ((compareSequenceNumbers(lastInSequence, s.seq()) < 0)
						&& (compareSequenceNumbers(lastOutSequence, s.seq()) > 0)) {

					try {
						retransmitSegment(s);
					} catch (IOException xcp) {
						xcp.printStackTrace();
					}
				}
			}

			_unackedSentQueue.notifyAll();
		}
	}

	/**
	 * 处理接收到的 RST，FIN 和 DAT 数据包。
	 * 
	 * @param segment
	 */
	private void handleSegment(Segment segment) {
		// 当接收到 RST 包时，发送方应该停止发送新的数据包，但是已经接收到的数据包应该继续分发给应用。
		if (segment instanceof RSTSegment) {
			synchronized (_resetLock) {
				_reset = true;
			}

			connectionReset();
		}

		// 当接收到 FIN 包时，可以预期在该数据包之后不会再接收到任何数据包。
		if (segment instanceof FINSegment) {
			switch (_state) {
			case SYN_SENT:
				synchronized (this) {
					notify();
				}
				break;
			case CLOSED:
				break;
			default:
				_state = CLOSE_WAIT;
			}
		}

		boolean inSequence = false;
		synchronized (_recvQueueLock) {

			if (compareSequenceNumbers(segment.seq(), _counters.getLastInSequence()) <= 0) {
				// 丢包：duplicate
			} else if (compareSequenceNumbers(segment.seq(), nextSequenceNumber(_counters.getLastInSequence())) == 0) {
				inSequence = true;
				if (_inSeqRecvQueue.size() == 0
						|| (_inSeqRecvQueue.size() + _outSeqRecvQueue.size() < _recvQueueSize)) {
					// 插入 in-sequence 队列
					_counters.setLastInSequence(segment.seq());
					if (segment instanceof DATSegment
							|| segment instanceof RSTSegment
							|| segment instanceof FINSegment) {
						_inSeqRecvQueue.add(segment);
					}

					if (segment instanceof DATSegment) {
						synchronized (_listeners) {
							Iterator<cell.util.rudp.ReliableSocketListener> it = _listeners.iterator();
							while (it.hasNext()) {
								cell.util.rudp.ReliableSocketListener l = (cell.util.rudp.ReliableSocketListener) it.next();
								l.packetReceivedInOrder();
							}
						}
					}

					checkRecvQueues();
				} else {
					// 丢包：queue is full
				}
			} else if (_inSeqRecvQueue.size() + _outSeqRecvQueue.size() < _recvQueueSize) {
				// 按顺序插入 out-of-sequence 队列
				boolean added = false;
				for (int i = 0; i < _outSeqRecvQueue.size() && !added; i++) {
					Segment s = (Segment) _outSeqRecvQueue.get(i);
					int cmp = compareSequenceNumbers(segment.seq(), s.seq());
					if (cmp == 0) {
						// 忽略重复的包
						added = true;
					} else if (cmp < 0) {
						_outSeqRecvQueue.add(i, segment);
						added = true;
					}
				}

				if (!added) {
					_outSeqRecvQueue.add(segment);
				}

				_counters.incOutOfSequenceCounter();

				if (segment instanceof DATSegment) {
					synchronized (_listeners) {
						Iterator<cell.util.rudp.ReliableSocketListener> it = _listeners.iterator();
						while (it.hasNext()) {
							cell.util.rudp.ReliableSocketListener l = (cell.util.rudp.ReliableSocketListener) it.next();
							l.packetReceivedOutOfOrder();
						}
					}
				}
			}

			if (inSequence
					&& (segment instanceof RSTSegment
							|| segment instanceof NULSegment || segment instanceof FINSegment)) {
				sendAck();
			} else if ((_counters.getOutOfSequenceCounter() > 0)
					&& (_profile.maxOutOfSequence() == 0 || _counters.getOutOfSequenceCounter() > _profile.maxOutOfSequence())) {
				sendExtendedAck();
			} else if ((_counters.getCumulativeAckCounter() > 0)
					&& (_profile.maxCumulativeAcks() == 0 || _counters.getCumulativeAckCounter() > _profile.maxCumulativeAcks())) {
				sendSingleAck();
			} else {
				synchronized (_cumulativeAckTimer) {
					if (_cumulativeAckTimer.isIdle()) {
						_cumulativeAckTimer.schedule(_profile.cumulativeAckTimeout());
					}
				}
			}
		}
	}

	/**
	 * 确认下一个需要确认的包。
	 * 如果接收方的无序队列里有数据，则进行 EAK 确认。
	 */
	private void sendAck() {
		synchronized (_recvQueueLock) {
			if (!_outSeqRecvQueue.isEmpty()) {
				sendExtendedAck();
				return;
			}

			sendSingleAck();
		}
	}

	/**
	 * 只要在无序接收队列里有数据，就发送 EAK 包。
	 */
	private void sendExtendedAck() {
		synchronized (_recvQueueLock) {

			if (_outSeqRecvQueue.isEmpty()) {
				return;
			}

			_counters.getAndResetCumulativeAckCounter();
			_counters.getAndResetOutOfSequenceCounter();

			// 合成无序序列号列表
			int[] acks = new int[_outSeqRecvQueue.size()];
			for (int i = 0; i < acks.length; i++) {
				Segment s = (Segment) _outSeqRecvQueue.get(i);
				acks[i] = s.seq();
			}

			try {
				int lastInSequence = _counters.getLastInSequence();
				sendSegment(new EAKSegment(nextSequenceNumber(lastInSequence),
						lastInSequence, acks));
			} catch (IOException xcp) {
				xcp.printStackTrace();
			}

		}
	}

	/**
	 * 如果接收到包需要确认，发送 ACK 包进行确认。
	 */
	private void sendSingleAck() {
		if (_counters.getAndResetCumulativeAckCounter() == 0) {
			return;
		}

		try {
			int lastInSequence = _counters.getLastInSequence();
			sendSegment(new ACKSegment(nextSequenceNumber(lastInSequence),
					lastInSequence));
		} catch (IOException xcp) {
			xcp.printStackTrace();
		}
	}

	/**
	 * 检测接收方如果有未确认包需要确认，则设置 ACK 标识及确认序号。
	 * 
	 * @param s 数据包
	 */
	private void checkAndSetAck(Segment s) {
		if (_counters.getAndResetCumulativeAckCounter() == 0) {
			return;
		}

		s.setAck(_counters.getLastInSequence());
	}

	/**
	 * 检查 ACK 标志和包数量。
	 * 
	 * @param segment 包段。
	 */
	private void checkAndGetAck(Segment segment) {
		int ackn = segment.getAck();

		if (ackn < 0) {
			return;
		}

		_counters.getAndResetOutstandingSegsCounter();

		if (_state == SYN_RCVD) {
			_state = ESTABLISHED;
			connectionOpened();
		}

		synchronized (_unackedSentQueue) {
			Iterator<Segment> it = _unackedSentQueue.iterator();
			while (it.hasNext()) {
				Segment s = (Segment) it.next();
				if (compareSequenceNumbers(s.seq(), ackn) <= 0) {
					it.remove();
				}
			}

			if (_unackedSentQueue.isEmpty()) {
				_retransmissionTimer.cancel();
			}

			_unackedSentQueue.notifyAll();
		}
	}

	/**
	 * 检查在无序队列里可以被移动到有序队列里的包段。
	 */
	private void checkRecvQueues() {
		synchronized (_recvQueueLock) {
			Iterator<Segment> it = _outSeqRecvQueue.iterator();
			while (it.hasNext()) {
				Segment s = (Segment) it.next();
				if (compareSequenceNumbers(s.seq(), nextSequenceNumber(_counters.getLastInSequence())) == 0) {
					_counters.setLastInSequence(s.seq());
					if (s instanceof DATSegment || s instanceof RSTSegment
							|| s instanceof FINSegment) {
						_inSeqRecvQueue.add(s);
					}
					it.remove();
				}
			}

			_recvQueueLock.notify();
		}
	}

	/**
	 * 将包段数据写入底层 UDP 套接字。
	 * 
	 * @param s 待写入的包段。
	 * @throws IOException 如果底层 UDP 套接字发生 I/O 错误。
	 */
	protected void sendSegmentImpl(Segment s) throws IOException {
		try {
			DatagramPacket packet = new DatagramPacket(s.getBytes(), s.length(), _endpoint);
			_sock.send(packet);
		} catch (IOException xcp) {
			if (!isClosed()) {
				xcp.printStackTrace();
			}
		}
	}

	/**
	 * 从底层 UDP 套接字读取包段数据。
	 * 
	 * @return s 读取成功的包段数据。
	 * @throws IOException 如果底层 UDP 套接字发生 I/O 错误。
	 */
	protected Segment receiveSegmentImpl() throws IOException {
		try {
			DatagramPacket packet = new DatagramPacket(_recvbuffer, _recvbuffer.length);
			_sock.receive(packet);
			return Segment.parse(packet.getData(), 0, packet.getLength());
		} catch (IOException ioXcp) {
			if (!isClosed()) {
				ioXcp.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * 关闭底层的 UDP 套接字。
	 */
	protected void closeSocket() {
		_sock.close();
	}

	/**
	 * 执行清理操作并关闭套接字。
	 */
	protected void closeImpl() {
		_nullSegmentTimer.cancel();
		_keepAliveTimer.cancel();
		_state = CLOSE_WAIT;

		Thread t = new Thread() {
			@Override
			public void run() {
				_keepAliveTimer.destroy();
				_nullSegmentTimer.destroy();

				try {
					Thread.sleep(_profile.nullSegmentTimeout() * 2);
				} catch (InterruptedException xcp) {
					xcp.printStackTrace();
				}

				_retransmissionTimer.destroy();
				_cumulativeAckTimer.destroy();

				closeSocket();
				connectionClosed();
			}
		};
		t.setName("ReliableSocket-Closing");
		t.setDaemon(true);
		t.start();
	}

	/**
	 * 打印日志。
	 */
	protected void log(String msg) {
		System.out.println(getLocalPort() + ": " + msg);
	}

	/**
	 * 计算指定序号的连续序号。
	 * 
	 * @return 指定序号的下一个连续序号。
	 */
	private static int nextSequenceNumber(int seqn) {
		return (seqn + 1) % MAX_SEQUENCE_NUMBER;
	}

	/**
	 * 比较两个序号。
	 * 
	 * @return 如果第一个序号等于、大于或小于第二个序号，则依次返回 0，1 或 -1 。
	 */
	private int compareSequenceNumbers(int seqn, int aseqn) {
		if (seqn == aseqn) {
			return 0;
		} else if (((seqn < aseqn) && ((aseqn - seqn) > MAX_SEQUENCE_NUMBER / 2))
				|| ((seqn > aseqn) && ((seqn - aseqn) < MAX_SEQUENCE_NUMBER / 2))) {
			return 1;
		} else {
			return -1;
		}
	}
	

	/*
	 * -----------------------------------------------------------------------
	 * INTERNAL CLASSES
	 * -----------------------------------------------------------------------
	 */

	private class Counters {
		// Segment 序列号 
		private int _seqn;
		// 最后一个接收到的有序包的序号
		private int _lastInSequence;

		/**
		 * Cumulative acknowledge counter
		 * <p>
		 * 接收方维护的未确认包计数器，该未确认包在没有向发送方进行确认的情况下被接收到。
		 * 可以对该计数的最大值进行配置。如果计数达到最大值，当前有无序包，发送方将发送单个确认或者扩展确认。
		 * 累计确认计数器的建议值为 3 。
		 */
		private int _cumAckCounter;

		/**
		 * Out-of-sequence acknowledgments counter
		 * <p>
		 * 接收方维护的已达到的无序包计数器。当该计数达到其可配置的最大值时，
		 * 将对当前所有无序包向发送方发送扩展确认包。
		 * 然后将计数重置为 0 。
		 * 无序确认计数器的建议值为 3 。
		 */
		private int _outOfSeqCounter;

		/**
		 * Outstanding segments counter
		 * <p>
		 * 发送方维护的计数器。记录没有获得确认的已发送的包的数量。
		 * 可以用于接收方进行流量控制。
		 */
		private int _outSegsCounter;
		
		public Counters() {
		}

		public synchronized int nextSequenceNumber() {
			return (_seqn = ReliableSocket.nextSequenceNumber(_seqn));
		}

		public synchronized int setSequenceNumber(int n) {
			_seqn = n;
			return _seqn;
		}

		public synchronized int setLastInSequence(int n) {
			_lastInSequence = n;
			return _lastInSequence;
		}

		public synchronized int getLastInSequence() {
			return _lastInSequence;
		}

		public synchronized void incCumulativeAckCounter() {
			_cumAckCounter++;
		}

		public synchronized int getCumulativeAckCounter() {
			return _cumAckCounter;
		}

		public synchronized int getAndResetCumulativeAckCounter() {
			int tmp = _cumAckCounter;
			_cumAckCounter = 0;
			return tmp;
		}

		public synchronized void incOutOfSequenceCounter() {
			_outOfSeqCounter++;
		}

		public synchronized int getOutOfSequenceCounter() {
			return _outOfSeqCounter;
		}

		public synchronized int getAndResetOutOfSequenceCounter() {
			int tmp = _outOfSeqCounter;
			_outOfSeqCounter = 0;
			return tmp;
		}

		public synchronized void incOutstandingSegsCounter() {
			_outSegsCounter++;
		}

		public synchronized int getOutstandingSegsCounter() {
			return _outSegsCounter;
		}

		public synchronized int getAndResetOutstandingSegsCounter() {
			int tmp = _outSegsCounter;
			_outSegsCounter = 0;
			return tmp;
		}

		public synchronized void reset() {
			_outOfSeqCounter = 0;
			_outSegsCounter = 0;
			_cumAckCounter = 0;
		}
	}


	/**
	 * 客户端线程。
	 */
	private class ReliableSocketThread extends Thread {
		public ReliableSocketThread() {
			super("ReliableSocket");
			setDaemon(true);
		}

		@Override
		public void run() {
			Segment segment;
			try {
				while ((segment = receiveSegment()) != null) {

					if (segment instanceof SYNSegment) {
						handleSYNSegment((SYNSegment) segment);
					} else if (segment instanceof EAKSegment) {
						handleEAKSegment((EAKSegment) segment);
					} else if (segment instanceof ACKSegment) {
						// do nothing.
					} else {
						handleSegment(segment);
					}

					checkAndGetAck(segment);
				}
			} catch (IOException xcp) {
				xcp.printStackTrace();
			}
		}
	}

	private class NullSegmentTimerTask implements Runnable {
		@Override
		public void run() {
			// 如果没有需要重传的包，则发送一个 NULL 包。
			synchronized (_unackedSentQueue) {
				if (_unackedSentQueue.isEmpty()) {
					try {
						sendAndQueueSegment(new NULSegment(_counters.nextSequenceNumber()));
					} catch (IOException xcp) {
						if (DEBUG) {
							xcp.printStackTrace();
						}
					}
				}
			}
		}
	}

	private class RetransmissionTimerTask implements Runnable {
		@Override
		public void run() {
			synchronized (_unackedSentQueue) {
				Iterator<Segment> it = _unackedSentQueue.iterator();
				while (it.hasNext()) {
					Segment s = (Segment) it.next();
					try {
						retransmitSegment(s);
					} catch (IOException xcp) {
						xcp.printStackTrace();
					}
				}
			}
		}
	}

	private class CumulativeAckTimerTask implements Runnable {
		@Override
		public void run() {
			sendAck();
		}
	}

	private class KeepAliveTimerTask implements Runnable {
		@Override
		public void run() {
			connectionFailure();
		}
	}

	private class ShutdownHook extends Thread {
		public ShutdownHook() {
			super("ReliableSocket-ShutdownHook");
		}

		@Override
		public void run() {
			try {
				switch (_state) {
				case CLOSED:
					return;
				default:
					sendSegment(new FINSegment(_counters.nextSequenceNumber()));
					break;
				}
			} catch (Throwable t) {
				// ignore exception
			}
		}
	}
}
