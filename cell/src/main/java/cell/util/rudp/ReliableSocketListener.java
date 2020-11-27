package cell.util.rudp;

/**
 * 套接字事件监听器。
 */
public interface ReliableSocketListener {
	/**
	 * 当一个数据包发出时回调。
	 */
	public void packetSent();

	/**
	 * 当一个数据包重传时回调。
	 */
	public void packetRetransmitted();

	/**
	 * 
	 * 当按顺序接收到一个数据包时回调。
	 */
	public void packetReceivedInOrder();

	/**
	 * 当接收到无序数据包时回调。
	 */
	public void packetReceivedOutOfOrder();

}
