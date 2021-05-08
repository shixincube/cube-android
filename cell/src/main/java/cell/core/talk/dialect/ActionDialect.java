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

import org.json.JSONObject;

import cell.core.talk.Primitive;
import cell.core.talk.Stuff;

/**
 * 动作方言。
 * 
 * 动作方言的特点是提供通过键对数据进行索引存储映射。
 */
public class ActionDialect extends Primitive {

	/**
	 * 构造函数。
	 * 用于从原始原语进行构造。
	 *
	 * @param primtive 原语。
	 */
	public ActionDialect(Primitive primtive) {
		super(primtive);
	}

	/**
	 * 构造函数。
	 *
	 * @param name 指定动作名称。
	 */
	public ActionDialect(String name) {
		super();
		this.stuffList.add(new Stuff(name));
	}

	/**
	 * 设置动作名。
	 *
	 * @param name 指定动作名。
	 */
	public void setName(String name) {
		this.stuffList.set(0, new Stuff(name));
	}

	/**
	 * 获取动作名。
	 *
	 * @return 返回动作名。
	 */
	public String getName() {
		return this.stuffList.get(0).getValueAsString();
	}

	/**
	 * 是否包含指定键的参数。
	 *
	 * @param key 指定参数的键。
	 * @return 如果包含该键对应的参数返回 {@code true} ，否则返回 {@code false} 。
	 */
	public boolean containsParam(String key) {
		for (int i = 1, size = this.stuffList.size(); i < size; i += 2) {
			String sk = this.stuffList.get(i).getValueAsString();
			if (sk.equals(key)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 添加值为字符串类型的键值对参数。
	 *
	 * @param key 指定参数的键。
	 * @param value 指定参数的值。
	 */
	public void addParam(String key, String value) {
		int index = keyIndex(key);
		if (index > 0) {
			this.stuffList.set(index + 1, new Stuff(value));
		}
		else {
			this.stuffList.add(new Stuff(key));
			this.stuffList.add(new Stuff(value));
		}
	}

	/**
	 * 添加值为整数类型的键值对参数。
	 *
	 * @param key 指定参数的键。
	 * @param value 指定参数的值。
	 */
	public void addParam(String key, int value) {
		int index = keyIndex(key);
		if (index > 0) {
			this.stuffList.set(index + 1, new Stuff(value));
		}
		else {
			this.stuffList.add(new Stuff(key));
			this.stuffList.add(new Stuff(value));
		}
	}

	/**
	 * 添加值为长整型的键值对参数。
	 *
	 * @param key 指定参数的键。
	 * @param value 指定参数的值。
	 */
	public void addParam(String key, long value) {
		int index = keyIndex(key);
		if (index > 0) {
			this.stuffList.set(index + 1, new Stuff(value));
		}
		else {
			this.stuffList.add(new Stuff(key));
			this.stuffList.add(new Stuff(value));
		}
	}

	/**
	 * 添加值为浮点型的键值对参数。
	 *
	 * @param key 指定参数的键。
	 * @param value 指定参数的值。
	 */
	public void addParam(String key, float value) {
		int index = keyIndex(key);
		if (index > 0) {
			this.stuffList.set(index + 1, new Stuff(value));
		}
		else {
			this.stuffList.add(new Stuff(key));
			this.stuffList.add(new Stuff(value));
		}
	}

	/**
	 * 添加值为双精浮点型的键值对参数。
	 *
	 * @param key 指定参数的键。
	 * @param value 指定参数的值。
	 */
	public void addParam(String key, double value) {
		int index = keyIndex(key);
		if (index > 0) {
			this.stuffList.set(index + 1, new Stuff(value));
		}
		else {
			this.stuffList.add(new Stuff(key));
			this.stuffList.add(new Stuff(value));
		}
	}

	/**
	 * 添加值为布尔型的键值对参数。
	 *
	 * @param key 指定参数的键。
	 * @param value 指定参数的值。
	 */
	public void addParam(String key, boolean value) {
		int index = keyIndex(key);
		if (index > 0) {
			this.stuffList.set(index + 1, new Stuff(value));
		}
		else {
			this.stuffList.add(new Stuff(key));
			this.stuffList.add(new Stuff(value));
		}
	}

	/**
	 * 添加值为 JSON 对象的键值对参数。
	 *
	 * @param key 指定参数的键。
	 * @param value 指定参数的值。
	 */
	public void addParam(String key, JSONObject value) {
		int index = keyIndex(key);
		if (index > 0) {
			this.stuffList.set(index + 1, new Stuff(value));
		}
		else {
			this.stuffList.add(new Stuff(key));
			this.stuffList.add(new Stuff(value));
		}
	}

	/**
	 * 添加值为二进制类型的键值对参数。
	 *
	 * @param key 指定参数的键。
	 * @param value 指定参数的值。
	 */
	public void addParam(String key, byte[] value) {
		int index = keyIndex(key);
		if (index > 0) {
			this.stuffList.set(index + 1, new Stuff(value));
		}
		else {
			this.stuffList.add(new Stuff(key));
			this.stuffList.add(new Stuff(value));
		}
	}

	/**
	 * 移除指定键的参数。
	 *
	 * @param key 指定参数的键。
	 */
	public void removeParam(String key) {
		int index = keyIndex(key);
		if (index > 0) {
			// 对 index 位置进行两次删除操作
			this.stuffList.remove(index);
			this.stuffList.remove(index);
		}
	}

	/**
	 * 获取指定键的参数值。
	 *
	 * @param key 指定参数的键。
	 * @return 返回对应的数据的字节数组形式，如果没有找到该键返回 {@code null} 值。
	 */
	public byte[] getParam(String key) {
		for (int i = 1, size = this.stuffList.size(); i < size; i += 2) {
			String sk = this.stuffList.get(i).getValueAsString();
			if (sk.equals(key)) {
				return this.stuffList.get(i + 1).getValue();
			}
		}

		return null;
	}

	/**
	 * 获取指定键的字符串形式的参数值。
	 *
	 * @param key 指定参数的键。
	 * @return 返回字符串形式的参数值。
	 */
	public String getParamAsString(String key) {
		for (int i = 1, size = this.stuffList.size(); i < size; i += 2) {
			String sk = this.stuffList.get(i).getValueAsString();
			if (sk.equals(key)) {
				return this.stuffList.get(i + 1).getValueAsString();
			}
		}

		return null;
	}

	/**
	 * 获取指定键的整数形式的参数值。
	 *
	 * @param key 指定参数的键。
	 * @return 返回整数形式的参数值。
	 */
	public int getParamAsInt(String key) {
		for (int i = 1, size = this.stuffList.size(); i < size; i += 2) {
			String sk = this.stuffList.get(i).getValueAsString();
			if (sk.equals(key)) {
				return this.stuffList.get(i + 1).getValueAsInt();
			}
		}

		return 0;
	}

	/**
	 * 获取指定键的长整型形式的参数值。
	 *
	 * @param key 指定参数的键。
	 * @return 返回长整型形式的参数值。
	 */
	public long getParamAsLong(String key) {
		for (int i = 1, size = this.stuffList.size(); i < size; i += 2) {
			String sk = this.stuffList.get(i).getValueAsString();
			if (sk.equals(key)) {
				return this.stuffList.get(i + 1).getValueAsLong();
			}
		}

		return 0;
	}

	/**
	 * 获取指定键的浮点型形式的参数值。
	 *
	 * @param key 指定参数的键。
	 * @return 返回浮点型形式的参数值。
	 */
	public float getParamAsFloat(String key) {
		for (int i = 1, size = this.stuffList.size(); i < size; i += 2) {
			String sk = this.stuffList.get(i).getValueAsString();
			if (sk.equals(key)) {
				return this.stuffList.get(i + 1).getValueAsFloat();
			}
		}

		return 0;
	}

	/**
	 * 获取指定键的双精浮点型形式的参数值。
	 *
	 * @param key 指定参数的键。
	 * @return 返回双精浮点型形式的参数值。
	 */
	public double getParamAsDouble(String key) {
		for (int i = 1, size = this.stuffList.size(); i < size; i += 2) {
			String sk = this.stuffList.get(i).getValueAsString();
			if (sk.equals(key)) {
				return this.stuffList.get(i + 1).getValueAsDouble();
			}
		}

		return 0;
	}

	/**
	 * 获取指定键的布尔型形式的参数值。
	 *
	 * @param key 指定参数的键。
	 * @return 返回布尔型形式的参数值。
	 */
	public boolean getParamAsBool(String key) {
		for (int i = 1, size = this.stuffList.size(); i < size; i += 2) {
			String sk = this.stuffList.get(i).getValueAsString();
			if (sk.equals(key)) {
				return this.stuffList.get(i + 1).getValueAsBool();
			}
		}

		return false;
	}

	/**
	 * 获取指定键的 JSON 形式的参数值。
	 *
	 * @param key 指定参数的键。
	 * @return 返回 JSON 形式的参数值。
	 */
	public JSONObject getParamAsJson(String key) {
		for (int i = 1, size = this.stuffList.size(); i < size; i += 2) {
			String sk = this.stuffList.get(i).getValueAsString();
			if (sk.equals(key)) {
				return this.stuffList.get(i + 1).getValueAsJson();
			}
		}

		return null;
	}

	/**
	 * 键所在的 Stuff List 索引。
	 *
	 * @param key 指定参数的键。
	 * @return 返回键所在的索引位置，如果没有找到该键返回 {@code -1} 值。
	 */
	private int keyIndex(String key) {
		for (int i = 1, size = this.stuffList.size(); i < size; i += 2) {
			String sk = this.stuffList.get(i).getValueAsString();
			if (sk.equals(key)) {
				return i;
			}
		}

		return -1;
	}
}
