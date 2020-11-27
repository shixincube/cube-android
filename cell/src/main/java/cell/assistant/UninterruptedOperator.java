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

package cell.assistant;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cell.api.NucleusTag;
import cell.api.Servable;
import cell.api.Speakable;
import cell.api.TalkListener;
import cell.api.TalkService;
import cell.core.talk.HeartbeatMachine;
import cell.core.talk.Primitive;
import cell.core.talk.Speaker;
import cell.core.talk.TalkError;

/**
 * 不间断操作器。
 *
 * 当丢失连接后依然可以进行数据发送。
 */
public final class UninterruptedOperator implements TalkService {

    private TalkService talkService;

    private Listener listener;

    private ConcurrentHashMap<String, TalkListener> listenerMap;

    private ConcurrentHashMap<String, Backlog> failedCelletMap;

    public UninterruptedOperator(TalkService talkService) {
        this.talkService = talkService;
        this.listener = new Listener();
        this.listenerMap = new ConcurrentHashMap<>();
        this.failedCelletMap = new ConcurrentHashMap<>();
    }

    @Override
    public Servable startServer(int port) {
        // Nothing
        return null;
    }

    @Override
    public Servable startServer(String host, int port) {
        // Nothing
        return null;
    }

    @Override
    public void stopServer(int port) {
        // Nothing
    }

    @Override
    public void stopAllServers() {
        // Nothing
    }

    @Override
    public List<Servable> getServers() {
        // Nothing
        return null;
    }

    @Override
    public Servable getServer(int port) {
        // Nothing
        return null;
    }

    @Override
    public Speakable call(String host, int port) {
        return this.talkService.call(host, port);
    }

    @Override
    public Speakable call(String host, int port, String cellet) {
        return this.talkService.call(host, port, cellet);
    }

    @Override
    public Speakable call(String host, int port, String[] cellets) {
        return this.talkService.call(host, port, cellets);
    }

    @Override
    public Speakable call(String host, int port, List<String> cellets) {
        return this.talkService.call(host, port, cellets);
    }

    @Override
    public void hangup(String host, int port, boolean now) {
        this.talkService.hangup(host, port, now);
    }

    @Override
    public boolean speak(String cellet, Primitive primitive) {
        return this.speakWithoutAck(cellet, primitive);
    }

    @Override
    public boolean speak(String cellet, Primitive primitive, boolean ack) {
        return ack ? this.speakWithAck(cellet, primitive) : this.speakWithoutAck(cellet, primitive);
    }

    @Override
    public boolean speakWithAck(String cellet, Primitive primitive) {
        if (this.failedCelletMap.containsKey(cellet)) {
            Backlog bl = this.failedCelletMap.get(cellet);
            synchronized (bl) {
                bl.listWithAck.add(primitive);
            }
            return true;
        }

        return this.talkService.speakWithAck(cellet, primitive);
    }

    @Override
    public boolean speakWithoutAck(String cellet, Primitive primitive) {
        if (this.failedCelletMap.containsKey(cellet)) {
            Backlog bl = this.failedCelletMap.get(cellet);
            synchronized (bl) {
                bl.listWithoutAck.add(primitive);
            }
            return true;
        }

        return this.talkService.speakWithoutAck(cellet, primitive);
    }

    @Override
    public void setAckTimeout(long timeout) {
        this.talkService.setAckTimeout(timeout);
    }

    @Override
    public long getAckTimeout() {
        return this.talkService.getAckTimeout();
    }

    @Override
    public void setListener(String cellet, TalkListener listener) {
        this.listenerMap.put(cellet, listener);
        this.talkService.setListener(cellet, this.listener);
    }

    @Override
    public void removeListener(String cellet) {
        this.talkService.removeListener(cellet);
        this.listenerMap.remove(cellet);
    }

    @Override
    public boolean isCalled(String cellet) {
        return this.talkService.isCalled(cellet);
    }

    @Override
    public boolean isCalled(String host, int port) {
        return this.talkService.isCalled(host, port);
    }

    @Override
    public List<String> getCellets(Speakable speakable) {
        return this.talkService.getCellets(speakable);
    }

    @Override
    public HeartbeatMachine.HeartbeatContext getHeartbeatContext(String host, int port) {
        return this.talkService.getHeartbeatContext(host, port);
    }

    @Override
    public NucleusTag getTag() {
        return this.talkService.getTag();
    }


    /**
     * 积压数据。
     */
    private class Backlog {

        protected LinkedList<Primitive> listWithAck;

        protected LinkedList<Primitive> listWithoutAck;

        protected Backlog() {
            this.listWithAck = new LinkedList<>();
            this.listWithoutAck = new LinkedList<>();
        }
    }


    /**
     * 监听器。
     */
    private class Listener implements TalkListener {

        private Listener() {
        }

        @Override
        public void onListened(Speakable speaker, String cellet, Primitive primitive) {
            TalkListener tl = listenerMap.get(cellet);
            if (null != tl) {
                tl.onListened(speaker, cellet, primitive);
            }
        }

        @Override
        public void onSpoke(Speakable speaker, String cellet, Primitive primitive) {
            TalkListener tl = listenerMap.get(cellet);
            if (null != tl) {
                tl.onSpoke(speaker, cellet, primitive);
            }
        }

        @Override
        public void onAck(Speakable speaker, String cellet, Primitive primitive) {
            TalkListener tl = listenerMap.get(cellet);
            if (null != tl) {
                tl.onAck(speaker, cellet, primitive);
            }
        }

        @Override
        public void onSpeakTimeout(Speakable speaker, String cellet, Primitive primitive) {
            TalkListener tl = listenerMap.get(cellet);
            if (null != tl) {
                tl.onSpeakTimeout(speaker, cellet, primitive);
            }
        }

        @Override
        public void onContacted(String cellet, final Speakable speaker) {
            final List<String> cellets = getCellets(speaker);
            if (null != cellets) {
                (new Thread() {
                    @Override
                    public void run() {
                        for (String cellet : cellets) {
                            final Backlog bl = failedCelletMap.remove(cellet);
                            if (null != bl) {
                                synchronized (bl) {
                                    for (Primitive primitive : bl.listWithAck) {
                                        speaker.speakWithAck(cellet, primitive);
                                    }

                                    for (Primitive primitive : bl.listWithoutAck) {
                                        speaker.speakWithoutAck(cellet, primitive);
                                    }
                                }
                            }
                        }
                    }
                }).start();
            }

            for (Map.Entry<String, TalkListener> e : listenerMap.entrySet()) {
                String celletName = e.getKey();
                e.getValue().onContacted(celletName, speaker);
            }
        }

        @Override
        public void onQuitted(Speakable speaker) {
            List<String> cellets = getCellets(speaker);
            if (null != cellets) {
                for (String cellet : cellets) {
                    if (!failedCelletMap.containsKey(cellet)) {
                        Backlog bl = new Backlog();
                        failedCelletMap.put(cellet.toString(), bl);
                    }
                }
            }

            for (TalkListener tl : listenerMap.values()) {
                if (null != tl) {
                    tl.onQuitted(speaker);
                }
            }
        }

        @Override
        public void onFailed(Speakable speaker, TalkError error) {
            Iterator<TalkListener> iter = listenerMap.values().iterator();
            while (iter.hasNext()) {
                TalkListener tl = iter.next();
                tl.onFailed(speaker, error);
            }
        }
    }
}
