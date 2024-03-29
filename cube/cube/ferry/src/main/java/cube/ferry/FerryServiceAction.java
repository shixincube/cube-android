/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

package cube.ferry;

/**
 * 数据摆渡服务动作定义。
 */
public final class FerryServiceAction {

    /**
     * 域服务器上线。
     */
    public final static String Online = "online";

    /**
     * 域服务器离线。
     */
    public final static String Offline = "offline";

    /**
     * 执行信条。
     */
    public final static String Tenet = "tenet";

    /**
     * 取出信条。
     */
    public final static String TakeOutTenet = "takeOutTenet";

    /**
     * 查询域。
     */
    public final static String QueryDomain = "queryDomain";

    /**
     * 加入域。
     */
    public final static String JoinDomain = "joinDomain";

    /**
     * 退出域。
     */
    public final static String QuitDomain = "quitDomain";

    /**
     * Ping
     */
    public final static String Ping = "ping";

    /**
     * 报告数据。
     */
    public final static String Report = "report";


    private FerryServiceAction() {
    }
}
