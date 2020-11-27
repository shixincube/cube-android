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

package cell.core.talk;

import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentLinkedQueue;

import cell.util.ByteUtils;
import cell.util.collection.FlexibleByteBuffer;

/**
 * Talk 协议。
 *
 * 数据包字段，单位 byte：<br/>
 * <pre>
 * <code>
 * +--|-00-|-01-|-02-|-03-|-04-|-05-|-06-|-07-+<br>
 * |--+---------------------------------------+<br>
 * |01| VER| SN |         RES       |    PN   |<br>
 * |--+---------------------------------------+<br>
 * |02|              DATA BEGIN               |<br>
 * |--+---------------------------------------+<br>
 * |03|                ... ...                |<br>
 * |--+---------------------------------------+<br>
 * |04|               DATA END                |<br>
 * |--+---------------------------------------+<br>
 * </code>
 * </pre>
 * DATA 段的数据使用 0x1E 作为数据分隔符。
 */
public class Protocol {

	/**
	 * 握手协议。
	 */
	public final static byte[] Handshake = new byte[] { 'H', 'S' };

	/**
	 * 无应答会话协议。
	 */
	public final static byte[] SpeakNoAck = new byte[] { 'S', 'N' };

	/**
	 * 可应答会话协议。
	 */
	public final static byte[] SpeakAck = new byte[] { 'S', 'A' };

	/**
	 * 流会话协议。
	 */
	public final static byte[] Stream = new byte[] { 'S', 'T' };

	/**
	 * 流会话控制协议。
	 */
	public final static byte[] StreamControl = new byte[] { 'S', 'C' };

	/**
	 * 会话应答协议。
	 */
	public final static byte[] Ack = new byte[] { 'A', 'C' };

	/**
	 * 心跳协议。
	 */
	public final static byte[] Heartbeat = new byte[] { 'H', 'B' };

	/**
	 * 心跳应答协议。
	 */
	public final static byte[] HeartbeatAck = new byte[] { 'H', 'A' };

	/**
	 * 结束会话协议。
	 */
	public final static byte[] Goodbye = new byte[] { 'G', 'B' };

	/**
	 * 协议默认版本。
	 */
	protected final static byte Version = 0x01;

	/**
	 * 协议默认保留字。
	 */
	protected final static byte[] Reserve = new byte[] { 0x11, 0x24, 0x11, 0x24 };

	/**
	 * 协议数据分隔符。
	 */
	protected final static byte SEPARATE_CHAR = 0x1E;

	/**
	 * 协议数据转义符。
	 */
	protected final static byte ESCAPE_CHAR = '\\';

	/**
	 * 全局 SN 记录。
	 */
	private static int sGlobalSN = 0;

	/**
	 * BUF 池。
	 */
	private final static ConcurrentLinkedQueue<FlexibleByteBuffer> sBufPool = new ConcurrentLinkedQueue<FlexibleByteBuffer>();

	private Protocol() {
	}

	/**
	 * 识别协议类型。
	 *
	 * @param data 原始报文。
	 * @return 对应的协议类型描述。
	 */
	public static byte[] recognize(byte[] data) {
		if (data.length < 10) {
			return null;
		}

		byte b6 = data[6];
		byte b7 = data[7];

		if (b6 == SpeakNoAck[0] && b7 == SpeakNoAck[1]) {
			return SpeakNoAck;
		}
		else if (b6 == Stream[0] && b7 == Stream[1]) {
			return Stream;
		}
		else if (b6 == StreamControl[0] && b7 == StreamControl[1]) {
			return StreamControl;
		}
		else if (b6 == SpeakAck[0] && b7 == SpeakAck[1]) {
			return SpeakAck;
		}
		else if (b6 == Ack[0] && b7 == Ack[1]) {
			return Ack;
		}
		else if (b6 == Heartbeat[0] && b7 == Heartbeat[1]) {
			return Heartbeat;
		}
		else if (b6 == HeartbeatAck[0] && b7 == HeartbeatAck[1]) {
			return HeartbeatAck;
		}
		else if (b6 == Handshake[0] && b7 == Handshake[1]) {
			return Handshake;
		}
		else if (b6 == Goodbye[0] && b7 == Goodbye[1]) {
			return Goodbye;
		}
		else {
			return null;
		}
	}

	/**
	 * 序列化握手协议。
	 *
	 * @param key 对端给出的密钥。
	 * @param tag 对端的标签。
	 * @return 返回保存了报文数据的缓存。
	 */
	public static FlexibleByteBuffer serializeHandshake(byte[] key, String tag) {
		FlexibleByteBuffer result = Protocol.borrowBuffer();

		// 版本 - 1
		result.put(Version);
		// 序列号 - 1
		result.put(consumeSN());
		// 保留段 - 4
		result.put(Reserve);
		// 协议名 - 2
		result.put(Handshake);

		// 访问 Key
		result.put(key);

		// 标签
		result.put(SEPARATE_CHAR);
		result.put(tag.getBytes(Charset.forName("UTF-8")));

		// 整理
		result.flip();

		return result;
	}

	/**
	 * 反序列化握手协议。
	 *
	 * @param bytes 原始数据报文。
	 * @return 握手协议实例。
	 */
	public static HandshakeProtocol deserializeHandshake(byte[] bytes) {
		HandshakeProtocol hp = new Protocol.HandshakeProtocol();
		FlexibleByteBuffer buf = Protocol.borrowBuffer();

		int index = 0;
		for (int i = 8, len = bytes.length; i < len; ++i) {
			do {
				byte b = bytes[i];
				if (b == SEPARATE_CHAR) {
					break;
				}

				buf.put(b);
				++i;
			} while (i < len);

			// 整理
			buf.flip();

			if (index == 0) {
				hp.key = new byte[buf.limit()];
				System.arraycopy(buf.array(), 0, hp.key, 0, hp.key.length);
			}
			else if (index == 1) {
				hp.tag = new String(buf.array(), 0, buf.limit());
			}

			buf.clear();
			++index;
		}

		Protocol.returnBuffer(buf);

		return hp;
	}

	/**
	 * 序列化无应答会话协议。
	 *
	 * 每段数据的格式：
	 * 数据字面义(1字节)+数据内容(变长)+数据分隔符(1字节)...(依次循环)
	 *
	 * @param target 目标名称。
	 * @param primitive 原语数据。
	 * @return 返回保存了报文数据的缓存。
	 */
	public static FlexibleByteBuffer serializeSpeakNoAck(String target, Primitive primitive) {
		FlexibleByteBuffer result = Protocol.borrowBuffer();

		// 版本 - 1
		result.put(Version);
		// 序列号 - 1
		result.put(consumeSN());
		// 保留段 - 4，使用 SN 填充
		result.put(primitive.getSN());
		// 协议名 - 2
		result.put(SpeakNoAck);

		// 目标
		result.put(target.getBytes());
		result.put(SEPARATE_CHAR);

		// 原语数据
		for (Stuff stuff : primitive.stuffList) {
			// 字面义
			result.put(stuff.literal.code);

			// 数据
			for (byte b : stuff.value) {
				if (b == SEPARATE_CHAR) {
					// 转义
					result.put(ESCAPE_CHAR);
				}
				else if (b == ESCAPE_CHAR) {
					// 转义
					result.put(ESCAPE_CHAR);
				}
				result.put(b);
			}

			// 分隔数据
			result.put(SEPARATE_CHAR);
		}

		// 整理
		result.flip();

		// 删除数据尾的分隔符
		result.limit(result.limit() - 1);

		return result;
	}

	/**
	 * 序列化可应答会话协议。
	 *
	 * 每段数据的格式：
	 * 数据字面义(1字节)+数据内容(变长)+数据分隔符(1字节)...(依次循环)
	 *
	 * @param target 目标名称。
	 * @param primitive 原语数据。
	 * @return 返回保存了报文数据的缓存。
	 */
	public static FlexibleByteBuffer serializeSpeakAck(String target, Primitive primitive) {
		FlexibleByteBuffer result = Protocol.borrowBuffer();

		// 版本 - 1
		result.put(Version);
		// 序列号 - 1
		result.put(consumeSN());
		// 保留段 - 4，使用 SN 填充
		result.put(primitive.getSN());
		// 协议名 - 2
		result.put(SpeakAck);

		// 目标
		result.put(target.getBytes());
		result.put(SEPARATE_CHAR);

		// 原语数据
		for (Stuff stuff : primitive.stuffList) {
			// 字面义
			result.put(stuff.literal.code);

			// 数据
			for (byte b : stuff.value) {
				if (b == SEPARATE_CHAR) {
					// 转义
					result.put(ESCAPE_CHAR);
				}
				else if (b == ESCAPE_CHAR) {
					// 转义
					result.put(ESCAPE_CHAR);
				}
				result.put(b);
			}

			// 分隔数据
			result.put(SEPARATE_CHAR);
		}

		// 整理
		result.flip();

		// 删除数据尾的分隔符
		result.limit(result.limit() - 1);

		return result;
	}

	/**
	 * 反序列化会话协议。
	 *
	 * @param bytes 数据报文。
	 * @return 返回会话协议实例。
	 */
	public static SpeakProtocol deserializeSpeak(byte[] bytes) {
		if (bytes.length < 10) {
			return null;
		}

		// 提取 SN
		byte[] sn = new byte[4];
		System.arraycopy(bytes, 2, sn, 0, 4);

		SpeakProtocol protocol = new SpeakProtocol();

		FlexibleByteBuffer buf = Protocol.borrowBuffer();

		// 目标
		for (int i = 8; i < bytes.length; ++i) {
			byte b = bytes[i];

			if (b == SEPARATE_CHAR) {
				break;
			}

			buf.put(b);
		}

		// 整理
		buf.flip();

		// 赋值
		protocol.target = new String(buf.array(), 0, buf.limit());

		// 调整游标位置
		int start = 8 + buf.limit() + 1;

		// 清空
		buf.clear();

		Primitive primitive = new Primitive(sn);

		for (int i = start, len = bytes.length; i < len; ++i) {
			// 字面义
			byte lb = bytes[i];

			// 定位到数据内容位置
			++i;

			// 数据内容提取
			while (i < len) {
				byte dc = bytes[i];

				if (dc == ESCAPE_CHAR) {
					++i;
					dc = bytes[i];
					buf.put(dc);
					// 下一字节
					++i;
					continue;
				}
				else if (dc == SEPARATE_CHAR) {
					// 结束
					break;
				}

				buf.put(dc);
				// 下一字节
				++i;
			}

			// 整理
			buf.flip();

			Stuff stuff = new Stuff(lb, buf.array(), 0, buf.limit());
			primitive.commit(stuff);

			// 清理
			buf.clear();
		}

		// 赋值
		protocol.primitive = primitive;

		Protocol.returnBuffer(buf);

		return protocol;
	}

	/**
	 * 序列化应答协议。
	 *
	 * @param target 目标名称。
	 * @param sn 应答序号。
	 * @return 返回保存了报文数据的缓存。
	 */
	public static FlexibleByteBuffer serializeAck(String target, byte[] sn) {
		FlexibleByteBuffer result = Protocol.borrowBuffer();

		// 版本 - 1
		result.put(Version);
		// 序列号 - 1
		result.put(consumeSN());
		// 保留段 - 4，使用保留段存储 SN
		result.put(sn);
		// 协议名 - 2
		result.put(Ack);

		// 目标
		result.put(target.getBytes(Charset.forName("UTF-8")));

		// 整理
		result.flip();

		return result;
	}

	/**
	 * 反序列化会话应答协议。
	 *
	 * @param bytes 数据报文。
	 * @return 返回应答协议实例。
	 */
	public static AckProtocol deserializeAck(byte[] bytes) {
		AckProtocol protocol = new Protocol.AckProtocol();

		// 提取 SN
		protocol.sn = new byte[4];
		System.arraycopy(bytes, 2, protocol.sn, 0, 4);

		// Target
		byte[] target = new byte[bytes.length - 8];
		System.arraycopy(bytes, 8, target, 0, target.length);
		protocol.target = new String(target, Charset.forName("UTF-8"));

		return protocol;
	}

	/**
	 * 序列化心跳协议。
	 *
	 * @param tag 自身的标签。
	 * @param timestamp 时间戳。
	 * @return 返回保存了报文数据的缓存。
	 */
	public static FlexibleByteBuffer serializeHeartbeat(String tag, long timestamp) {
		FlexibleByteBuffer result = Protocol.borrowBuffer();

		// 版本 - 1
		result.put(Version);
		// 序列号 - 1
		result.put(consumeSN());
		// 保留段 - 4
		result.put(Reserve);
		// 协议名 - 2
		result.put(Heartbeat);

		// 标签
		result.put(tag.getBytes(Charset.forName("UTF-8")));

		// 时间戳
		result.put(SEPARATE_CHAR);
		result.put(escape(ByteUtils.toBytes(timestamp)));

		// 整理
		result.flip();

		return result;
	}

	/**
	 * 反序列化心跳协议。
	 *
	 * @param bytes 数据报文。
	 * @return 返回心跳协议实例。
	 */
	public static HeartbeatProtocol deserializeHeartbeat(byte[] bytes) {
		HeartbeatProtocol hp = new Protocol.HeartbeatProtocol();
		FlexibleByteBuffer buf = Protocol.borrowBuffer();

		int index = 0;
		for (int i = 8, len = bytes.length; i < len; ++i) {
			do {
				byte b = bytes[i];
				if (b == SEPARATE_CHAR) {
					break;
				}
				else if (b == ESCAPE_CHAR) {
					// 解析转义
					i += 1;
					b = bytes[i];
				}

				buf.put(b);
				++i;
			} while (i < len);

			// 整理
			buf.flip();

			if (index == 0) {
				hp.tag = new String(buf.array(), 0, buf.limit());
			}
			else if (index == 1) {
				byte[] timestamp = new byte[8];
				System.arraycopy(buf.array(), 0, timestamp, 0, 8);
				hp.originateTimestamp = ByteUtils.toLong(timestamp);
			}

			buf.clear();
			++index;
		}

		Protocol.returnBuffer(buf);

		return hp;
	}

	/**
	 * 序列化心跳应答协议。
	 *
	 * @param tag 给予应答的标签。
	 * @param originateTimestamp 原心跳协议的原时间戳。
	 * @param receiveTimestamp 本端接收数据的时间戳。
	 * @param transmitTimestamp 本端发送应答的时间戳。
	 * @return 返回保存了报文数据的缓存。
	 */
	public static FlexibleByteBuffer serializeHeartbeatAck(String tag, long originateTimestamp,
														   long receiveTimestamp, long transmitTimestamp) {
		FlexibleByteBuffer result = Protocol.borrowBuffer();

		// 版本 - 1
		result.put(Version);
		// 序列号 - 1
		result.put(consumeSN());
		// 保留段 - 4
		result.put(Reserve);
		// 协议名 - 2
		result.put(HeartbeatAck);

		// 标签
		result.put(tag.getBytes(Charset.forName("UTF-8")));

		// 时间戳
		result.put(SEPARATE_CHAR);
		result.put(escape(ByteUtils.toBytes(originateTimestamp)));
		result.put(SEPARATE_CHAR);
		result.put(escape(ByteUtils.toBytes(receiveTimestamp)));
		result.put(SEPARATE_CHAR);
		result.put(escape(ByteUtils.toBytes(transmitTimestamp)));

		// 整理
		result.flip();

		return result;
	}

	/**
	 * 反序列化心跳应答协议。
	 *
	 * @param bytes 数据报文。
	 * @return 返回心跳协议实例。
	 */
	public static HeartbeatProtocol deserializeHeartbeatAck(byte[] bytes) {
		HeartbeatProtocol hp = new Protocol.HeartbeatProtocol();
		FlexibleByteBuffer buf = Protocol.borrowBuffer();

		int index = 0;
		for (int i = 8, len = bytes.length; i < len; ++i) {
			do {
				byte b = bytes[i];
				if (b == SEPARATE_CHAR) {
					break;
				}
				else if (b == ESCAPE_CHAR) {
					// 解析转义
					i += 1;
					b = bytes[i];
				}

				buf.put(b);
				++i;
			} while (i < len);

			// 整理
			buf.flip();

			if (index == 0) {
				hp.tag = new String(buf.array(), 0, buf.limit());
			}
			else if (index == 1) {
				byte[] timestamp = new byte[8];
				System.arraycopy(buf.array(), 0, timestamp, 0, 8);
				hp.originateTimestamp = ByteUtils.toLong(timestamp);
			}
			else if (index == 2) {
				byte[] timestamp = new byte[8];
				System.arraycopy(buf.array(), 0, timestamp, 0, 8);
				hp.receiveTimestamp = ByteUtils.toLong(timestamp);
			}
			else if (index == 3) {
				byte[] timestamp = new byte[8];
				System.arraycopy(buf.array(), 0, timestamp, 0, 8);
				hp.transmitTimestamp = ByteUtils.toLong(timestamp);
			}

			buf.clear();
			++index;
		}

		Protocol.returnBuffer(buf);

		return hp;
	}

	/**
	 * 序列化 Goodbye 协议。
	 *
	 * @return 保存了数据报文的缓存。
	 */
	public static FlexibleByteBuffer serializeGoodbye() {
		FlexibleByteBuffer result = Protocol.borrowBuffer();

		// 版本 - 1
		result.put(Version);
		// 序列号 - 1
		result.put(consumeSN());
		// 保留段 - 4
		result.put(Reserve);
		// 协议名 - 2
		result.put(Goodbye);

		return result;
	}

	/**
	 * 序列化流协议。
	 *
	 * @param target 目标名称。
	 * @param primitive 原语。
	 * @return 保存了数据报文的缓存。
	 */
	public static FlexibleByteBuffer serializeStream(String target, Primitive primitive) {
		FlexibleByteBuffer result = Protocol.borrowBuffer();

		// 版本 - 1
		result.put(Version);
		// 序列号 - 1
		result.put(consumeSN());
		// 保留段 - 4，使用 SN 填充
		result.put(primitive.getSN());
		// 协议名 - 2
		result.put(Stream);

		// 目标
		result.put(target.getBytes());
		result.put(SEPARATE_CHAR);

		// 原语数据
		for (Stuff stuff : primitive.stuffList) {
			// 字面义
			result.put(stuff.literal.code);

			// 数据
			for (byte b : stuff.value) {
				if (b == SEPARATE_CHAR) {
					// 转义
					result.put(ESCAPE_CHAR);
				}
				else if (b == ESCAPE_CHAR) {
					// 转义
					result.put(ESCAPE_CHAR);
				}
				result.put(b);
			}

			// 分隔数据
			result.put(SEPARATE_CHAR);
		}

		// 整理
		result.flip();

		// 删除数据尾的分隔符
		result.limit(result.limit() - 1);

		return result;
	}

	/**
	 * 反序列化流协议。
	 *
	 * @param bytes 数据报文。
	 * @return 返回流协议。
	 */
	public static StreamProtocol deserializeStream(byte[] bytes) {
		if (bytes.length < 10) {
			return null;
		}

		// 提取 SN
		byte[] sn = new byte[4];
		System.arraycopy(bytes, 2, sn, 0, 4);

		StreamProtocol protocol = new StreamProtocol();

		FlexibleByteBuffer buf = Protocol.borrowBuffer();

		// 目标
		for (int i = 8; i < bytes.length; ++i) {
			byte b = bytes[i];

			if (b == SEPARATE_CHAR) {
				break;
			}

			buf.put(b);
		}

		// 整理
		buf.flip();

		// 赋值
		protocol.target = new String(buf.array(), 0, buf.limit());

		// 调整游标位置
		int start = 8 + buf.limit() + 1;

		// 清空
		buf.clear();

		Primitive primitive = new Primitive(sn);

		for (int i = start, len = bytes.length; i < len; ++i) {
			// 字面义
			byte lb = bytes[i];

			// 定位到数据内容位置
			++i;

			// 数据内容提取
			while (i < len) {
				byte dc = bytes[i];

				if (dc == ESCAPE_CHAR) {
					++i;
					dc = bytes[i];
					buf.put(dc);
					// 下一字节
					++i;
					continue;
				}
				else if (dc == SEPARATE_CHAR) {
					// 结束
					break;
				}

				buf.put(dc);
				// 下一字节
				++i;
			}

			// 整理
			buf.flip();

			Stuff stuff = new Stuff(lb, buf.array(), 0, buf.limit());
			primitive.commit(stuff);

			// 清理
			buf.clear();
		}

		// 赋值
		protocol.primitive = primitive;

		Protocol.returnBuffer(buf);

		return protocol;
	}

	/**
	 * 回收报文缓存。
	 *
	 * @param buf 被回收的报文缓存。
	 */
	public static void recovery(FlexibleByteBuffer buf) {
		Protocol.returnBuffer(buf);
	}

	/**
	 * 产生并消费掉一个可用的包序号。
	 *
	 * @return 返回可用的包序号。
	 */
	private static synchronized byte consumeSN() {
		byte sn = (byte) sGlobalSN;
		sGlobalSN += 1;
		if (sGlobalSN > 127) {
			sGlobalSN = 0;
		}
		return sn;
	}

	/**
	 * 字节信息转义。
	 *
	 * @param input
	 * @return
	 */
	private static byte[] escape(byte[] input) {
		int size = 0;
		for (int i = 0; i < input.length; ++i) {
			++size;
			if (input[i] == SEPARATE_CHAR || input[i] == ESCAPE_CHAR) {
				// 需要转义
				++size;
			}
		}

		// 长度相同，没有转义
		if (size == input.length) {
			return input;
		}

		byte[] output = new byte[size];
		for (int i = 0, o = 0; i < input.length; ++i, ++o) {
			byte b = input[i];
			if (b == SEPARATE_CHAR) {
				output[o] = ESCAPE_CHAR;
				++o;
			}
			else if (b == ESCAPE_CHAR) {
				output[o] = ESCAPE_CHAR;
				++o;
			}
			output[o] = b;
		}

		return output;
	}

//	private static byte[] unescape(byte[] input) {
//		int size = 0;
//		for (int i = 0; i < input.length; ++i) {
//			++size;
//
//			if (input[i] == ESCAPE_CHAR) {
//				++i;	// 跳过被转义字符
//			}
//		}
//
//		if (size == input.length) {
//			return input;
//		}
//
//		byte[] output = new byte[size];
//		for (int i = 0, o = 0; i < input.length; ++i, ++o) {
//			byte b = input[i];
//
//			if (b == ESCAPE_CHAR) {
//				// 转义字符，取下一个
//				++i;
//
//				if (i >= input.length) {
//					// 数据格式错误
//					break;
//				}
//
//				b = input[i];
//			}
//
//			output[o] = b;
//		}
//
//		return output;
//	}

	/**
	 * 借出字节缓存对象。
	 *
	 * @return 返回字节缓存对象。
	 */
	private static FlexibleByteBuffer borrowBuffer() {
		FlexibleByteBuffer buf = sBufPool.poll();
		if (null == buf) {
			return new FlexibleByteBuffer(1024);
		}

		return buf;
	}

	/**
	 * 归还字节缓存对象。
	 *
	 * @param buffer 字节缓存。
	 */
	private static void returnBuffer(FlexibleByteBuffer buffer) {
		buffer.clear();
		sBufPool.offer(buffer);
	}

	/**
	 * 握手协议。
	 */
	public static class HandshakeProtocol {
		public byte[] key;
		public String tag;

		public void destroy() {
			this.key = null;
			this.tag = null;
		}
	}

	/**
	 * 会话协议。
	 */
	public static class SpeakProtocol {
		public String target;
		public Primitive primitive;

		// 是否需要对端进行应答。
		public boolean ack = false;

		public void destroy() {
			this.target = null;
			this.primitive = null;
		}
	}

	/**
	 * 会话应答协议。
	 */
	public static class AckProtocol {
		public String target;
		public byte[] sn;

		public void destroy() {
			this.target = null;
			this.sn = null;
		}
	}

	/**
	 * 心跳协议。
	 */
	public static class HeartbeatProtocol {
		public String tag;
		public long originateTimestamp = 0;
		public long receiveTimestamp = 0;
		public long transmitTimestamp = 0;

		public void destroy() {
			this.tag = null;
		}
	}

	/**
	 * 流协议。
	 */
	public static class StreamProtocol {
		public String target;
		public Primitive primitive;

		public void destroy() {
			this.target = null;
			this.primitive = null;
		}
	}
}
