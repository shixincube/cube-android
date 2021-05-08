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

import java.util.List;

import cell.util.collection.FlexibleByteBuffer;

/**
 * 消息描述类。
 */
public class Message {

	/**
	 * 系统默认的消息头格式。
	 */
	public final static byte[] DefaultHead = new byte[]{ 0x20, 0x13, 0x09, 0x08 };

	/**
	 * 系统默认的消息尾格式。
	 */
	public final static byte[] DefaultTail = new byte[]{ 0x19, 0x78, 0x10, 0x04 };

	/**
	 * 压缩阀值。最小有效值为 120 。
	 */
	public final static int CompressionThreshold = 1024;

	/** 存储消息数据的数组。 */
	protected byte[] payload = null;

	/** 是否启用数据压缩。 */
	private boolean compressible = false;

	/** 自定义上下文对象。 */
	protected Object context = null;

	/**
	 * 构造函数。
	 *
	 * @param payload 指定消息有效负载数据。
	 */
	public Message(byte[] payload) {
		this.payload = new byte[payload.length];
		System.arraycopy(payload, 0, this.payload, 0, payload.length);
	}

	/**
	 * 构造函数。
	 * 
	 * @param payload 指定消息有效负载数据。
	 * @param offset 指定数据数组的偏移位置。
	 * @param length 指定数据数组的有效数据长度。
	 */
	public Message(byte[] payload, int offset, int length) {
		this.payload = new byte[length];
		System.arraycopy(payload, offset, this.payload, 0, length);
	}

	/**
	 * 校验负载是否是压缩格式。
	 * 
	 * @return 返回是否压缩。
	 */
	public boolean verifyCompression() {
		if (this.payload[0] == 120 && this.payload[1] == -100) {
			this.compressible = true;
		}
		else {
			this.compressible = false;
		}
		return this.compressible;
	}

	/**
	 * 是否压缩。
	 * 
	 * @return 返回是否压缩。
	 */
	public boolean compressible() {
		return this.compressible;
	}

	/**
	 * 是否需要压缩。
	 * 
	 * @return 返回是否需要压缩。
	 */
	public boolean needCompressible() {
		return (this.payload.length >= Message.CompressionThreshold);
	}

	/**
	 * 获得消息有效负载数据。
	 * 
	 * @return 返回消息有效负载数据。
	 */
	public byte[] getPayload() {
		return this.payload;
	}

	/**
	 * 设置消息有效负载数据。
	 *
	 * @param payload 指定新的有效负载数据。
	 * @return 返回消息自己的实例。
	 */
	public Message setPayload(byte[] payload) {
		this.payload = payload;
		return this;
	}

	/**
	 * 整个消息的数据总长度。
	 * 
	 * @return 消息数据长度。
	 */
	public int length() {
		return this.payload.length;
	}

	/**
	 * 设置消息的自定义上下文对象。
	 * 
	 * @param context 指定上下文对象。
	 */
	public void setContext(Object context) {
		this.context = context;
	}

	/**
	 * 获得消息的自定义上下文对象。
	 * 
	 * @return 返回消息的自定义上下文对象。
	 */
	public Object getContext() {
		return this.context;
	}

	/**
	 * 将消息数据复制到指定的缓存。
	 * 
	 * @param dest 指定接收数据的字节缓存。
	 * @param message 指定消息数据。
	 * @return 返回复制的数据长度。
	 */
	public static int pack(FlexibleByteBuffer dest, Message message) {
		int length = DefaultHead.length + message.payload.length + DefaultTail.length;
		dest.put(DefaultHead);
		dest.put(message.payload);
		dest.put(DefaultTail);
		return length;
	}

	/**
	 * 将消息数据复制到指定的字节数组。
	 * 
	 * @param dest 指定接收数据的字节数组。
	 * @param message 指定消息数据。
	 * @return 返回复制的数据长度。
	 */
	public static int pack(byte[] dest, Message message) {
		int length = DefaultHead.length + message.payload.length + DefaultTail.length;
		if (dest.length < length) {
			return -1;
		}

		System.arraycopy(DefaultHead, 0, dest, 0, DefaultHead.length);
		System.arraycopy(message.payload, 0, dest, DefaultHead.length, message.payload.length);
		System.arraycopy(DefaultTail, 0, dest, DefaultHead.length + message.payload.length, DefaultTail.length);
		return length;
	}

	/**
	 * 从指定的数据源提取符合格式的 Message 数据对象列表。
	 *
	 * @param source 数据源。
	 * @param remains 剩余的数据缓存。
	 * @param result 保存提取结果。
	 * @return 返回数据提取结果列表。
	 */
	public static List<Message> extract(FlexibleByteBuffer source, FlexibleByteBuffer remains, List<Message> result) {
		if (source.position() != 0) {
			source.flip();
		}

		byte[] src = new byte[source.limit()];
		System.arraycopy(source.array(), 0, src, 0, source.limit());

		int headState = -1;
		int tailState = -1;
		int exindex = 0;

		do {
			for (int i = 0; i < src.length; ++i) {
				tailState = -1;
				headState = compareBytes(src, i, DefaultHead, 0, DefaultHead.length);
				if (0 == headState) {
					// 匹配
					// 删除之前的缓存
					remains.clear();

					if (DefaultHead.length == src.length) {
						// 数据尾后面仅有数据头标识
						break;
					}

					// 定位数据当前位置
					int headIndex = i + DefaultHead.length;

					// 查找数据尾
					for (int j = headIndex; j < src.length; ++j) {
						tailState = compareBytes(src, j, DefaultTail, 0, DefaultTail.length);
						if (0 == tailState) {
							remains.clear();
							// 匹配，提取数据
							byte[] payload = new byte[j - headIndex];
							System.arraycopy(src, headIndex, payload, 0, payload.length);
							result.add(new Message(payload));
							exindex = j + DefaultTail.length;
							break;
						}
						else if (1 == tailState) {
							// 越界，从 headIndex 位置开始的数据都保留下来
							src = rebuildBytes(src, headIndex);
							remains.clear();
							remains.put(src);
							break;
						}
						else {
							// 不匹配，继续查找
							remains.put(src[j]);
							continue;
						}
					}

					// 退出本次循环
					break;
				}
				else if (1 == headState) {
					// 越界，丢弃前面的数据
					src = rebuildBytes(src, i);
					remains.put(src);
					break;
				}
				else {
					// 不匹配，继续查找，直到数据越界
					remains.put(src[i]);
					continue;
				}
			}

			if (0 == headState && 0 == tailState) {
				src = rebuildBytes(src, exindex);
			}
			else if (0 == headState && 0 != tailState) {
				// 找到头，没有找到尾
				remains.flip();
				int len = remains.limit();
				if (len > 0) {
					byte[] tmp = new byte[len];
					System.arraycopy(remains.array(), 0, tmp, 0, len);
					remains.clear();
					remains.put(DefaultHead);
					remains.put(tmp, 0, len);
					tmp = null;
				}
				else {
					remains.clear();
					remains.put(DefaultHead);
				}
				src = null;
			}
		} while (0 == headState && 0 == tailState && null != src);

		remains.flip();

		return result;
	}

	/**
	 * 重建字节数组。
	 *
	 * @param bytes
	 * @param offset
	 * @return
	 */
	private static byte[] rebuildBytes(byte[] bytes, int offset) {
		int length = bytes.length - offset;
		if (0 == length) {
			return null;
		}

		byte[] ret = new byte[length];
		System.arraycopy(bytes, offset, ret, 0, ret.length);
		return ret;
	}

	/**
	 * 比较字节数组是否相等。
	 * 
	 * @param b1 指定字节数组1。
	 * @param offsetB1 指定字节数组1操作偏移。
	 * @param b2 指定字节数组2。
	 * @param offsetB2 指定字节数组2操作偏移。
	 * @param length 指定数组比较长度。
	 * @return 返回 {@code 0} 表示匹配，{@code -1} 表示不匹配，{@code 1} 表示越界。
	 */
	private static int compareBytes(byte[] b1, int offsetB1, byte[] b2, int offsetB2, int length) {
		for (int i = 0; i < length; ++i) {
			// 判断数组越界
			if (offsetB1 + i >= b1.length || offsetB2 + i >= b2.length) {
				return 1;
			}

			if (b1[offsetB1 + i] != b2[offsetB2 + i]) {
				return -1;
			}
		}

		return 0;
	}

}
