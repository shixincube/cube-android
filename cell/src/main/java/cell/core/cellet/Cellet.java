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

import cell.api.Nucleus;
import cell.api.Servable;
import cell.core.talk.Primitive;
import cell.core.talk.ServerListener;
import cell.core.talk.TalkContext;
import cell.core.talk.TalkError;
import cell.util.log.Logger;

/**
 * Cellet 基础管理模组的基类。
 */
public abstract class Cellet implements ServerListener {

	/** Cellet 名称。每个 Nucleus 里 Cellet 名称必须唯一。 */
	private String name;

	/** 当前有效的 Nucleus 实例。 */
	protected Nucleus nucleus;

	/** Cellet 的服务核心实例。 */
	protected CelletServiceKernel celletService;

	/** 工作端口列表。 */
	protected List<Integer> workPorts;

	/**
	 * 构造函数。
	 *
	 * @param name 指定名称。
	 */
	public Cellet(String name) {
		this.name = name.toString();
		this.workPorts = new ArrayList<Integer>();
	}

	/**
	 * 获得 Cellet 的名称。
	 *
	 * @return 返回 Cellet 的名称。
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 添加工作端口信息。
	 *
	 * @param port 工作的端口。
	 */
	public void addWorkPort(Integer port) {
		if (!this.workPorts.contains(port)) {
			this.workPorts.add(port);
		}
	}

	/**
	 * 移除工作端口信息。
	 *
	 * @param port 移除的端口。
	 */
	public void removeWorkPort(Integer port) {
		this.workPorts.remove(port);
	}

	/**
	 * 获取所有的工作端口。
	 *
	 * @return 返回工作端口列表。
	 */
	public List<Integer> getWorkPorts() {
		return new ArrayList<Integer>(this.workPorts);
	}

	/**
	 * 向指定上下文的客户端发送原语数据。
	 *
	 * @param context 指定上下文。
	 * @param primitive 指定原语。
	 * @return 状态正确返回 {@code true} 。
	 */
	public boolean speak(TalkContext context, Primitive primitive) {
		return this.speakWithoutAck(context, primitive);
	}

	/**
	 * 向指定上下文的客户端发送原语数据。
	 *
	 * @param context 指定上下文。
	 * @param primitive 指定原语。
	 * @param ack 是否需要对端对该原语进行应答。
	 * @return 状态正确返回 {@code true} 。
	 */
	public boolean speak(TalkContext context, Primitive primitive, boolean ack) {
		if (ack) {
			return speakWithAck(context, primitive);
		}
		else {
			return speakWithoutAck(context, primitive);
		}
	}

	/**
	 * 向指定的终端发送原语，需要终端进行答应。
	 *
	 * @param context 指定上下文。
	 * @param primitive 指定原语。
	 * @return 状态正确返回 {@code true} 。
	 */
	public boolean speakWithAck(TalkContext context, Primitive primitive) {
		if (null == this.celletService) {
			Logger.w(this.getClass(), "Not install cellet '" + this.name + "' correctly");
			return false;
		}

		return this.celletService.speakWithAck(this, context, primitive);
	}

	/**
	 * 向指定的终端发送原语，终端不进行答应。
	 *
	 * @param context 指定上下文。
	 * @param primitive 指定原语。
	 * @return 状态正确返回 {@code true} 。
	 */
	public boolean speakWithoutAck(TalkContext context, Primitive primitive) {
		if (null == this.celletService) {
			Logger.w(this.getClass(), "Not install cellet '" + this.name + "' correctly");
			return false;
		}

		return this.celletService.speakWithoutAck(this, context, primitive);
	}

	/**
	 * 挂断指定客户端的连接。
	 *
	 * @param context 指定上下文。
	 * @param now 是否立即关闭不等待连接正常关闭。
	 */
	public void hangup(TalkContext context, boolean now) {
		if (null == this.celletService) {
			Logger.w(this.getClass(), "Not install cellet '" + this.name + "' correctly");
			return;
		}

		this.celletService.hangup(this, context, now);
	}

	/**
	 * 获得所有已经建立连接终端的上下文清单。
	 *
	 * @return 返回所有已经建立连接终端的上下文清单。
	 */
	public List<TalkContext> getAllContext() {
		return this.celletService.getAllContext(this);
	}

	/**
	 * 获取内核实例。
	 *
	 * @return 返回内核实例。
	 */
	protected Nucleus getNucleus() {
		return this.nucleus;
	}

	/**
	 * 当 {@link cell.api.CelletService} 安装 Cellet 时调用该方法。
	 *
	 * @return 只有当该方法返回 {@code true} 时，容器才能正确安装该 Cellet 。
	 */
	public abstract boolean install();

	/**
	 * 当 {@link cell.api.CelletService} 卸载 Cellet 时调用该方法。
	 */
	public abstract void uninstall();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onListened(TalkContext context, Primitive primitive) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSpoke(TalkContext context, Primitive primitive) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAck(TalkContext context, Primitive primitive) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSpeakTimeout(TalkContext context, Primitive primitive) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onContacted(TalkContext context, Servable server) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onQuitted(TalkContext context, Servable server) {
		// Nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onFailed(TalkError fault) {
		// Nothing
	}

}
