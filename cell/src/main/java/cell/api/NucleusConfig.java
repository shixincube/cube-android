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

import java.io.File;

/**
 * 内核配置。
 */
public class NucleusConfig {

	/** 心跳间隔，单位：毫秒。 */
	public long heartbeat = 60L * 1000L;

	/** 内核运行的设备类型。 */
	public NucleusDevice nucleusDevice = NucleusDevice.MOBILE;

	/** 内核的工作目录。 */
	public File workingPath = null;

	/**
	 * 构造函数。
	 */
	public NucleusConfig() {
		this.workingPath = new File("");
	}

	/**
	 * 构造函数。
	 *
	 * @param nucleusDevice 内核设备类型。
	 */
	public NucleusConfig(NucleusDevice nucleusDevice) {
		this.nucleusDevice = nucleusDevice;
		this.workingPath = new File("");
	}
}
