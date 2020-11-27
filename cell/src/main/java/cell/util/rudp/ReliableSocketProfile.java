package cell.util.rudp;

/**
 * RUDP 参数描述配置文件。
 * 
 * @see cell.util.rudp.ReliableSocket
 */
public class ReliableSocketProfile {

	public final static int MAX_SEND_QUEUE_SIZE = 64;
	public final static int MAX_RECV_QUEUE_SIZE = 64;

	public final static int MAX_SEGMENT_SIZE = 128 * 10;
	public final static int MAX_OUTSTANDING_SEGS = 3;
	public final static int MAX_RETRANS = 3;
	public final static int MAX_CUMULATIVE_ACKS = 3;
	public final static int MAX_OUT_OF_SEQUENCE = 3;
	public final static int MAX_AUTO_RESET = 3;
	public final static int NULL_SEGMENT_TIMEOUT = 2000;
	public final static int RETRANSMISSION_TIMEOUT = 600;
	public final static int CUMULATIVE_ACK_TIMEOUT = 300;

	private int _maxSendQueueSize;
	private int _maxRecvQueueSize;
	private int _maxSegmentSize;
	private int _maxOutstandingSegs;
	private int _maxRetrans;
	private int _maxCumulativeAcks;
	private int _maxOutOfSequence;
	private int _maxAutoReset;
	private int _nullSegmentTimeout;
	private int _retransmissionTimeout;
	private int _cumulativeAckTimeout;

	/**
	 * 使用默认的 RUDP 参数值创建配置文件。
	 * 
	 * 根据 RUDP 协议草案，默认的最大重传次数为3次。
	 * 但是，如果数据包丢失太高，除非发送方继续重新传输尚未确认的数据包，否则连接可能会停止。
	 * 我们将使用 0，这意味着不受限制。
	 */
	public ReliableSocketProfile() {
		this(MAX_SEND_QUEUE_SIZE, MAX_RECV_QUEUE_SIZE, MAX_SEGMENT_SIZE,
				MAX_OUTSTANDING_SEGS, 0/* MAX_RETRANS */, MAX_CUMULATIVE_ACKS,
				MAX_OUT_OF_SEQUENCE, MAX_AUTO_RESET, NULL_SEGMENT_TIMEOUT,
				RETRANSMISSION_TIMEOUT, CUMULATIVE_ACK_TIMEOUT);
	}

	/**
	 * 使用指定的 RUDP 参数值创建配置文件。
	 * 
	 * @param maxSendQueueSize 最大发送队列长度（包数量）。
	 * @param maxRecvQueueSize 最大接收队列长度（包数量）。
	 * @param maxSegmentSize 最大包段长度（字节）（最小值22）。
	 * @param maxOutstandingSegs 最大未完成传输包段数量。
	 * @param maxRetrans 连续重发的最大数量（0表示无限制）。
	 * @param maxCumulativeAcks 未确认接收包段的最大数量。
	 * @param maxOutOfSequence 未按顺序接收到的包段的最大数量。
	 * @param maxAutoReset 连续自动重置的最大次数（未使用）。
	 * @param nullSegmentTimeout 空包段超时时长（毫秒）。
	 * @param retransmissionTimeout 重传超时时长（毫秒）。
	 * @param cumulativeAckTimeout 累计确认超时时长（毫秒）。
	 */
	public ReliableSocketProfile(int maxSendQueueSize, int maxRecvQueueSize,
			int maxSegmentSize, int maxOutstandingSegs, int maxRetrans,
			int maxCumulativeAcks, int maxOutOfSequence, int maxAutoReset,
			int nullSegmentTimeout, int retransmissionTimeout,
			int cumulativeAckTimeout) {
		checkValue("maxSendQueueSize", maxSendQueueSize, 1, 255);
		checkValue("maxRecvQueueSize", maxRecvQueueSize, 1, 255);
		checkValue("maxSegmentSize", maxSegmentSize, 22, 65535);
		checkValue("maxOutstandingSegs", maxOutstandingSegs, 1, 255);
		checkValue("maxRetrans", maxRetrans, 0, 255);
		checkValue("maxCumulativeAcks", maxCumulativeAcks, 0, 255);
		checkValue("maxOutOfSequence", maxOutOfSequence, 0, 255);
		checkValue("maxAutoReset", maxAutoReset, 0, 255);
		checkValue("nullSegmentTimeout", nullSegmentTimeout, 0, 65535);
		checkValue("retransmissionTimeout", retransmissionTimeout, 100, 65535);
		checkValue("cumulativeAckTimeout", cumulativeAckTimeout, 100, 65535);

		_maxSendQueueSize = maxSendQueueSize;
		_maxRecvQueueSize = maxRecvQueueSize;
		_maxSegmentSize = maxSegmentSize;
		_maxOutstandingSegs = maxOutstandingSegs;
		_maxRetrans = maxRetrans;
		_maxCumulativeAcks = maxCumulativeAcks;
		_maxOutOfSequence = maxOutOfSequence;
		_maxAutoReset = maxAutoReset;
		_nullSegmentTimeout = nullSegmentTimeout;
		_retransmissionTimeout = retransmissionTimeout;
		_cumulativeAckTimeout = cumulativeAckTimeout;
	}

	/**
	 * 返回最大发送队列长度（包数量）。
	 */
	public int maxSendQueueSize() {
		return _maxSendQueueSize;
	}

	/**
	 * 返回最大接收队列长度（包数量）。
	 */
	public int maxRecvQueueSize() {
		return _maxRecvQueueSize;
	}

	/**
	 * 返回最大包段长度（字节）。
	 */
	public int maxSegmentSize() {
		return _maxSegmentSize;
	}

	/**
	 * 返回最大未完成传输包段数量。
	 */
	public int maxOutstandingSegs() {
		return _maxOutstandingSegs;
	}

	/**
	 * 返回连续重发的最大数量（0表示无限制）。
	 */
	public int maxRetrans() {
		return _maxRetrans;
	}

	/**
	 * 返回未确认接收包段的最大数量。
	 */
	public int maxCumulativeAcks() {
		return _maxCumulativeAcks;
	}

	/**
	 * 返回未按顺序接收到的包段的最大数量。
	 */
	public int maxOutOfSequence() {
		return _maxOutOfSequence;
	}

	/**
	 * 返回连续自动重置的最大次数。
	 */
	public int maxAutoReset() {
		return _maxAutoReset;
	}

	/**
	 * 返回空包段超时时长（毫秒）。
	 */
	public int nullSegmentTimeout() {
		return _nullSegmentTimeout;
	}

	/**
	 * 返回重传超时时长（毫秒）。
	 */
	public int retransmissionTimeout() {
		return _retransmissionTimeout;
	}

	/**
	 * 返回累计确认超时时长（毫秒）。
	 */
	public int cumulativeAckTimeout() {
		return _cumulativeAckTimeout;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(_maxSendQueueSize).append(", ");
		sb.append(_maxRecvQueueSize).append(", ");
		sb.append(_maxSegmentSize).append(", ");
		sb.append(_maxOutstandingSegs).append(", ");
		sb.append(_maxRetrans).append(", ");
		sb.append(_maxCumulativeAcks).append(", ");
		sb.append(_maxOutOfSequence).append(", ");
		sb.append(_maxAutoReset).append(", ");
		sb.append(_nullSegmentTimeout).append(", ");
		sb.append(_retransmissionTimeout).append(", ");
		sb.append(_cumulativeAckTimeout);
		sb.append("]");
		return sb.toString();
	}

	private void checkValue(String param, int value, int minValue, int maxValue) {
		if (value < minValue || value > maxValue) {
			throw new IllegalArgumentException(param);
		}
	}

}
