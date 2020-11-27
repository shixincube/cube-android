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

import cell.core.net.MessageErrorCode;

/**
 * 故障描述。
 */
public class TalkError {

	/** 网络一般错误。 */
	public final static int NetworkError = 1000;

	/** 网络链路错误。 */
	public final static int NetworkLinkError = 1010;

	/** 网络 Socket 错误。 */
	public final static int NetworkSocketError = 1020;

	/** 网络 I/O 错误。 */
	public final static int NetworkIOError = 1030;

	/** 心跳超时。 */
	public final static int HeartbeatTimeout = 1040;

	/** 握手错误。 */
	public final static int HandshakeError = 2000;

	/** 握手超时。 */
	public final static int HandshakeTimeout = 2001;


	/**
	 * 错误码。
	 */
	private int errorCode = 0;

	private TalkContext talkContext;

	/**
	 * 构造函数。
	 *
	 * @param error 错误码。
	 */
	public TalkError(int error) {
		this.errorCode = error;
	}

	/**
	 * 获取错误码。
	 *
	 * @return 返回错误码。
	 */
	public int getErrorCode() {
		return this.errorCode;
	}

	public void setTalkContext(TalkContext context) {
		this.talkContext = context;
	}

	/**
	 * 获取会话上下文。
	 *
	 * @return 返回会话上下文。
	 */
	public TalkContext getTalkContext() {
		return this.talkContext;
	}

	/**
	 * 将消息层错误代码转为错误描述。
	 *
	 * @param messageError 消息错误码。
	 * @return 返回错误描述
	 */
	public static TalkError transformError(int messageError) {
		TalkError error = new TalkError(NetworkError);
		switch (messageError) {
		case MessageErrorCode.CONNECT_FAILED:
		case MessageErrorCode.CONNECT_TIMEOUT:
			error.errorCode = NetworkLinkError;
			break;
		case MessageErrorCode.SOCKET_FAILED:
		case MessageErrorCode.BIND_FAILED:
		case MessageErrorCode.LISTEN_FAILED:
		case MessageErrorCode.ACCEPT_FAILED:
			error.errorCode = NetworkSocketError;
			break;
		case MessageErrorCode.WRITE_TIMEOUT:
		case MessageErrorCode.READ_TIMEOUT:
		case MessageErrorCode.WRITE_FAILED:
		case MessageErrorCode.READ_FAILED:
		case MessageErrorCode.WRITE_OUTOFBOUNDS:
			error.errorCode = NetworkIOError;
			break;
		default:
			break;
		}
		return error;
	}

}
