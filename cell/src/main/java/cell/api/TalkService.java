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

import java.util.List;

import cell.core.talk.HeartbeatMachine.HeartbeatContext;
import cell.core.talk.Primitive;

/**
 * 会话服务。
 */
public interface TalkService {

	/**
	 * 在指定端口启动服务器。
	 *
	 * @param port 指定服务器绑定端口。
	 * @return 返回启动的服务器实例。
	 */
	public Servable startServer(int port);

	/**
	 * 在指定地址和端口启动服务器。
	 *
	 * @param host 指定服务器绑定地址。
	 * @param port 指定服务器绑定端口。
	 * @return 返回启动的服务器实例。
	 */
	public Servable startServer(String host, int port);

	/**
	 * 停止在指定端口上的服务器。
	 *
	 * @param port 指定端口。
	 */
	public void stopServer(int port);

	/**
	 * 停止所有服务器。
	 */
	public void stopAllServers();

	/**
	 * 获取所有服务器实例。
	 *
	 * @return 返回所有服务器实例列表。
	 */
	public List<Servable> getServers();

	/**
	 * 获取指定端口上的服务器。
	 *
	 * @param port 指定端口上的服务器。
	 * @return 返回服务器实例。
	 */
	public Servable getServer(int port);

	/**
	 * 与指定地址和端口的服务器建立连接。
	 *
	 * @param host 指定服务器地址。
	 * @param port 指定服务器端口。
	 * @return 返回连接到服务器的会话器。
	 */
	public Speakable call(String host, int port);

	/**
	 * 与指定地址和端口的服务器建立连接。
	 *
	 * @param host 指定服务器地址。
	 * @param port 指定服务器端口。
	 * @param cellet 指定 Cellet 名。
	 * @return 返回连接到服务器的会话器。
	 */
	public Speakable call(String host, int port, String cellet);

	/**
	 * 与指定地址和端口的服务器建立连接。
	 *
	 * @param host 指定服务器地址。
	 * @param port 指定服务器端口。
	 * @param cellets 指定待连接的 Cellet 名称列表。
	 * @return 返回连接到服务器的会话器。
	 */
	public Speakable call(String host, int port, String[] cellets);

	/**
	 * 与指定地址和端口的服务器建立连接。
	 *
	 * @param host 指定服务器地址。
	 * @param port 指定服务器端口。
	 * @param cellets 指定待连接的 Cellet 名称列表。
	 * @return 返回连接到服务器的会话器。
	 */
	public Speakable call(String host, int port, List<String> cellets);

	/**
	 * 关闭与指定地址和端口的服务器的连接。
	 *
	 * @param host 指定服务器地址。
	 * @param port 指定服务器端口。
	 * @param now 指定是否立即断开不等待数据发送完成。
	 */
	public void hangup(String host, int port, boolean now);

	/**
	 * 向指定的 Cellet 发送原语。
	 *
	 * @param cellet 指定 Cellet 名。
	 * @param primitive 指定待发送的原语。
	 * @return 发送状态正确返回 {@code true} 。
	 */
	public boolean speak(String cellet, Primitive primitive);

	/**
	 * 向指定的 Cellet 发送原语。
	 *
	 * @param cellet 指定 Cellet 名。
	 * @param primitive 指定待发送的原语。
	 * @param ack 是否需要对端进行应答。
	 * @return 发送状态正确返回 {@code true} 。
	 */
	public boolean speak(String cellet, Primitive primitive, boolean ack);

	/**
	 * 向指定的 Cellet 发送原语，该原语会进行应答。
	 *
	 * @param cellet 指定 Cellet 名。
	 * @param primitive 指定待发送的原语。
	 * @return 发送状态正确返回 {@code true} 。
	 */
	public boolean speakWithAck(String cellet, Primitive primitive);

	/**
	 * 向指定的 Cellet 发送原语，该原语不会进行应答。
	 *
	 * @param cellet 指定 Cellet 名。
	 * @param primitive 指定待发送的原语。
	 * @return 发送状态正确返回 {@code true} 。
	 */
	public boolean speakWithoutAck(String cellet, Primitive primitive);

	/**
	 * 设置原语应答超时时间。
	 *
	 * @param timeout 应答的超时时间。
	 */
	public void setAckTimeout(long timeout);

	/**
	 * 获取原语应答超时时间。
	 *
	 * @return 返回原语应答超时时间。
	 */
	public long getAckTimeout();

	/**
	 * 设置指定 Cellet 的监听器。
	 *
	 * @param cellet 指定监听的 Cellet 的名称。
	 * @param listener 指定监听器实例。
	 */
	public void setListener(String cellet, TalkListener listener);

	/**
	 * 删除指定 Cellet 的监听器。
	 *
	 * @param cellet 指定 Cellet 的名称。
	 */
	public void removeListener(String cellet);

	/**
	 * 是否已经连接到指定的 Cellet 。
	 *
	 * @param cellet 指定 Cellet 名称。
	 * @return 如果已经连接到指定的 Cellet 返回 {@code true} 。
	 */
	public boolean isCalled(String cellet);

	/**
	 * 是否已经连接到服务器。
	 *
	 * @param host 指定服务器地址。
	 * @param port 指定服务器端口。
	 * @return 如果已经连接到服务器返回 {@code true} 。
	 */
	public boolean isCalled(String host, int port);

	/**
	 * 获取会话器对应的 Cellet 列表。
	 *
	 * @param speakable 指定会话器。
	 * @return 返回会话器连接的所有 Cellet 名称列表。
	 */
	public List<String> getCellets(Speakable speakable);

	/**
	 * 根据连接的主机地址和端口获取心跳上下文。
	 *
	 * @param host 指定主机地址。
	 * @param port 指定主机端口。
	 * @return 返回心跳上下文。
	 */
	public HeartbeatContext getHeartbeatContext(String host, int port);

	/**
	 * 获取内核的标签。
	 *
	 * @return 返回内核标签。
	 */
	public NucleusTag getTag();

}
