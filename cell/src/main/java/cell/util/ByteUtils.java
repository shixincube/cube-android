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

package cell.util;

import java.nio.charset.Charset;

/**
 * 字节操作辅助函数。
 */
public final class ByteUtils {

	private ByteUtils() {
	}

	/**
	 * short 转字节数组。
	 *
	 * @param data 输入 short 。
	 * @return 返回字节数组。
	 */
	public static byte[] toBytes(short data) {
		byte[] bytes = new byte[2];
		bytes[0] = (byte) (data & 0xff);
		bytes[1] = (byte) ((data & 0xff00) >> 8);
		return bytes;
	}

	/**
	 * char 转字节数组。
	 *
	 * @param data 输入 char 。
	 * @return 返回字节数组。
	 */
	public static byte[] toBytes(char data) {
		byte[] bytes = new byte[2];
		bytes[0] = (byte) (data);
		bytes[1] = (byte) (data >> 8);
		return bytes;
	}

	/**
	 * int 转字节数组。
	 *
	 * @param data 输入 int  。
	 * @return 返回字节数组。
	 */
	public static byte[] toBytes(int data) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte) (data & 0xff);
		bytes[1] = (byte) ((data & 0xff00) >> 8);
		bytes[2] = (byte) ((data & 0xff0000) >> 16);
		bytes[3] = (byte) ((data & 0xff000000) >> 24);
		return bytes;
	}

	/**
	 * long 转字节数组。
	 *
	 * @param data 输入 long 。
	 * @return 返回字节数组。
	 */
	public static byte[] toBytes(long data) {
		byte[] bytes = new byte[8];
		bytes[0] = (byte) (data & 0xff);
		bytes[1] = (byte) ((data >> 8) & 0xff);
		bytes[2] = (byte) ((data >> 16) & 0xff);
		bytes[3] = (byte) ((data >> 24) & 0xff);
		bytes[4] = (byte) ((data >> 32) & 0xff);
		bytes[5] = (byte) ((data >> 40) & 0xff);
		bytes[6] = (byte) ((data >> 48) & 0xff);
		bytes[7] = (byte) ((data >> 56) & 0xff);
		return bytes;
	}

	/**
	 * float 转字节数组。
	 *
	 * @param data 输入 float 。
	 * @return 返回字节数组。
	 */
	public static byte[] toBytes(float data) {
		int intBits = Float.floatToIntBits(data);
		return toBytes(intBits);
	}

	/**
	 * double 转字节数组。
	 *
	 * @param data 输入 double 。
	 * @return 返回字节数组。
	 */
	public static byte[] toBytes(double data) {
		long longBits = Double.doubleToLongBits(data);
		return toBytes(longBits);
	}

	/**
	 * boolean 转字节数组。
	 *
	 * @param data 输入 boolean 。
	 * @return 返回字节数组。
	 */
	public static byte[] toBytes(boolean data) {
		return new byte[]{ data ? (byte) 1 : (byte) 0 };
	}

	/**
	 * String 转字节数组。
	 *
	 * @param data 输入 String 。
	 * @param charsetName 字符串的解码字符集。
	 * @return 返回字节数组。
	 */
	public static byte[] toBytes(String data, String charsetName) {
		return data.getBytes(Charset.forName(charsetName));
	}

	/**
	 * String 转字节数组。
	 *
	 * @param data 输入 String 。
	 * @return 返回字节数组。
	 */
	public static byte[] toBytes(String data) {
		return toBytes(data, "UTF-8");
	}

	/**
	 * 字节数组转 short 。
	 *
	 * @param bytes 输入数据。
	 * @return 返回 short 。
	 */
	public static short toShort(byte[] bytes) {
		return (short) ((0xff & bytes[0]) | (0xff00 & (bytes[1] << 8)));
	}

	/**
	 * 字节数组转 char 。
	 *
	 * @param bytes 输入数据。
	 * @return 返回 char 。
	 */
	public static char toChar(byte[] bytes) {
		return (char) ((0xff & bytes[0]) | (0xff00 & (bytes[1] << 8)));
	}

	/**
	 * 字节数组转 int 。
	 *
	 * @param bytes 输入数据。
	 * @return 返回 int 。
	 */
	public static int toInt(byte[] bytes) {
		return (0xff & bytes[0])
				| (0xff00 & (bytes[1] << 8))
				| (0xff0000 & (bytes[2] << 16))
				| (0xff000000 & (bytes[3] << 24));
	}

	/**
	 * 字节数组转 long 。
	 *
	 * @param bytes 输入数据。
	 * @return 返回 long 。
	 */
	public static long toLong(byte[] bytes) {
		return (0xffL & (long) bytes[0])
				| (0xff00L & ((long) bytes[1] << 8))
				| (0xff0000L & ((long) bytes[2] << 16))
				| (0xff000000L & ((long) bytes[3] << 24))
				| (0xff00000000L & ((long) bytes[4] << 32))
				| (0xff0000000000L & ((long) bytes[5] << 40))
				| (0xff000000000000L & ((long) bytes[6] << 48))
				| (0xff00000000000000L & ((long) bytes[7] << 56));
	}

	/**
	 * 字节数组转 float 。
	 *
	 * @param bytes 输入数据。
	 * @return 返回 float 。
	 */
	public static float toFloat(byte[] bytes) {
		return Float.intBitsToFloat(toInt(bytes));
	}

	/**
	 * 字节数组转 double 。
	 *
	 * @param bytes 输入数据。
	 * @return 返回 double 。
	 */
	public static double toDouble(byte[] bytes) {
		return Double.longBitsToDouble(toLong(bytes));
	}

	/**
	 * 字节数组转 boolean 。
	 *
	 * @param bytes 输入数据。
	 * @return 返回 boolean 。
	 */
	public static boolean toBoolean(byte[] bytes) {
		return (bytes[0] == (byte)1) ? true : false;
	}

	/**
	 * 字节数组转 String 。
	 *
	 * @param bytes 输入数据。
	 * @param charsetName 字符串的编码字符集。
	 * @return 返回 String 。
	 */
	public static String toString(byte[] bytes, String charsetName) {
		return new String(bytes, Charset.forName(charsetName));
	}

	/**
	 * 字节数组转 String 。
	 *
	 * @param bytes 输入数据。
	 * @return 返回 String 。
	 */
	public static String toString(byte[] bytes) {
		return toString(bytes, "UTF-8");
	}

}
