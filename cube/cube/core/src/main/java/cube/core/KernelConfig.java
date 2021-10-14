/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Shixin Cube Team.
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

package cube.core;

/**
 * 内核配置定义。
 */
public class KernelConfig {

    /**
     * 管道服务器地址。
     */
    public final String address;

    /**
     * 授权的指定域。
     */
    public final String domain;

    /**
     * 当前应用申请到的 App Key 串。
     */
    public final String appKey;

    /**
     * 管道服务器端口。
     */
    public final int port;

    /**
     * 内核是否等待通道就绪再回调。
     */
    public boolean pipelineReady;

    /**
     * 构造函数。
     * 仅用于测试。
     */
    public KernelConfig() {
        this.address = "127.0.0.1";
        this.domain = "shixincube.com";
        this.appKey = "shixin-cubeteam-opensource-appkey";
        this.port = 7000;
        this.pipelineReady = false;
    }

    /**
     * 构造函数。
     *
     * @param address 指定服务器地址。
     * @param domain 指定所属域名称。
     * @param appKey 指定该 App 的 Key 串。
     */
    public KernelConfig(String address, String domain, String appKey) {
        this.address = address;
        this.domain = domain;
        this.appKey = appKey;
        this.port = 7000;
        this.pipelineReady = false;
    }

    /**
     * 构造函数。
     *
     * @param address 指定服务器地址。
     * @param port 指定服务器访问端口。
     * @param domain 指定所属域名称。
     * @param appKey 指定该 App 的 Key 串。
     */
    public KernelConfig(String address, int port, String domain, String appKey) {
        this.address = address;
        this.port = port;
        this.domain = domain;
        this.appKey = appKey;
        this.pipelineReady = false;
    }

    public String print() {
        StringBuilder buf = new StringBuilder("Cube Kernel Config:\n");
        buf.append("address: ").append(this.address).append("\n");
        buf.append("port:    ").append(this.port).append("\n");
        buf.append("domain:  ").append(this.domain).append("\n");
        buf.append("appKey:  ").append(this.appKey).append("\n");
        return buf.toString();
    }
}
