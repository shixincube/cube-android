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

import android.content.Context;

import java.util.concurrent.ConcurrentHashMap;

import cell.core.cellet.CelletServiceKernel;
import cell.core.talk.TalkServiceKernel;
import cell.util.Clock;

/**
 * 内核对象。所有 API 对象的根入口。
 */
public class Nucleus {

	/** 内核配置。 */
	private NucleusConfig config;

	/** 内核标签。 */
	private NucleusTag tag;

	/** 会话服务核心。 */
	private TalkServiceKernel talkService;

	/** Cellet 服务核心。 */
	private CelletServiceKernel celletService;

	/** 全局参数。 */
	private ConcurrentHashMap<String, Object> globalParams;

	/**
	 * 默认配置构造 Nucleus 。
	 *
	 * @param androidContext  Android 上下文对象。
	 */
	public Nucleus(Context androidContext) {
		this(androidContext, new NucleusConfig());
	}

	/**
	 * 通过配置信息构造 Nucleus 。
	 *
	 * @param androidContext  Android 上下文对象。
	 * @param config 内核配置文件。
	 */
	public Nucleus(Context androidContext, NucleusConfig config) {
		this.config = config;
		this.tag = new NucleusTag(androidContext);
		this.talkService = new TalkServiceKernel(this.tag, config);
		this.celletService = new CelletServiceKernel(this);
		this.globalParams = new ConcurrentHashMap<>();
	}

	/**
	 * 销毁 Nucleus 内容。
	 */
	public void destroy() {
		Clock.stop();

		// 停止所有 Speaker 
		this.talkService.hangupAll();
	}

	/**
	 * 获得内核标签。
	 *
	 * @return 返回内核标签对象实例。
	 */
	public NucleusTag getTag() {
		return this.tag;
	}

	/**
	 * 获得会话服务。
	 *
	 * @return 返回会话服务对象实例。
	 */
	public TalkService getTalkService() {
		return this.talkService;
	}

	/**
	 * 获得 Cellet 模组服务。
	 *
	 * @return 返回 Cellet 模组服务对象实例。
	 */
	public CelletService getCelletService() {
		return this.celletService;
	}

	/**
	 * 设置键值对形式的全局参数。
	 *
	 * @param key 指定参数的键。
	 * @param value 指定参数的值。
	 */
	public void setParameter(String key, Object value) {
		this.globalParams.put(key, value);
	}

	/**
	 * 获取一个指定的参数。
	 *
	 * @param key 指定参数的键。
	 * @return 返回参数值。
	 */
	public Object getParameter(String key) {
		return this.globalParams.get(key);
	}
}
