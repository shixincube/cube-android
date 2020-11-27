package cell.util.rudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;

import cell.util.rudp.impl.SYNSegment;
import cell.util.rudp.impl.Segment;

/**
 * 实现基于 RUDP 协议的服务器端套接字。
 * 
 * @see java.net.ServerSocket
 */
public class ReliableServerSocket extends ServerSocket {

	private DatagramSocket _serverSock;
	private int _timeout;
	private int _backlogSize;
	private boolean _closed;

	/*
	 * 监听的积压队列。
	 */
	private ArrayList<ReliableSocket> _backlog;

	/*
	 * 被管理的活跃客户端套接字表。
	 */
	private HashMap<SocketAddress, ReliableClientSocket> _clientSockTable;

	private ReliableSocketStateListener _stateListener;

	private static final int DEFAULT_BACKLOG_SIZE = 50;

	/**
	 * 创建未绑定端口的 RUDP 服务器套接字。
	 * @throws IOException 如果打开底层 UDP 协议套接字失败抛出 I/O 异常。
	 * 
	 * @see java.net.ServerSocket#ServerSocket()
	 */
	public ReliableServerSocket() throws IOException {
		this(0, 0, null);
	}

	/**
	 * 创建绑定指定端口的 RUDP 服务器套接字。
	 * 如果指定端口为 <code>0</code> 则使用任意闲置端口。
	 * 套接字连接默认最大队列长度为 <code>50</code>。
	 * 如果连接达到最大队列长度，新连接将被拒绝。
	 * 
	 * 
	 * @param port 指定端口。
	 * @throws IOException 如果打开底层 UDP 协议套接字失败抛出 I/O 异常。
	 * @see java.net.ServerSocket#ServerSocket(int)
	 */
	public ReliableServerSocket(int port) throws IOException {
		this(port, 0, null);
	}

	/**
	 * 创建指定端口和 Backlog 大小的 RUDP 服务器套接字。
	 * 如果指定端口为 <code>0</code> 则使用任意闲置端口。
	 * 
	 * @param port 指定端口。
	 * @param backlog 指定监听队列长度。
	 * @throws IOException 如果打开底层 UDP 协议套接字失败抛出 I/O 异常。
	 * @see java.net.ServerSocket#ServerSocket(int, int)
	 */
	public ReliableServerSocket(int port, int backlog) throws IOException {
		this(port, backlog, null);
	}

	/**
	 * 创建指定端口、指定 Backlog 和指定绑定地址的 RUDP 服务器套接字。
	 * 如果绑定地址 bindAddr 设置为 <code>null</code> 则使用所有本地地址绑定。
	 * 
	 * @param port 指定端口。
	 * @param backlog 指定监听队列长度。
	 * @param bindAddr 指定需绑定的本地地址。
	 * @throws IOException 如果打开底层 UDP 协议套接字失败抛出 I/O 异常。
	 * @see java.net.ServerSocket#ServerSocket(int, int, InetAddress)
	 */
	public ReliableServerSocket(int port, int backlog, InetAddress bindAddr)
			throws IOException {
		this(new DatagramSocket(new InetSocketAddress(bindAddr, port)), backlog);
	}

	/**
	 * 创建指定 UDP 套接字，并指定 Backlog 大小创建 RUDP 服务器套接字。
	 * 
	 * @param sock 指定底层 UDP 套接字。
	 * @param backlog 指定监听队列长度。
	 * @throws IOException 如果出错抛出 I/O 异常。
	 */
	public ReliableServerSocket(DatagramSocket sock, int backlog)
			throws IOException {
		if (sock == null) {
			throw new NullPointerException("sock");
		}

		_serverSock = sock;
		_backlogSize = (backlog <= 0) ? DEFAULT_BACKLOG_SIZE : backlog;
		_backlog = new ArrayList<ReliableSocket>(_backlogSize);
		_clientSockTable = new HashMap<SocketAddress, ReliableClientSocket>();
		_stateListener = new StateListener();
		_timeout = 0;
		_closed = false;

		new ReceiverThread().start();
	}

	@Override
	public Socket accept() throws IOException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		synchronized (_backlog) {
			while (_backlog.isEmpty()) {
				try {
					if (_timeout == 0) {
						_backlog.wait();
					} else {
						long startTime = System.currentTimeMillis();
						_backlog.wait(_timeout);
						if (System.currentTimeMillis() - startTime >= _timeout) {
							throw new SocketTimeoutException();
						}
					}
				} catch (InterruptedException xcp) {
					xcp.printStackTrace();
				}

				if (isClosed()) {
					throw new IOException();
				}
			}

			return (Socket) _backlog.remove(0);
		}
	}

	@Override
	public synchronized void bind(SocketAddress endpoint) throws IOException {
		bind(endpoint, 0);
	}

	@Override
	public synchronized void bind(SocketAddress endpoint, int backlog)
			throws IOException {
		if (isClosed()) {
			throw new SocketException("Socket is closed");
		}

		_serverSock.bind(endpoint);
	}

	@Override
	public synchronized void close() {
		if (isClosed()) {
			return;
		}

		_closed = true;
		synchronized (_backlog) {
			_backlog.clear();
			_backlog.notify();
		}

		if (_clientSockTable.isEmpty()) {
			_serverSock.close();
		}
	}

	public synchronized void forceClose() {
		ArrayList<ReliableClientSocket> list = new ArrayList<>(_clientSockTable.values());
		for (ReliableClientSocket sock : list) {
			if (!sock.isClosed()) {
				try {
					sock.close();
				} catch (IOException e) {
					// Nothing
				}
			}
		}

		this.close();

		try {
			_serverSock.close();
		} catch (Exception e) {
			// Nothing
		}
	}

	@Override
	public InetAddress getInetAddress() {
		return _serverSock.getInetAddress();
	}

	@Override
	public int getLocalPort() {
		return _serverSock.getLocalPort();
	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		return _serverSock.getLocalSocketAddress();
	}

	@Override
	public boolean isBound() {
		return _serverSock.isBound();
	}

	@Override
	public boolean isClosed() {
		return _closed;
	}

	@Override
	public void setSoTimeout(int timeout) {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout < 0");
		}

		_timeout = timeout;
	}

	@Override
	public int getSoTimeout() {
		return _timeout;
	}

	/**
	 * 注册一个指定终端地址的新客户端套接字。
	 * 
	 * @param endpoint 新的终端套接字地址。
	 * @return 注册成功的客户端套接字。
	 */
	private ReliableClientSocket addClientSocket(SocketAddress endpoint) {
		synchronized (_clientSockTable) {
			ReliableClientSocket sock = (ReliableClientSocket) _clientSockTable.get(endpoint);

			if (sock == null) {
				try {
					sock = new ReliableClientSocket(_serverSock, endpoint);
					sock.addStateListener(_stateListener);
					_clientSockTable.put(endpoint, sock);
				} catch (IOException xcp) {
					xcp.printStackTrace();
				}
			}

			return sock;
		}
	}

	/**
	 * 注销指定终端地址的客户端套接字。
	 * 
	 * @param endpoint 终端的套接字地址。
	 * @return 已注销的客户端套接字。
	 */
	private ReliableClientSocket removeClientSocket(SocketAddress endpoint) {
		synchronized (_clientSockTable) {
			ReliableClientSocket sock = (ReliableClientSocket) _clientSockTable.remove(endpoint);

			if (_clientSockTable.isEmpty()) {
				if (isClosed()) {
					_serverSock.close();
				}
			}

			return sock;
		}
	}


	/**
	 * 接收数据线程。
	 */
	private class ReceiverThread extends Thread {
		public ReceiverThread() {
			super("ReliableServerSocket");
			setDaemon(true);
		}

		@Override
		public void run() {
			byte[] buffer = new byte[65535];

			while (true) {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				ReliableClientSocket sock = null;

				try {
					_serverSock.receive(packet);
					SocketAddress endpoint = packet.getSocketAddress();
					Segment s = Segment.parse(packet.getData(), 0, packet.getLength());

					synchronized (_clientSockTable) {

						if (!isClosed()) {
							if (s instanceof SYNSegment) {
								// 尝试将终端加入到活跃列表里
								if (!_clientSockTable.containsKey(endpoint)) {
									sock = addClientSocket(endpoint);
								}
							}
						}

						sock = (ReliableClientSocket) _clientSockTable.get(endpoint);
					}

					if (sock != null) {
						sock.segmentReceived(s);
					}
				} catch (IOException xcp) {
					if (isClosed()) {
						break;
					}
					xcp.printStackTrace();
				}
			}
		}
	}

	
	/**
	 * 服务器套接字使用的客户端结构。
	 */
	public class ReliableClientSocket extends ReliableSocket {

		private ArrayList<Segment> _queue;

		public ReliableClientSocket(DatagramSocket sock, SocketAddress endpoint)
				throws IOException {
			super(sock);
			_endpoint = endpoint;
		}

		@Override
		protected void init(DatagramSocket sock, ReliableSocketProfile profile) {
			_queue = new ArrayList<Segment>();
			super.init(sock, profile);
		}

		@Override
		protected Segment receiveSegmentImpl() {
			synchronized (_queue) {
				while (_queue.isEmpty()) {
					try {
						_queue.wait();
					} catch (InterruptedException xcp) {
						xcp.printStackTrace();
					}
				}

				return (Segment) _queue.remove(0);
			}
		}

		protected void segmentReceived(Segment s) {
			synchronized (_queue) {
				_queue.add(s);
				_queue.notify();
			}
		}

		@Override
		protected void closeSocket() {
			synchronized (_queue) {
				_queue.clear();
				_queue.add(null);
				_queue.notify();
			}
		}

		@Override
		protected void log(String msg) {
			System.out.println(getPort() + ": " + msg);
		}

	}

	private class StateListener implements ReliableSocketStateListener {

		public StateListener() {
		}

		@Override
		public void connectionOpened(ReliableSocket sock) {
			if (sock instanceof ReliableClientSocket) {
				synchronized (_backlog) {
					while (_backlog.size() > DEFAULT_BACKLOG_SIZE) {
						try {
							_backlog.wait();
						} catch (InterruptedException xcp) {
							xcp.printStackTrace();
						}
					}

					_backlog.add(sock);
					_backlog.notify();
				}
			}
		}

		@Override
		public void connectionRefused(ReliableSocket sock) {
			// do nothing.
		}

		@Override
		public void connectionClosed(ReliableSocket sock) {
			// 从活跃连接表里移除客户端套接字。
			if (sock instanceof ReliableClientSocket) {
				removeClientSocket(sock.getRemoteSocketAddress());
			}
		}

		@Override
		public void connectionFailure(ReliableSocket sock) {
			// 从活跃连接表里移除客户端套接字。
			if (sock instanceof ReliableClientSocket) {
				removeClientSocket(sock.getRemoteSocketAddress());
			}
		}

		@Override
		public void connectionReset(ReliableSocket sock) {
			// do nothing.
		}
	}
}
