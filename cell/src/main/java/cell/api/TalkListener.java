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

import cell.core.talk.Primitive;
import cell.core.talk.TalkError;

/**
 * 会话监听器。
 */
public interface TalkListener {

	/**
	 * 当收到对端发送过来的原语时该方法被调用。
	 * 
	 * @param speaker 接收到数据的会话者。
	 * @param cellet 发送原语的 Cellet 名。
	 * @param primitive 接收到的原语。
	 */
	public void onListened(Speakable speaker, String cellet, Primitive primitive);

	/**
	 * 当原语发送出去后该方法被调用。
	 * 
	 * @param speaker 发送数据的会话者。
	 * @param cellet 接收原语的 Cellet 名。
	 * @param primitive 发送的原语。
	 */
	public void onSpoke(Speakable speaker, String cellet, Primitive primitive);

	/**
	 * 当收到服务器回送的原语应答时该方法被调用。
	 * 
	 * @param speaker 发送数据的会话者。
	 * @param cellet 接收原语的 Cellet 名。
	 * @param primitive 发送的原语。
	 */
	public void onAck(Speakable speaker, String cellet, Primitive primitive);

	/**
	 * 当发送的原语在指定时间内没有应答时该方法被调用。
	 * 
	 * @param speaker 发送数据的会话者。
	 * @param cellet 接收原语的 Cellet 名。
	 * @param primitive 发送的原语。
	 */
	public void onSpeakTimeout(Speakable speaker, String cellet, Primitive primitive);

	/**
	 * 当会话者连接到服务器完成握手后该方法被调用。
	 *
	 * @param cellet
	 * @param speaker 发送数据的会话者。
	 */
	public void onContacted(String cellet, Speakable speaker);

	/**
	 * 当会话者断开与服务器的连接时该方法被调用。
	 * 
	 * @param speaker 发送数据的会话者。
	 */
	public void onQuitted(Speakable speaker);

	/**
	 * 当发生故障时该方法被调用。
	 * 
	 * @param speaker 发送数据的会话者。
	 * @param error 故障描述。
	 */
	public void onFailed(Speakable speaker, TalkError error);

}
