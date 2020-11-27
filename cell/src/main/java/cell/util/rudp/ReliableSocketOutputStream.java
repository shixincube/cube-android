package cell.util.rudp;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 实现 RUDP 套接字输出流。
 */
class ReliableSocketOutputStream extends OutputStream {

	protected ReliableSocket _sock;

	protected byte[] _buf;

	protected int _count;

	/**
	 * 创建一个 ReliableSocketOutputStream 实例。
	 * 
	 * @param sock 用于发送数据的的 RUDP 套接字。
	 * @throws IOException
	 */
	public ReliableSocketOutputStream(ReliableSocket sock) throws IOException {
		if (sock == null) {
			throw new NullPointerException("sock");
		}

		_sock = sock;
		_buf = new byte[_sock.getSendBufferSize()];
		_count = 0;
	}

	@Override
	public synchronized void write(int b) throws IOException {
		if (_count >= _buf.length) {
			flush();
		}

		_buf[_count++] = (byte) (b & 0xFF);
	}

	@Override
	public synchronized void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public synchronized void write(byte[] b, int off, int len)
			throws IOException {
		if (b == null) {
			throw new NullPointerException();
		}

		if (off < 0 || len < 0 || (off + len) > b.length) {
			throw new IndexOutOfBoundsException();
		}

		int buflen;
		int writtenBytes = 0;

		while (writtenBytes < len) {
			buflen = Math.min(_buf.length, len - writtenBytes);
			if (buflen > (_buf.length - _count)) {
				flush();
			}
			System.arraycopy(b, off + writtenBytes, _buf, _count, buflen);
			_count += buflen;
			writtenBytes += buflen;
		}
	}

	@Override
	public synchronized void flush() throws IOException {
		if (_count > 0) {
			_sock.write(_buf, 0, _count);
			_count = 0;
		}
	}

	@Override
	public synchronized void close() throws IOException {
		flush();
		_sock.shutdownOutput();
	}
}
