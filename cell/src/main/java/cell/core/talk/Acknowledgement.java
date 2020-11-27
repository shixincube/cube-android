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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import cell.core.net.Session;
import cell.util.Clock;

/**
 * 应答数据追踪管理定时任务。
 */
public final class Acknowledgement {

	/** 应答超时时间。 */
	private long timeout;
	/** 任务执行周期。 */
	private long period;

	/** 会话ID与原语关系的映射。 */
	private HashMap<Long, PrimitiveCapsule> dataMap;

	/** 回调监听器。 */
	private AcknowledgementListener listener;

	/** 定时器。 */
	private Timer timer;

	/** 存储回调的 Session 。 */
	private ArrayList<Session> cacheSession;
	/** 存储回调的 Cellet 名。 */
	private ArrayList<String> cacheCellet;
	/** 存储回调的 原语。 */
	private ArrayList<Primitive> cachePrimitive;

	/**
	 * 构造函数。
	 *
	 * @param dataMap 指定维护的数据映射。
	 */
	public Acknowledgement(HashMap<Long, PrimitiveCapsule> dataMap) {
		this.timeout = 5000L;
		this.period = Math.round(((double)timeout * 0.5f));
		this.dataMap = dataMap;
		this.cacheSession = new ArrayList<Session>();
		this.cacheCellet = new ArrayList<String>();
		this.cachePrimitive = new ArrayList<Primitive>();
	}

	/**
	 * 重置超时时间。
	 *
	 * @param timeout 新的超时时间。
	 */
	public void resetTimeout(long timeout) {
		if (this.timeout == timeout) {
			// 超时时间没有改变
			return;
		}

		this.timeout = timeout;
		this.period = Math.round(((double)timeout * 0.5f));

		if (null != this.timer) {
			// 先停止定时器
			this.stop();

			// 再重启定时器
			this.start();
		}
	}

	/**
	 * 返回设置的超时时间。
	 *
	 * @return 返回设置的超时时间。
	 */
	public long getTimeout() {
		return this.timeout;
	}

	/**
	 * 启动管理器。
	 *
	 * @return 启动成功返回 {@code true} 。
	 */
	public boolean start() {
		synchronized (this) {
			if (null != this.timer) {
				return false;
			}

			this.timer = new Timer();
			this.timer.schedule(new AckTimerTask(), 1000L, this.period);

			return true;
		}
	}

	/**
	 * 停止管理器。
	 */
	public void stop() {
		synchronized (this) {
			if (null != this.timer) {
				this.timer.cancel();
				this.timer.purge();
				this.timer = null;
			}

			this.cacheSession.clear();
			this.cacheCellet.clear();
			this.cachePrimitive.clear();
		}
	}

	/**
	 * 设置监听器。
	 *
	 * @param listener 监听器实例。
	 */
	public void setListener(AcknowledgementListener listener) {
		this.listener = listener;
	}

	/**
	 * 定时对未应答的数据进行处理。
	 */
	private class AckTimerTask extends TimerTask {

		public AckTimerTask() {
		}

		@Override
		public void run() {
			long time = Clock.currentTimeMillis();

			synchronized (dataMap) {
				for (PrimitiveCapsule pc : dataMap.values()) {
					synchronized (pc.data) {
						Iterator<Map.Entry<String, LinkedList<Primitive>>> iter = pc.data.entrySet().iterator();
						while (iter.hasNext()) {
							Map.Entry<String, LinkedList<Primitive>> e = iter.next();
							Iterator<Primitive> piter = e.getValue().iterator();
							while (piter.hasNext()) {
								Primitive primitive = piter.next();
								if (time - primitive.timestamp > timeout) {
									// 原语超时
									cacheSession.add(pc.session);
									cacheCellet.add(e.getKey());
									cachePrimitive.add(primitive);
									// 移除
									piter.remove();
								}
							}
						}
					} // #synchronized
				}
			} // #synchronized

			if (!cacheSession.isEmpty()) {
				for (int i = 0, size = cacheSession.size(); i < size; ++i) {
					Session session = cacheSession.get(i);
					String cellet = cacheCellet.get(i);
					Primitive primitive = cachePrimitive.get(i);
					listener.onAckTimeout(session, cellet, primitive);
				}

				cacheSession.clear();
				cacheCellet.clear();
				cachePrimitive.clear();
			}
		}
	}
}
