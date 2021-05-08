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

/**
 * 变量字面意义。
 */
public enum LiteralBase {

	/**
	 * 字符串型。
	 */
	STRING((byte)'S'),

	/**
	 * 整数型。
	 */
	INT((byte)'I'),

	/**
	 * 长整数型。
	 */
	LONG((byte)'L'),

	/**
	 * 浮点型。
	 */
	FLOAT((byte)'F'),

	/**
	 * 使用字符描述的浮点型。
	 */
	FLOAT_S((byte)'f'),

	/**
	 * 双精浮点型。
	 */
	DOUBLE((byte)'D'),

	/**
	 * 使用字符描述的双精浮点型。
	 */
	DOUBLE_S((byte)'d'),

	/**
	 * 布尔型。
	 */
	BOOL((byte)'B'),

	/**
	 * JSON 类型。
	 */
	JSON((byte)'J'),

	/**
	 * 二进制类型。
	 */
	BIN((byte)'N');


	protected byte code;

	LiteralBase(byte code) {
		this.code = code;
	}

	/**
	 * 返回编码。
	 *
	 * @return 返回编码。
	 */
	public byte getCode() {
		return this.code;
	}

	/**
	 * 根据给定的编码解析出对应的字面义。
	 *
	 * @param code 字面义的编码。
	 * @return 返回编码对应的字面义。
	 */
	public static LiteralBase parseLiteralBase(byte code) {
		switch (code) {
			case ((byte)'S'):
				return LiteralBase.STRING;
			case ((byte)'I'):
				return LiteralBase.INT;
			case ((byte)'L'):
				return LiteralBase.LONG;
			case ((byte)'F'):
				return LiteralBase.FLOAT;
			case ((byte)'f'):
				return LiteralBase.FLOAT_S;
			case ((byte)'D'):
				return LiteralBase.DOUBLE;
			case ((byte)'d'):
				return LiteralBase.DOUBLE_S;
			case ((byte)'B'):
				return LiteralBase.BOOL;
			case ((byte)'J'):
				return LiteralBase.JSON;
			default:
				return LiteralBase.BIN;
		}
	}

}
