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

import java.util.LinkedList;

import cell.util.Utils;

/**
 * 原语描述。
 */
public class Primitive {

	/** 创建时的时间戳。 */
	protected long timestamp;

	/** 原语的序号。 */
	protected byte[] sn;

	/** 原语包含的素材列表。 */
	protected LinkedList<Stuff> stuffList;

	/**
	 * 构造函数。
	 */
	public Primitive() {
		this.timestamp = System.currentTimeMillis();
		this.sn = Utils.randomBytes(4);
		this.stuffList = new LinkedList<Stuff>();
	}

	/**
	 * 构造函数。
	 * 
	 * @param sn 指定原语的序号。
	 */
	public Primitive(byte[] sn) {
		this.timestamp = System.currentTimeMillis();
		this.sn = sn;
		this.stuffList = new LinkedList<Stuff>();
	}

	/**
	 * 构造函数。
	 *
	 * @param primitive 原语。
	 */
	public Primitive(Primitive primitive) {
		this.timestamp = primitive.timestamp;
		this.sn = primitive.sn;
		this.stuffList = primitive.stuffList;
	}

	/**
	 * 返回序号。
	 *
	 * @return 返回序号。
	 */
	public byte[] getSN() {
		return this.sn;
	}

	/**
	 * 提交语素到原语。
	 *
	 * @param stuff 提交的语素。
	 * @return 返回添加的 Stuff 的索引位置。
	 */
	public int commit(Stuff stuff) {
		this.stuffList.add(stuff);
		return (this.stuffList.size() - 1);
	}

	/**
	 * 设置指定索引处的语素。
	 *
	 * @param index 指定索引位置。
	 * @param stuff 指定语素。
	 */
	public void setStuff(int index, Stuff stuff) {
		this.stuffList.set(index, stuff);
	}

	/**
	 * 获取指定索引位置的语素。
	 *
	 * @param index 指定索引位置。
	 * @return 返回语素。
	 */
	public Stuff getStuff(int index) {
		return this.stuffList.get(index);
	}

	/**
	 * 删除指定索引位置的语素。
	 *
	 * @param index 指定索引位置。
	 * @return 返回语素。
	 */
	public Stuff removeStuff(int index) {
		return this.stuffList.remove(index);
	}

	/**
	 * 获取原语里的语素数量。
	 *
	 * @return 返回原语里的语素数量。
	 */
	public int numStuff() {
		return this.stuffList.size();
	}

	@Override
	public boolean equals(Object obj) {
		if (null == obj || !(obj instanceof Primitive)) {
			return false;
		}

		Primitive other = (Primitive) obj;
		if (other.stuffList.size() != this.stuffList.size()) {
			return false;
		}

		for (int i = 0, size = other.stuffList.size(); i < size; ++i) {
			Stuff stuff = other.stuffList.get(i);
			if (!stuff.equals(this.stuffList.get(i))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public Primitive clone() {
		Primitive clone = new Primitive(this.sn);
		clone.timestamp = this.timestamp;

		for (Stuff stuff : this.stuffList) {
			switch (stuff.literal) {
			case STRING:
				clone.commit(new Stuff(stuff.getValueAsString()));
				break;
			case INT:
				clone.commit(new Stuff(stuff.getValueAsInt()));
				break;
			case LONG:
				clone.commit(new Stuff(stuff.getValueAsLong()));
				break;
			case FLOAT:
				clone.commit(new Stuff(stuff.getValueAsFloat()));
				break;
			case DOUBLE:
				clone.commit(new Stuff(stuff.getValueAsDouble()));
				break;
			case BOOL:
				clone.commit(new Stuff(stuff.getValueAsBool()));
				break;
			case JSON:
				clone.commit(new Stuff(stuff.getValueAsJson()));
				break;
			case BIN:
				clone.commit(new Stuff(stuff.getValue()));
				break;
			default:
				clone.commit(new Stuff(stuff.getValue()));
				break;
			}
		}
		return clone;
	}

	/**
	 * 判断序号是否和当前原语的序号相同。
	 *
	 * @param sn 指定序号。
	 * @return 如果序号相同返回 {@code true} 。
	 */
	public boolean equalsSN(byte[] sn) {
		if (sn.length != this.sn.length) {
			return false;
		}

		for (int i = 0; i < sn.length; ++i) {
			if (sn[i] != this.sn[i]) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 校验所有的 {@link Stuff} 的数据类型，尽可能使用安全的数据类型。
	 */
	public void adjustSafeType() {
		for (Stuff stuff : this.stuffList) {
			if (stuff.literal == LiteralBase.FLOAT) {
				stuff.reset(LiteralBase.FLOAT_S);
			}
			else if (stuff.literal == LiteralBase.DOUBLE) {
				stuff.reset(LiteralBase.DOUBLE_S);
			}
		}
	}
}
