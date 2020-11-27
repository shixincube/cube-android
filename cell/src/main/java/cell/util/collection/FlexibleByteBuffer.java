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

package cell.util.collection;

import java.nio.ByteBuffer;

import cell.util.log.Logger;

/**
 * 支持弹性空间调整的字节缓存。
 */
public class FlexibleByteBuffer implements Comparable<FlexibleByteBuffer> {

	private ByteBuffer buf;

	/**
	 * 构造函数。
	 */
	public FlexibleByteBuffer() {
		this.buf = ByteBuffer.allocate(8192);
	}

	/**
	 * 构造函数。
	 *
	 * @param initCapacity 初始化容量。
	 */
	public FlexibleByteBuffer(int initCapacity) {
		this.buf = ByteBuffer.allocate(initCapacity);
	}

	/**
	 * 返回数据游标 position 位置。
	 *
	 * @return 返回数据游标 position 位置。
	 */
	public int position() {
		return this.buf.position();
	}

	/**
	 * 定位数据游标。
	 *
	 * @param newPosition 新的游标位置。
	 * @return 返回 {@code this} 引用。
	 */
	public FlexibleByteBuffer position(int newPosition) {
		this.buf.position(newPosition);
		return this;
	}

	/**
	 * 返回 limit 限制位。
	 *
	 * @return 返回 limit 限制位。
	 */
	public int limit() {
		return this.buf.limit();
	}

	/**
	 * 设置新的限制位。
	 *
	 * @param newLimit 新的限制位位置。
	 * @return 返回 {@code this} 引用。
	 */
	public FlexibleByteBuffer limit(int newLimit) {
		this.buf.limit(newLimit);
		return this;
	}

	/**
	 * 返回数据游标距离限制位的长度。
	 *
	 * @return 返回数据游标距离限制位的长度。
	 */
	public int remaining() {
		return this.buf.remaining();
	}

	/**
	 * 返回当前的容量。
	 *
	 * @return 返回当前的容量。
	 */
	public int capacity() {
		return this.buf.capacity();
	}

	/**
	 * 写入数据并后移游标。如果当前容量不足以容纳数据将重置缓存容量，以保证数据被写入。
	 *
	 * @param src 数据。
	 * @return 返回 {@code this} 引用。
	 */
	public FlexibleByteBuffer put(byte src) {
		if (1 > this.buf.remaining()) {
			// 重置缓存
			this.reset(this.buf.position() + 1);
		}
		this.buf.put(src);
		return this;
	}

	/**
	 * 写入数据并后移游标。如果当前容量不足以容纳数据将重置缓存容量，以保证数据被写入。
	 *
	 * @param src 数据。
	 * @return 返回 {@code this} 引用。
	 */
	public FlexibleByteBuffer put(byte[] src) {
		if (src.length > this.buf.remaining()) {
			// 重置缓存
			this.reset(this.buf.position() + src.length);
		}
		this.buf.put(src);
		return this;
	}

	/**
	 * 写入数据并后移游标。如果当前容量不足以容纳数据将重置缓存容量，以保证数据被写入。
	 *
	 * @param src 数据源。
	 * @param offset 数据偏移。
	 * @param length 数据长度。
	 * @return 返回 {@code this} 引用。
	 */
	public FlexibleByteBuffer put(byte[] src, int offset, int length) {
		if (length > this.buf.remaining()) {
			// 重置缓存
			this.reset(this.buf.position() + length);
		}
		this.buf.put(src, offset, length);
		return this;
	}

	/**
	 * 写入数据并后移游标。如果当前容量不足以容纳数据将重置缓存容量，以保证数据被写入。
	 *
	 * @param buf 源缓存。
	 * @return 返回 {@code this} 引用。
	 */
	public FlexibleByteBuffer put(FlexibleByteBuffer buf) {
		int len = Math.max(buf.position(), buf.limit());
		if (len > this.buf.remaining()) {
			// 重置缓存
			this.reset(this.buf.position() + len);
		}
		this.buf.put(buf.getBuffer());
		return this;
	}

	/**
	 * 返回当前游标位置的数据并后移游标。
	 *
	 * @return 返回当前游标位置的数据并后移游标。
	 */
	public byte get() {
		return this.buf.get();
	}

	/**
	 * 返回指定位置处的数据。
	 *
	 * @param index 指定位置。
	 * @return 返回指定位置处的数据。
	 */
	public byte get(int index) {
		return this.buf.get(index);
	}

	/**
	 * 将缓存内的数据复制到输入的字节数组。
	 *
	 * @param array 字节数组。
	 * @return 返回 {@code this} 引用。
	 */
	public FlexibleByteBuffer get(byte[] array) {
		this.buf.get(array);
		return this;
	}

	/**
	 * 整理数据，设定限制位到当前游标，并将游标重置。
	 *
	 * @return 返回 {@code this} 引用。
	 */
	public FlexibleByteBuffer flip() {
		this.buf.flip();
		return this;
	}

	/**
	 * 将整个缓存的数据返回。
	 *
	 * @return 返回整个缓存的数据。
	 */
	public byte[] array() {
		return this.buf.array();
	}

	/**
	 * 复位所有标记，实现清空数据的效果。
	 *
	 * @return 返回 {@code this} 引用。
	 */
	public FlexibleByteBuffer clear() {
		this.buf.clear();
		return this;
	}

	/**
	 * 返回内部的 ByteBuffer 实例。
	 *
	 * @return 返回内部的 ByteBuffer 实例。
	 */
	public ByteBuffer getBuffer() {
		return this.buf;
	}

	@Override
	public int compareTo(FlexibleByteBuffer o) {
		return this.buf.limit() - o.buf.limit();
	}

	/**
	 * 重置缓存容量空间。
	 * 新产生的空间大小总是2的倍数。
	 *
	 * @param newSize 新的空间大小。
	 */
	private void reset(int newSize) {
		ByteBuffer newBuf = ByteBuffer.allocate((newSize % 2 == 0) ? newSize : newSize + 1);
		if (this.buf.position() != 0) {
			this.buf.flip();
			newBuf.put(this.buf);
		}
		this.buf.clear();
		this.buf = newBuf;

		if (Logger.isDebugLevel()) {
//			Logger.d(this.getClass(), "Reset buffer size: " + this.buf.capacity());
		}
	}

}
