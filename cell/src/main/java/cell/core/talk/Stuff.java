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

import java.nio.charset.Charset;

import cell.util.ByteUtils;
import org.json.JSONException;
import org.json.JSONObject;
import cell.util.log.Logger;

/**
 * 原语素材。
 */
public class Stuff {

	/** 字面义。 */
	protected LiteralBase literal;

	/** 数据。 */
	protected byte[] value;

	/**
	 * 构造字符串类型数据。
	 *
	 * @param value 数据值。
	 */
	public Stuff(String value) {
		this.literal = LiteralBase.STRING;
		this.value = value.getBytes(Charset.forName("UTF-8"));
	}

	/**
	 * 构造整数类型数据。
	 *
	 * @param value 数据值。
	 */
	public Stuff(int value) {
		this.literal = LiteralBase.INT;
		this.value = ByteUtils.toBytes(value);
	}

	/**
	 * 构造长整型数据。
	 *
	 * @param value 数据值。
	 */
	public Stuff(long value) {
		this.literal = LiteralBase.LONG;
		this.value = ByteUtils.toBytes(value);
	}

	/**
	 * 构造浮点型数据。
	 *
	 * @param value 数据值。
	 */
	public Stuff(float value) {
		this.literal = LiteralBase.FLOAT;
		this.value = ByteUtils.toBytes(value);
	}

	/**
	 * 构造双精浮点型数据。
	 *
	 * @param value 数据值。
	 */
	public Stuff(double value) {
		this.literal = LiteralBase.DOUBLE;
		this.value = ByteUtils.toBytes(value);
	}

	/**
	 * 构造布尔型数据。
	 *
	 * @param value 数据值。
	 */
	public Stuff(boolean value) {
		this.literal = LiteralBase.BOOL;
		this.value = ByteUtils.toBytes(value);
	}

	/**
	 * 构造 JSON 数据。
	 *
	 * @param value 数据值。
	 */
	public Stuff(JSONObject value) {
		this.literal = LiteralBase.JSON;
		this.value = value.toString().getBytes(Charset.forName("UTF-8"));
	}

	/**
	 * 构造二进制数据。
	 *
	 * @param value 数据值。
	 */
	public Stuff(byte[] value) {
		this.literal = LiteralBase.BIN;
		this.value = value;
	}

	/**
	 * 构造二进制数据。
	 *
	 * @param value 数据值。
	 * @param offset 数组数据的偏移。
	 * @param length 数据长度。
	 */
	public Stuff(byte[] value, int offset, int length) {
		this.literal = LiteralBase.BIN;
		this.value = new byte[length];
		System.arraycopy(value, offset, this.value, 0, length);
	}

	/**
	 * 用于反序列化的构造函数。
	 *
	 * @param literal 字面义。
	 * @param value 数据值。
	 * @param offset 数组数据的偏移。
	 * @param length 数据长度。
	 */
	public Stuff(byte literal, byte[] value, int offset, int length) {
		this.literal = LiteralBase.parseLiteralBase(literal);
		this.value = new byte[length];
		System.arraycopy(value, offset, this.value, 0, length);
	}

	/**
	 * 获取语素的字面义。
	 *
	 * @return 返回语素的字面义。
	 */
	public LiteralBase getLiteralBase() {
		return this.literal;
	}

	/**
	 * 获取二进制形式的数据值。
	 *
	 * @return 返回二进制形式的数据值。
	 */
	public byte[] getValue() {
		return this.value;
	}

	/**
	 * 获取字符串形式的数据值。
	 *
	 * @return 返回字符串形式的数据值。
	 */
	public String getValueAsString() {
		return new String(this.value, Charset.forName("UTF-8"));
	}

	/**
	 * 获取整数形式的数据值。
	 *
	 * @return 返回整数形式的数据值。
	 */
	public int getValueAsInt() {
		return ByteUtils.toInt(this.value);
	}

	/**
	 * 获取长整数形式的数据值。
	 *
	 * @return 返回长整数形式的数据值。
	 */
	public long getValueAsLong() {
		return ByteUtils.toLong(this.value);
	}

	/**
	 * 获取浮点数形式的数据值。
	 *
	 * @return 返回浮点数形式的数据值。
	 */
	public float getValueAsFloat() {
		if (this.literal == LiteralBase.FLOAT_S) {
			return Float.parseFloat(new String(this.value));
		}

		return ByteUtils.toFloat(this.value);
	}

	/**
	 * 获取双精浮点形式的数据值。
	 *
	 * @return 返回双精浮点形式的数据值。
	 */
	public double getValueAsDouble() {
		if (this.literal == LiteralBase.DOUBLE_S) {
			return Double.parseDouble(new String(this.value));
		}

		return ByteUtils.toDouble(this.value);
	}

	/**
	 * 获取布尔型形式的数据值。
	 *
	 * @return 返回布尔型形式的数据值。
	 */
	public boolean getValueAsBool() {
		return ByteUtils.toBoolean(this.value);
	}

	/**
	 * 获取 JSON 形式的数据值。
	 *
	 * @return 返回 JSON 形式的数据值。
	 */
	public JSONObject getValueAsJson() {
		JSONObject json = null;
		try {
			json = new JSONObject(new String(this.value, Charset.forName("UTF-8")));
		} catch (JSONException e) {
			Logger.e(this.getClass(), "JSON format error", e);
		}
		return json;
	}

	/**
	 * 重置字面义，仅用于矫正浮点数。
	 *
	 * @param newLiteral 新的字面义。
	 */
	protected void reset(LiteralBase newLiteral) {
		if (newLiteral == LiteralBase.FLOAT_S) {
			float fv = this.getValueAsFloat();
			String str = Float.toString(fv);
			this.value = str.getBytes();
			this.literal = newLiteral;
		}
		else if (newLiteral == LiteralBase.DOUBLE_S) {
			double dv = this.getValueAsDouble();
			String str = Double.toString(dv);
			this.value = str.getBytes();
			this.literal = newLiteral;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (null == obj || !(obj instanceof Stuff)) {
			return false;
		}

		Stuff other = (Stuff) obj;

		if (other.literal != this.literal || other.value.length != this.value.length) {
			return false;
		}

		for (int i = 0, size = other.value.length; i < size; ++i) {
			if (other.value[i] != this.value[i]) {
				return false;
			}
		}

		return true;
	}

}
