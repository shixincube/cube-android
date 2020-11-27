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

import cell.core.cellet.Cellet;

/**
 * Cellet 服务接口。
 */
public interface CelletService {

	/**
	 * 安装指定的 Cellet 模组。
	 *
	 * @param cellet Cellet 实例。
	 * @return 返回 {@code true} 表示该 Cellet 被正确安装。
	 */
	public boolean installCellet(Cellet cellet);

	/**
	 * 卸载指定的 Cellet 模组。
	 *
	 * @param cellet Cellet 实例。
	 */
	public void uninstallCellet(Cellet cellet);

	/**
	 * 获得指定名称的 Cellet 模组。
	 *
	 * @param name 指定 Cellet 名称。
	 * @return 返回指定名称的 Cellet 模组实例。
	 */
	public Cellet getCellet(String name);

	/**
	 * 在指定服务端口上启用 Cellet 。
	 *
	 * @param port 指定服务端口。
	 * @param cellet 指定在端口与上启用的 Cellet 。
	 * @return 如果启用成功返回 {@code true} ，否则返回 {@code false} 。
	 */
	public boolean activateCellet(int port, Cellet cellet);

	/**
	 * 在指定服务端口上停用 Cellet 。
	 *
	 * @param port 指定服务端口。
	 * @param cellet 指定在端口上停用的 Cellet 。
	 * @return 如果停用成功返回 {@code true} ，否则返回 {@code false} 。
	 */
	public boolean deactivateCellet(int port, Cellet cellet);

	/**
	 * 获取所有已经安装的 Cellet 。
	 *
	 * @return 返回所有已经安装的 Cellet 实例的列表。
	 */
	public List<Cellet> getCellets();
}
