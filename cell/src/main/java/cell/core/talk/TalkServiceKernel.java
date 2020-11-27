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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cell.api.NucleusConfig;
import cell.api.NucleusTag;
import cell.api.Servable;
import cell.api.Speakable;
import cell.api.TalkListener;
import cell.api.TalkService;
import cell.api.NucleusDevice;
import cell.core.talk.HeartbeatMachine.HeartbeatContext;
import cell.core.talk.Speaker.State;
import cell.util.log.Logger;

/**
 * Talk 服务实现。
 */
public class TalkServiceKernel implements TalkService, TalkListener {

	/** 内核标签。 */
	private NucleusTag tag;

	/** 内核配置。 */
	private NucleusConfig config;

	/** 服务器。 */
	private ConcurrentHashMap<Integer, BaseServer> servers;

	/** 地址及端口对应的 Speaker 。 */
	private ConcurrentHashMap<String, Speaker> speakers;

	/** 请求的 Cellet 对应的 Speaker 。 */
	private ConcurrentHashMap<String, Speaker> celletSpeakerMap;

	/** Cellet 对应的 Talk 监听器。 */
	private ConcurrentHashMap<String, TalkListener> listeners;

	/** 原语超时时间，单位：毫秒。 */
	private long ackTimeout = 5000L;

	/**
	 * 构造函数。
	 *
	 * @param tag 内核标签。
	 * @param config 内核配置。
	 */
	public TalkServiceKernel(NucleusTag tag, NucleusConfig config) {
		this.tag = tag;
		this.config = config;
		this.servers = new ConcurrentHashMap<Integer, BaseServer>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Servable startServer(int port) {
		return this.startServer("0.0.0.0", port);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Servable startServer(String host, int port) {
		Integer nPort = Integer.valueOf(port);
		BaseServer server = this.servers.get(nPort);
		if (null == server) {
			server = new Server(this.tag.asString(), host, port);
			this.servers.put(nPort, server);
		} else {
			if (!(server instanceof Server)) {
				Logger.w(this.getClass(), "Server socket type is error");
				return null;
			}
		}

		// 设置心跳间隔
		server.setHeartbeat(this.config.heartbeat);

		if (!server.start()) {
			// 记录故障
			Logger.w(this.getClass(), "Start server failed at " + host + ":" + port);
			return null;
		}

		Logger.i(this.getClass(), "Start server " + host + ":" + port);

		return server;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopServer(int port) {
		Integer nPort = Integer.valueOf(port);
		BaseServer server = this.servers.get(nPort);
		if (null != server) {
			server.stop();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopAllServers() {
		Iterator<BaseServer> iter = this.servers.values().iterator();
		while (iter.hasNext()) {
			BaseServer server = iter.next();
			server.stop();

			Logger.i(this.getClass(), "Stop server " + server.getHost() + ":" + server.getPort());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Servable> getServers() {
		ArrayList<Servable> list = new ArrayList<Servable>(this.servers.values());
		return list;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Servable getServer(int port) {
		return this.servers.get(port);
	}

	/**
	 * 添加服务器监听器。
	 *
	 * @param port 指定被监听的服务端口对应的服务器。
	 * @param target 目标名。
	 * @param listener 服务器监听器。
	 * @return 返回服务器实例。
	 */
	public BaseServer addListener(int port, String target, ServerListener listener) {
		BaseServer server = this.servers.get(port);
		if (null != server) {
			server.addListener(target, listener);
		}
		return server;
	}

	/**
	 * 移除服务器监听器。
	 *
	 * @param port 指定被监听的服务端口对应的服务器。
	 * @param target 目标名。
	 * @return 返回服务器实例。
	 */
	public BaseServer removeListener(int port, String target) {
		BaseServer server = this.servers.get(port);
		if (null != server) {
			server.removeListener(target);
		}
		return server;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Speakable call(String host, int port) {
		return this.call(host, port, new ArrayList<String>());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Speakable call(String host, int port, String cellet) {
		return this.call(host, port, new String[] { cellet });
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Speakable call(String host, int port, String[] cellets) {
		ArrayList<String> list = new ArrayList<String>(cellets.length);
		for (String cellet : cellets) {
			list.add(cellet);
		}
		return this.call(host, port, list);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Speakable call(String host, int port, List<String> cellets) {
		if (null == this.speakers) {
			this.speakers = new ConcurrentHashMap<String, Speaker>();
		}

		if (null == this.celletSpeakerMap) {
			this.celletSpeakerMap = new ConcurrentHashMap<String, Speaker>();
		}

		String key = host + ":" + port;
		Speaker speaker = this.speakers.get(key);
		if (null == speaker) {
			speaker = new Speaker(this.tag, host, port, this);
			speaker.setAckTimeout(this.ackTimeout);
			this.speakers.put(key, speaker);
		}

		// 根据设备类型决定线程数量
		if (!speaker.start(config.nucleusDevice == NucleusDevice.SERVER ? 4 : 2)) {
			Logger.e(this.getClass(), "Call '" + key + "' failed");
			return null;
		}

		if (null != cellets && !cellets.isEmpty()) {
			for (String cellet : cellets) {
				this.celletSpeakerMap.put(cellet, speaker);
			}
		}

		return speaker;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void hangup(String host, int port, boolean now) {
		if (null == this.speakers) {
			return;
		}

		String key = host + ":" + port;
		Speaker speaker = this.speakers.remove(key);
		if (null == speaker) {
			return;
		}

		// 停止
		speaker.stop(now);

		if (null != this.celletSpeakerMap) {
			Iterator<Map.Entry<String, Speaker>> iter = this.celletSpeakerMap.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, Speaker> e = iter.next();
				Speaker spk = e.getValue();
				if (spk == speaker) {
					iter.remove();
				}
			}
		}
	}

	/**
	 * 挂断所有 Speaker ，从服务器断开连接。
	 */
	public void hangupAll() {
		if (null == this.speakers) {
			return;
		}

		Iterator<Speaker> spkiter = this.speakers.values().iterator();
		while (spkiter.hasNext()) {
			Speaker speaker = spkiter.next();

			spkiter.remove();

			// 停止
			speaker.stop(false);

			if (null != this.celletSpeakerMap) {
				Iterator<Map.Entry<String, Speaker>> iter = this.celletSpeakerMap.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry<String, Speaker> e = iter.next();
					Speaker spk = e.getValue();
					if (spk == speaker) {
						iter.remove();
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean speak(String cellet, Primitive primitive) {
		return speakWithoutAck(cellet, primitive);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean speak(String cellet, Primitive primitive, boolean ack) {
		if (ack) {
			return speakWithAck(cellet, primitive);
		}
		else {
			return speakWithoutAck(cellet, primitive);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean speakWithAck(String cellet, Primitive primitive) {
		if (null == this.celletSpeakerMap) {
			return false;
		}

		if (0 == primitive.numStuff()) {
			Logger.w(this.getClass(), "Primitive has no sutff data");
			return false;
		}

		Speaker speaker = this.celletSpeakerMap.get(cellet);
		if (null == speaker) {
			Iterator<Speaker> iter = this.speakers.values().iterator();
			if (!iter.hasNext()) {
				Logger.w(this.getClass(), "Can not find speaker for " + cellet);
				return false;
			}

			speaker = iter.next();
			this.celletSpeakerMap.put(cellet, speaker);
		}

		return speaker.speakWithAck(cellet, primitive);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean speakWithoutAck(String cellet, Primitive primitive) {
		if (null == this.celletSpeakerMap) {
			return false;
		}

		if (0 == primitive.numStuff()) {
			Logger.w(this.getClass(), "Primitive has no sutff data");
			return false;
		}

		Speaker speaker = this.celletSpeakerMap.get(cellet);
		if (null == speaker) {
			Iterator<Speaker> iter = this.speakers.values().iterator();
			if (!iter.hasNext()) {
				Logger.w(this.getClass(), "Can not find speaker for " + cellet);
				return false;
			}

			speaker = iter.next();
			this.celletSpeakerMap.put(cellet, speaker);
		}

		return speaker.speakWithoutAck(cellet, primitive);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setAckTimeout(long timeout) {
		this.ackTimeout = timeout;
		Iterator<Speaker> iter = this.speakers.values().iterator();
		while (iter.hasNext()) {
			Speaker speaker = iter.next();
			speaker.setAckTimeout(timeout);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getAckTimeout() {
		return this.ackTimeout;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setListener(String cellet, TalkListener listener) {
		if (null == this.listeners) {
			this.listeners = new ConcurrentHashMap<String, TalkListener>();
		}

		if (null == listener) {
			this.listeners.remove(cellet);
		}
		else {
			this.listeners.put(cellet, listener);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeListener(String cellet) {
		if (null == this.listeners) {
			return;
		}

		this.listeners.remove(cellet);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCalled(String cellet) {
		Speaker speaker = this.celletSpeakerMap.get(cellet);
		if (null != speaker) {
			return (State.Connected == speaker.getState());
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCalled(String host, int port) {
		String key = host + ":" + port;
		Speaker speaker = this.speakers.get(key);
		if (null == speaker) {
			return false;
		}

		return State.Connected == speaker.getState();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getCellets(Speakable speakable) {
		if (null == this.celletSpeakerMap) {
			return null;
		}

		ArrayList<String> result = new ArrayList<>();
		
		for (Map.Entry<String, Speaker> e : this.celletSpeakerMap.entrySet()) {
			String cellet = e.getKey();
			Speaker speaker = e.getValue();
			if (speaker == (Speaker) speakable) {
				result.add(cellet);
			}
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public HeartbeatContext getHeartbeatContext(String host, int port) {
		if (null == this.speakers) {
			return null;
		}

		String key = host + ":" + port;
		Speaker speaker = this.speakers.get(key);
		if (null == speaker) {
			return null;
		}

		return speaker.getHeartbeatMachine().getContext(speaker.getSession());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NucleusTag getTag() {
		return this.tag;
	}

	// -------------------------------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onListened(Speakable speaker, String cellet, Primitive primitive) {
		if (null == this.listeners) {
			return;
		}

		TalkListener listener = this.listeners.get(cellet);
		if (null != listener) {
			listener.onListened(speaker, cellet, primitive);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSpoke(Speakable speaker, String cellet, Primitive primitive) {
		if (null == this.listeners) {
			return;
		}

		TalkListener listener = this.listeners.get(cellet);
		if (null != listener) {
			listener.onSpoke(speaker, cellet, primitive);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAck(Speakable speaker, String cellet, Primitive primitive) {
		if (null == this.listeners) {
			return;
		}

		TalkListener listener = this.listeners.get(cellet);
		if (null != listener) {
			listener.onAck(speaker, cellet, primitive);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSpeakTimeout(Speakable speaker, String cellet, Primitive primitive) {
		if (null == this.listeners) {
			return;
		}

		TalkListener listener = this.listeners.get(cellet);
		if (null != listener) {
			listener.onSpeakTimeout(speaker, cellet, primitive);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onContacted(String cellet, Speakable speaker) {
		if (null == this.listeners) {
			return;
		}

		if (speaker instanceof Speaker) {
			for (Map.Entry<String, TalkListener> e : this.listeners.entrySet()) {
				String celletName = e.getKey();
				Speaker celletSpeaker = this.celletSpeakerMap.get(celletName);
				e.getValue().onContacted(celletName, celletSpeaker);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onQuitted(Speakable speaker) {
		if (null == this.listeners) {
			return;
		}

		if (speaker instanceof Speaker) {
			Speaker spr = (Speaker) speaker;

			for (Map.Entry<String, TalkListener> e : this.listeners.entrySet()) {
				String celletName = e.getKey();
				Speaker celletSpeaker = this.celletSpeakerMap.get(celletName);
				if (null != celletSpeaker) {
					if (celletSpeaker == spr) {
						e.getValue().onQuitted(speaker);
					}
				}
				else {
					e.getValue().onQuitted(speaker);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onFailed(Speakable speaker, TalkError error) {
		if (null == this.listeners) {
			return;
		}

		if (speaker instanceof Speaker) {
			Speaker spr = (Speaker) speaker;

			for (Map.Entry<String, TalkListener> e : this.listeners.entrySet()) {
				String celletName = e.getKey();
				Speaker celletSpeaker = this.celletSpeakerMap.get(celletName);
				if (null != celletSpeaker) {
					if (celletSpeaker == spr) {
						e.getValue().onFailed(speaker, error);
					}
				}
				else {
					e.getValue().onFailed(speaker, error);
				}
			}
		}
	}

	// -------------------------------------------------------------------------

}
