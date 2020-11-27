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

import java.net.InetSocketAddress;

import cell.util.Utils;

/**
 * 消息会话描述。
 */
public class Session {

	/** 会话的 ID 。 */
	protected Long id;

	/** 会话创建时的时间戳。 */
	protected long timestamp;

	/** 会话对应的终端地址。 */
	private InetSocketAddress address;

	/** 会话的密钥。 */
	protected byte[] secretKey = null;

	/**
	 * 构造函数。
	 *
	 * @param address 指定对端的连接地址。
	 */
	public Session(InetSocketAddress address) {
		this.id = Utils.generateUnsignedSerialNumber();
		this.timestamp = System.currentTimeMillis();
		this.address = address;
	}

	/**
	 * 返回会话 ID 。
	 *
	 * @return 返回会话 ID 。
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * 返回 Socket 地址信息。
	 *
	 * @return 返回 Socket 地址信息。
	 */
	public InetSocketAddress getAddress() {
		return this.address;
	}

	/**
	 * 设置会话的密钥。
	 *
	 * @param key 指定存储密钥的字节数组。
	 */
	public void setSecretKey(byte[] key) {
		this.secretKey = key;
	}

	/**
	 * 返回会话的密钥。
	 *
	 * @return 返回存储密钥的字节数组。
	 */
	public byte[] getSecretKey() {
		return this.secretKey;
	}

	/**
	 * 返回会话的地址信息。
	 *
	 * @return 返回会话的地址信息。
	 */
	@Override
	public String toString() {
		return this.address.getHostString() + ":" + this.address.getPort();
	}

}
