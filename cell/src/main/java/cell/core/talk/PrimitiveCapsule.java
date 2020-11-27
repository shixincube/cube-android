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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import cell.core.net.Session;

/**
 * 发送的原语封装结构。
 */
public class PrimitiveCapsule {

	/** 关联的会话。 */
	protected Session session;

	/** 对应的数据映射。 */
	protected HashMap<String, LinkedList<Primitive>> data;

	/**
	 * 构造函数。
	 *
	 * @param session 消息层的会话上下文。
	 */
	public PrimitiveCapsule(Session session) {
		this.session = session;
		this.data = new HashMap<String, LinkedList<Primitive>>();
	}

	/**
	 * 返回所有待应答的原语数量。
	 *
	 * @return 返回所有待应答的原语数量。
	 */
	public int getPrimitiveNum() {
		int num = 0;
		synchronized (this.data) {
			Iterator<LinkedList<Primitive>> iter = this.data.values().iterator();
			while (iter.hasNext()) {
				LinkedList<Primitive> list = iter.next();
				num += list.size();
			}
		}
		return num;
	}

	/**
	 * 添加待管理原语。
	 *
	 * @param cellet 目标 Cellet 名称。
	 * @param primitive 原语。
	 */
	public void addPrimitive(String cellet, Primitive primitive) {
		synchronized (this.data) {
			LinkedList<Primitive> list = this.data.get(cellet);
			if (null == list) {
				list = new LinkedList<Primitive>();
				this.data.put(cellet, list);
			}
			list.add(primitive);
		}
	}

	/**
	 * 移除原语。
	 *
	 * @param cellet 目标 Cellet 名称。
	 * @param primitive 原语。
	 */
	public void removePrimitive(String cellet, Primitive primitive) {
		synchronized (this.data) {
			LinkedList<Primitive> list = this.data.get(cellet);
			if (null != list) {
				list.remove(primitive);
			}
		}
	}

	/**
	 * 移除原语。
	 *
	 * @param cellet 目标 Cellet 名称。
	 * @param sn 原语的序号。
	 * @return 返回被移除的原语实例。
	 */
	public Primitive removePrimitive(String cellet, byte[] sn) {
		Primitive result = null;
		synchronized (this.data) {
			LinkedList<Primitive> list = this.data.get(cellet);
			if (null != list) {
				for (Primitive primitive : list) {
					if (primitive.sn[0] == sn[0] && primitive.sn[1] == sn[1]
							&& primitive.sn[2] == sn[2] && primitive.sn[3] == sn[3]) {
						result = primitive;
						list.remove(primitive);
						break;
					}
				}
			}
		}
		return result;
	}

	/**
	 * 清空所有数据。
	 */
	public void clean() {
		synchronized (this.data) {
			for (LinkedList<Primitive> list : this.data.values()) {
				list.clear();
			}

			this.data.clear();
		}
	}

}
