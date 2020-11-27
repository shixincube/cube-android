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

package cell.core.cellet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import cell.api.CelletService;
import cell.api.Nucleus;
import cell.core.talk.BaseServer;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.TalkServiceKernel;
import cell.util.log.Logger;

/**
 * Cellet 模组服务核心。
 */
public class CelletServiceKernel implements CelletService {

	/** 内核。 */
	private Nucleus nucleus;

	/** 存储已安装的 Cellet 。 */
	private ConcurrentHashMap<String, Cellet> cellets;

	/** Cellet 对应的绑定服务器。 */
	private ConcurrentHashMap<String, ArrayList<BaseServer>> celletServersMap;

	/**
	 * 构造函数。
	 * 
	 * @param nucleus 内核。
	 */
	public CelletServiceKernel(Nucleus nucleus) {
		this.nucleus = nucleus;
		this.cellets = new ConcurrentHashMap<String, Cellet>();
		this.celletServersMap = new ConcurrentHashMap<String, ArrayList<BaseServer>>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean installCellet(Cellet cellet) {
		// 安装
		cellet.nucleus = this.nucleus;

		if (cellet.install()) {
			this.cellets.put(cellet.getName(), cellet);
			return true;
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void uninstallCellet(Cellet cellet) {
		if (this.cellets.containsKey(cellet.getName())) {

			// 卸载
			cellet.uninstall();

			this.cellets.remove(cellet.getName());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cellet getCellet(String name) {
		return this.cellets.get(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean activateCellet(int port, Cellet cellet) {
		BaseServer server = ((TalkServiceKernel) this.nucleus.getTalkService()).addListener(port, cellet.getName(), cellet);
		if (null == server) {
			Logger.w(this.getClass(), "Activate cellet '" + cellet.getName() + "' failed at port " + port);
			return false;
		}

		ArrayList<BaseServer> list = this.celletServersMap.get(cellet.getName());
		if (null == list) {
			list = new ArrayList<BaseServer>(2);
			this.celletServersMap.put(cellet.getName(), list);
		}

		// 添加
		list.add(server);
		cellet.celletService = this;
		cellet.addWorkPort(port);

		Logger.i(this.getClass(), "Activate cellet '" + cellet.getName() + "' at " + port);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean deactivateCellet(int port, Cellet cellet) {
		BaseServer server = ((TalkServiceKernel) this.nucleus.getTalkService()).removeListener(port, cellet.getName());
		if (null == server) {
			Logger.w(this.getClass(), "Deactivate cellet '" + cellet.getName() + "' failed at port " + port);
			return false;
		}

		ArrayList<BaseServer> list = this.celletServersMap.get(cellet.getName());
		if (null == list) {
			return false;
		}

		// 删除
		list.remove(server);

		if (list.isEmpty()) {
			this.celletServersMap.remove(cellet.getName());
			cellet.celletService = null;
		}

		cellet.removeWorkPort(port);

		Logger.i(this.getClass(), "Deactivate cellet '" + cellet.getName() + "' at " + port);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Cellet> getCellets() {
		ArrayList<Cellet> result = new ArrayList<Cellet>(this.cellets.size());
		result.addAll(this.cellets.values());
		return result;
	}

	/**
	 * 向指定的客户端发送原语数据。
	 *
	 * @param cellet Cellet 的实例。
	 * @param context 客户端的会话上下文。
	 * @param primitive 待发送的原语。
	 * @param ack 是否需要终端应答。
	 * @return 状态正确返回 {@code true} 。
	 */
	protected boolean speak(Cellet cellet, TalkContext context, Primitive primitive, boolean ack) {
		if (ack) {
			return speakWithAck(cellet, context, primitive);
		}
		else {
			return speakWithoutAck(cellet, context, primitive);
		}
	}

	/**
	 * 向指定的客户端发送原语数据，客户端会进行应答。
	 *
	 * @param cellet Cellet 的实例。
	 * @param context 客户端的会话上下文。
	 * @param primitive 待发送的原语。
	 * @return 状态正确返回 {@code true} 。
	 */
	protected boolean speakWithAck(Cellet cellet, TalkContext context, Primitive primitive) {
		BaseServer server = context.getServer();
		if (null == server) {
			Logger.w(this.getClass(), "Not find server for cellet '" + cellet.getName() + "'");
			return false;
		}

		if (context.isLock()) {
			if (Logger.isDebugLevel()) {
				Logger.d(this.getClass(), "Session is locked");
			}
			return false;
		}

		return server.speakWithAck(context.getSession(), cellet.getName(), primitive);
	}

	/**
	 * 向指定的客户端发送原语数据，客户端不进行应答。
	 *
	 * @param cellet Cellet 的实例。
	 * @param context 客户端的会话上下文。
	 * @param primitive 待发送的原语。
	 * @return 状态正确返回 {@code true} 。
	 */
	protected boolean speakWithoutAck(Cellet cellet, TalkContext context, Primitive primitive){
		BaseServer server = context.getServer();
		if (null == server) {
			Logger.w(this.getClass(), "Not find server for cellet '" + cellet.getName() + "'");
			return false;
		}

		if (context.isLock()) {
			if (Logger.isDebugLevel()) {
				Logger.d(this.getClass(), "Session is locked");
			}
			return false;
		}

		return server.speakWithoutAck(context.getSession(), cellet.getName(), primitive);
	}

	/**
	 * 关闭与指定客户端的连接。
	 *
	 * @param cellet Cellet 实例。
	 * @param context 客户端的会话上下文。
	 * @param now 是否立即关闭。
	 */
	protected void hangup(Cellet cellet, TalkContext context, boolean now) {
		BaseServer server = context.getServer();
		if (null == server) {
			Logger.w(this.getClass(), "Not find server for cellet '" + cellet.getName() + "'");
			return;
		}

		// 设置锁
		context.setLock(true);

		// 关闭会话
		server.hangup(context.getSession(), cellet.getName(), now);
	}

	/**
	 * 返回所有上下文。
	 *
	 * @param cellet 指定 Cellet 实例。
	 * @return Cellet 上的所有客户端上下文。
	 */
	public List<TalkContext> getAllContext(Cellet cellet) {
		ArrayList<BaseServer> list = this.celletServersMap.get(cellet.getName());
		if (null == list) {
			Logger.w(this.getClass(), "Not find server for cellet '" + cellet.getName() + "'");
			return null;
		}

		ArrayList<TalkContext> ret = new ArrayList<TalkContext>();

		for (BaseServer server : list) {
			ret.addAll(server.getAllContext());
		}

		return ret;
	}

}
