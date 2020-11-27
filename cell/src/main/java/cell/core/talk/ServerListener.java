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

import cell.api.Servable;

/**
 * 服务器监听器。
 */
public interface ServerListener {

	/**
	 * 当客户端和服务器完成握手后该方法被调用。
	 *
	 * @param context 会话上下文。
	 * @param server 服务器实例。
	 */
	public void onContacted(TalkContext context, Servable server);

	/**
	 * 当客户端与服务器断开连接时该方法被调用。
	 *
	 * @param context 会话上下文。
	 * @param server 服务器实例。
	 */
	public void onQuitted(TalkContext context, Servable server);

	/**
	 * 当收到客户端发送的原语数据时该方法被调用。
	 *
	 * @param context 会话上下文。
	 * @param primitive 收到的原语。
	 */
	public void onListened(TalkContext context, Primitive primitive);

	/**
	 * 当数据发送给客户端时该方法被调用。
	 *
	 * @param context 会话上下文。
	 * @param primitive 已经发送的原语。
	 */
	public void onSpoke(TalkContext context, Primitive primitive);

	/**
	 * 当客户端对服务器发送的原语进行应答时该方法被调用。
	 *
	 * @param context 会话上下文。
	 * @param primitive 应答的原语。
	 */
	public void onAck(TalkContext context, Primitive primitive);

	/**
	 * 当发送的原语没有在指定时间内收到应答时该方法被调用。
	 *
	 * @param context 会话上下文。
	 * @param primitive 超时未发送成功的原语。
	 */
	public void onSpeakTimeout(TalkContext context, Primitive primitive);

	/**
	 * 当发生程序错误时该方法被调用。
	 *
	 * @param error 发生的错误描述。
	 */
	public void onFailed(TalkError error);

}
