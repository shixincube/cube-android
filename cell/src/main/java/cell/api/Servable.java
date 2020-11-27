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

import cell.core.talk.TalkContext;

/**
 * 服务器功能接口。
 */
public interface Servable {

	/**
	 * 获取服务器的标签。
	 *
	 * @return 返回字符串形式的服务器标签。
	 */
	public String getTag();

	/**
	 * 获取服务器绑定的主机地址。
	 *
	 * @return 返回服务器绑定的主机地址。
	 */
	public String getHost();

	/**
	 * 获取服务器绑定的主机端口。
	 *
	 * @return 返回服务器绑定的主机端口。
	 */
	public int getPort();

	/**
	 * 获取指定会话 ID 的上下文。
	 *
	 * @param sessionId 指定会话上下文 ID 。
	 * @return 返回指定会话 ID 的上下文。
	 */
	public TalkContext getTalkContext(Long sessionId);

	/**
	 * 设置允许的最大连接数。
	 *
	 * @param num 指定最大连接数。
	 */
	public void setMaxConnections(int num);

	/**
	 * 获取允许的最大连接数。
	 *
	 * @return 返回允许的最大连接数。
	 */
	public int getMaxConnections();
}
