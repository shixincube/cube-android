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

package cell.core.net;

/**
 * SSL Secure 配置信息。
 */
public class SSLSecurity {

	private String jksSource;

	private String keyStorePassword;

	private String keyManagePassword;

	/**
	 * 构造函数。
	 *
	 * @param jksSource 指定 JKS 文件。
	 * @param keyStorePassword 指定密钥库密钥。
	 * @param keyManagePassword 指定密码。
	 */
	public SSLSecurity(String jksSource, String keyStorePassword, String keyManagePassword) {
		this.jksSource = jksSource;
		this.keyStorePassword = keyStorePassword;
		this.keyManagePassword = keyManagePassword;
	}

	public String getJksSource() {
		return this.jksSource;
	}

	public String getKeyStorePassword() {
		return this.keyStorePassword;
	}

	public String getKeyManagePassword() {
		return this.keyManagePassword;
	}
}
