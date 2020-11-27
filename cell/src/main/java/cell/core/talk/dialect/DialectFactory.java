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

package cell.core.talk.dialect;

import java.util.concurrent.ConcurrentHashMap;

import cell.core.talk.Primitive;

/**
 * 方言工厂。
 */
public class DialectFactory {

	private static DialectFactory instance = new DialectFactory();

	private ConcurrentHashMap<PSN, ActionDialect> cache;

	private int maxCacheSize = 1000;

	private DialectFactory() {
		this.cache = new ConcurrentHashMap<PSN, ActionDialect>();
	}

	/**
	 * 获得方言工厂的单例。
	 *
	 * @return 返回方言工厂的单例。
	 */
	public static DialectFactory getInstance() {
		return DialectFactory.instance;
	}

	/**
	 * 通过原语创建方言实例。
	 *
	 * @param primitive 原语对象。
	 * @return 返回动作方言实例。
	 */
	public ActionDialect createActionDialect(Primitive primitive) {
		if (this.cache.size() > this.maxCacheSize) {
			this.cache.clear();
		}

		PSN psn = new PSN(primitive.getSN());
		ActionDialect dialect = this.cache.get(psn);
		if (null == dialect) {
			dialect = new ActionDialect(primitive);
			this.cache.put(psn, dialect);
		}
		return dialect;
	}


	/**
	 * PSN
	 */
	protected class PSN {

		protected byte[] sn;

		public PSN(byte[] sn) {
			this.sn = sn;
		}

		@Override
		public boolean equals(Object obj) {
			if (null != obj && obj instanceof PSN) {
				PSN other = (PSN) obj;
				return (other.sn[0] == this.sn[0]) && (other.sn[1] == this.sn[1])
						&& (other.sn[2] == this.sn[2]) && (other.sn[3] == this.sn[3]);
			}

			return false;
		}

		@Override
		public int hashCode() {
			return ((int)this.sn[0] + (int)this.sn[1] + (int)this.sn[2] + (int)this.sn[3]);
		}
	}
}
