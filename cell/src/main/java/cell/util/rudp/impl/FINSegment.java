package cell.util.rudp.impl;

/*
 *  FIN Segment
 *
 *   0 1 2 3 4 5 6 7 8            15
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | |A| | | | | | |               |
 *  |0|C|0|0|0|0|1|0|        6      |
 *  | |K| | | | | | |               |
 *  +-+-+-+-+-+-+-+-+---------------+
 *  | Sequence #    |   Ack Number  |
 *  +---------------+---------------+
 *  |         Header Checksum       |
 *  +---------------+---------------+
 *
 */
public class FINSegment extends cell.util.rudp.impl.Segment {

	protected FINSegment() {
	}

	public FINSegment(int seqn) {
		init(cell.util.rudp.impl.Segment.FIN_FLAG, seqn, cell.util.rudp.impl.Segment.RUDP_HEADER_LEN);
	}

	@Override
	public String type() {
		return "FIN";
	}

}
