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

package cell.api;

import java.net.InetSocketAddress;

import cell.core.talk.Primitive;

/**
 * 会话服务会话器接口。
 */
public interface Speakable {

	/**
	 * 获取远程连接地址。
	 *
	 * @return 返回远程连接地址。
	 */
	public InetSocketAddress getRemoteAddress();

	/**
	 * 向指定 Cellet 发送原语数据。
	 *
	 * @param cellet 指定 Cellet 名称。
	 * @param primitive 指定待发送的原语。
	 * @return 状态正确返回 {@code true} 。
	 */
	public boolean speak(String cellet, Primitive primitive);

	/**
	 * 向指定 Cellet 发送原语数据。
	 *
	 * @param cellet 指定 Cellet 名称。
	 * @param primitive 指定待发送的原语。
	 * @param ack 是否需要对端进行应答。
	 * @return 状态正确返回 {@code true} 。
	 */
	public boolean speak(String cellet, Primitive primitive, boolean ack);

	/**
	 * 向指定 Cellet 发送需要应答的原语数据。
	 *
	 * @param cellet 指定 Cellet 名称。
	 * @param primitive 指定待发送的原语。
	 * @return 状态正确返回 {@code true} 。
	 */
	public boolean speakWithAck(String cellet, Primitive primitive);

	/**
	 * 向指定 Cellet 发送不需要应答的原语数据。
	 *
	 * @param cellet 指定 Cellet 名称。
	 * @param primitive 指定待发送的原语。
	 * @return 状态正确返回 {@code true} 。
	 */
	public boolean speakWithoutAck(String cellet, Primitive primitive);
}
