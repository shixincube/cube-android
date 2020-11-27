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

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * 消息接收器接口。
 */
public interface MessageAcceptor {

	/**
	 * 绑定消息接收服务到指定端口。
	 *
	 * @param port 指定绑定端口。
	 * @return 如果绑定成功返回 {@code true} 。
	 */
	public boolean bind(int port);

	/**
	 * 绑定消息接收服务到指定端口。
	 *
	 * @param port 指定绑定端口。
	 * @param ssl 指定 SSL 配置信息。
	 * @return 如果绑定成功返回 {@code true} 。
	 */
	public boolean bind(int port, SSLSecurity ssl);

	/**
	 * 绑定消息接收服务到指定地址。
	 *
	 * @param address 指定绑定端口。
	 * @return 如果绑定成功返回 {@code true} 。
	 */
	public boolean bind(InetSocketAddress address);

	/**
	 * 绑定消息接收服务到指定地址。
	 *
	 * @param address 指定绑定端口。
	 * @param sslSecure 指定 SSL 配置信息。
	 * @return 如果绑定成功返回 {@code true} 。
	 */
	public boolean bindSSL(InetSocketAddress address, SSLSecurity sslSecure);

	/**
	 * 解绑当前绑定的服务地址。
	 */
	public void unbind();

	/**
	 * 向指定会话发送消息数据。
	 *
	 * @param session 指定发送消息的会话。
	 * @param message 指定待发送的消息。
	 * @throws IOException
	 */
	public void write(Session session, Message message) throws IOException;

	/**
	 * 关闭指定会话。指定会话连接将被断开。
	 *
	 * @param session 指定需关闭的会话。
	 */
	public void close(Session session);

	/**
	 * 获得指定 ID 的会话实例。
	 *
	 * @param sessionId 指定会话 ID 。
	 * @return 返回指定 ID 的会话实例。
	 */
	public Session getSession(Long sessionId);

	/**
	 * 设置接收器能接收的最大连接数量。
	 *
	 * @param num 指定连接数量。
	 */
	public void setMaxConnectNum(int num);

	/**
	 * 获得接收器能接收的最大连接数量。
	 *
	 * @return 返回接收器能接收的最大连接数量。
	 */
	public int getMaxConnectNum();

}
