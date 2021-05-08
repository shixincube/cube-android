package cell.util.rudp.impl;

/*
 *  Data Segment
 *
 *   0 1 2 3 4 5 6 7 8            15
 *  +-+-+-+-+-+-+-+-+---------------+
 *  |0|1|0|0|0|0|0|0|       6       |
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | Sequence #    |   Ack Number  |
 *  +---------------+---------------+
 *  |           Checksum            |
 *  +---------------+---------------+
 *  | ...                           |
 *  +-------------------------------+
 *
 */
public class DATSegment extends cell.util.rudp.impl.Segment {

	private byte[] _data;

	protected DATSegment() {
	}

	/**
	 * 
	 * @param seqn
	 * @param ackn
	 * @param b
	 * @param off
	 * @param len
	 */
	public DATSegment(int seqn, int ackn, byte[] b, int off, int len) {
		init(cell.util.rudp.impl.Segment.ACK_FLAG, seqn, cell.util.rudp.impl.Segment.RUDP_HEADER_LEN);
		setAck(ackn);
		_data = new byte[len];
		System.arraycopy(b, off, _data, 0, len);
	}

	@Override
	public int length() {
		return _data.length + super.length();
	}

	@Override
	public String type() {
		return "DAT";
	}

	public byte[] getData() {
		return _data;
	}

	@Override
	public byte[] getBytes() {
		byte[] buffer = super.getBytes();
		System.arraycopy(_data, 0, buffer, cell.util.rudp.impl.Segment.RUDP_HEADER_LEN, _data.length);
		return buffer;
	}

	@Override
	public void parseBytes(byte[] buffer, int off, int len) {
		super.parseBytes(buffer, off, len);
		_data = new byte[len - cell.util.rudp.impl.Segment.RUDP_HEADER_LEN];
		System.arraycopy(buffer, off + cell.util.rudp.impl.Segment.RUDP_HEADER_LEN, _data, 0, _data.length);
	}

}
