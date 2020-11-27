package cell.util.rudp;

/**
 * 套接字状态事件监听器。
 */
public interface ReliableSocketStateListener {
	/**
	 * 当连接打开时被回调。
	 */
	public void connectionOpened(ReliableSocket sock);

	/**
	 * 当尝试连接被拒绝时被回调。
	 */
	public void connectionRefused(ReliableSocket sock);

	/**
	 * 当连接关闭时被回调。
	 */
	public void connectionClosed(ReliableSocket sock);

	/**
	 * 当已建立的连接失效时被回调。
	 */
	public void connectionFailure(ReliableSocket sock);

	/**
	 * 当重置连接时被回调。
	 */
	public void connectionReset(ReliableSocket sock);
}
