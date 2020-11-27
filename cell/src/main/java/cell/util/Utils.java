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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

/**
 * 实用函数库。
 */
public class Utils {

	/**
	 * SN 魔法数。
	 */
	//private final static long sMagicNum = Math.round(System.currentTimeMillis() * 0.5f);

	/**
	 * 随机数发生器。
	 */
	private final static Random sRandom = new Random(System.currentTimeMillis());

	/**
	 * 字母表。
	 */
	private static final char[] ALPHABET = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
			'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
			'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

	/**
	 * 数字表。
	 */
	private static final char[] NUMERATION = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

	/**
	 * 常用日期格式。
	 */
	public final static SimpleDateFormat gsDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/** KB 换算值。 */
	private final static long CONV_KB = 1024L;

	/** MB 换算值。 */
	private final static long CONV_MB = 1024L * CONV_KB;

	/** GB 换算值。 */
	private final static long CONV_GB = 1024L * CONV_MB;

	/** TB 换算值。 */
	private final static long CONV_TB = 1024L * CONV_GB;

	private Utils() {
	}

	/**
	 * 生成随机长整数。
	 * 
	 * @return 返回随机长整数。
	 */
	public static long randomLong() {
		return sRandom.nextLong();
	}

	/**
	 * 生成随机整数。
	 * 
	 * @return 返回随机整数。
	 */
	public static int randomInt() {
		return sRandom.nextInt();
	}

	/**
	 * 生成随机无符号整数。
	 * 
	 * @return 返回随机无符号整数。
	 */
	public static int randomUnsigned() {
		return sRandom.nextInt() & 0x0FFFF;
	}

	/**
	 * 生成随机无符号长整数。
	 *
	 * @return 返回随机无符号长整数。
	 */
	public static long randomUnsignedLong() {
		return sRandom.nextLong() & 0x0FFFFFFFFl;
	}

	/**
	 * 生成指定范围内的随机整数。
	 *
	 * @param floor 指定起始值（包含）。
	 * @param ceil  指定结束值（包含）。
	 * @return 返回指定范围内的随机整数。
	 */
	public static int randomInt(final int floor, final int ceil) {
		if (floor > ceil) {
			return floor;
		}

		int realFloor = floor + 1;
		int realCeil = ceil + 1;

		return (sRandom.nextInt(realCeil) % (realCeil - realFloor + 1) + realFloor) - 1;
	}

	/**
	 * 生成指定长度的随机字符串。
	 *
	 * @param length 指定需生成字符串的长度。
	 * @return 返回指定长度的随机字符串。
	 */
	public static String randomString(int length) {
		char[] buf = new char[length];
		int max = ALPHABET.length - 1;
		int min = 0;
		int index = 0;
		for (int i = 0; i < length; ++i) {
			index = sRandom.nextInt(max) % (max - min + 1) + min;
			buf[i] = ALPHABET[index];
		}
		return new String(buf);
	}

	/**
	 * 生成指定长度的随机数字串。
	 *
	 * @param length 指定需生成数字串的长度。
	 * @return 返回指定长度的随机数字串。
	 */
	public static String randomNumberString(int length) {
		char[] buf = new char[length];
		int max = NUMERATION.length - 1;
		int min = 0;
		int index = 0;

		// 第一个字符不能是 '0'
		index = sRandom.nextInt(max) % (max - min + 1) + min;
		buf[0] = NUMERATION[(index == 0 ? 1 : index)];

		for (int i = 1; i < length; ++i) {
			index = sRandom.nextInt(max) % (max - min + 1) + min;
			buf[i] = NUMERATION[index];
		}

		return new String(buf);
	}

	/**
	 * 生成指定长度的随机字节数组。
	 *
	 * @param length 指定需生成数组的长度。
	 * @return 返回指定长度的随机字节数组。
	 */
	public static byte[] randomBytes(int length) {
		byte[] bytes = new byte[length];
		sRandom.nextBytes(bytes);
		return bytes;
	}

	/**
	 * 生成系统全局唯一序列号。
	 *
	 * @return 返回长整型形式的系统全局唯一序列号。
	 */
	public static long generateSerialNumber() {
		long sn = System.currentTimeMillis();
		sn += sRandom.nextInt();
		return sn;
	}

	/**
	 * 生成系统全局唯一无符号序列号。
	 *
	 * @return 返回长整型形式的系统全局唯一无符号序列号。
	 */
	public static long generateUnsignedSerialNumber() {
		long sn = System.currentTimeMillis();
		sn += sRandom.nextInt();
		sn = sn & 0x0FFFFFFFFl;
		return sn;
	}

	/**
	 * GZIP 格式压缩数据。
	 *
	 * @param input 指定输入数据。
	 * @return 返回压缩后的数据。
	 */
	public static byte[] gzCompress(byte[] input) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(input);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		GZIPOutputStream gos = null;

		byte[] ret = null;
		try {
			gos = new GZIPOutputStream(os);

			int len = 0;
			byte[] buf = new byte[1024];
			while ((len = is.read(buf)) > 0) {
				gos.write(buf, 0, len);
			}

			gos.finish();
			gos.flush();

			ret = os.toByteArray();
		} finally {
			if (null != gos) {
				gos.close();
			}

			os.close();
		}

		return ret;
	}

	/**
	 * ZIP 压缩数据。
	 *
	 * @param data 指定输入数据。
	 * @return 返回压缩后的数据。
	 * @throws IOException
	 */
	public static byte[] compress(byte[] data) throws IOException {
		byte[] output = null;

		Deflater compresser = new Deflater();
		compresser.reset();
		compresser.setInput(data);
		compresser.finish();

		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
		try {
			byte[] buf = new byte[1024];
			while (!compresser.finished()) {
				int i = compresser.deflate(buf);
				bos.write(buf, 0, i);
			}
			output = bos.toByteArray();
		} finally {
			try {
				bos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		compresser.end();
		return output;
	}

	/**
	 * GZIP 格式解压数据。
	 *
	 * @param input 指定输入数据。
	 * @return 返回解压后的数据。
	 * @throws IOException
	 */
	public static byte[] gzUncompress(byte[] input) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(input);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		GZIPInputStream gis = null;

		byte[] ret = null;
		try {
			gis = new GZIPInputStream(is);

			int len = 0;
			byte[] buf = new byte[1024];
			while ((len = gis.read(buf)) > 0) {
				os.write(buf, 0, len);
			}

			os.flush();

			ret = os.toByteArray();
		} finally {
			if (null != gis) {
				gis.close();
			}

			os.close();
		}

		return ret;
	}

	/**
	 * ZIP 解压数据。
	 *
	 * @param data 指定输入数据。
	 * @return 返回解压后的数据。
	 * @throws IOException
	 */
	public static byte[] uncompress(byte[] data) throws IOException {
		byte[] output = null;

		Inflater decompresser = new Inflater();
		decompresser.reset();
		decompresser.setInput(data);

		ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);
		try {
			byte[] buf = new byte[1024];
			while (!decompresser.finished()) {
				int i = decompresser.inflate(buf);
				o.write(buf, 0, i);
			}
			output = o.toByteArray();
		} catch (DataFormatException e) {
			throw new IOException(e.getMessage());
		} finally {
			try {
				o.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		decompresser.end();
		return output;
	}

	/**
	 * 拷贝文件。
	 *
	 * @param srcFile  指定源文件。
	 * @param destFile 指定目标文件。
	 * @return 拷贝成功返回 {@code true} 。
	 * @throws IOException
	 */
	public static long copyFile(File srcFile, File destFile) throws IOException {
		long bytesum = 0;
		int byteread = 0;

		if (srcFile.exists()) {
			FileInputStream fis = null;
			FileOutputStream fos = null;
			try {
				fis = new FileInputStream(srcFile);
				fos = new FileOutputStream(destFile);
				byte[] buffer = new byte[4096];
				while ((byteread = fis.read(buffer)) > 0) {
					bytesum += byteread;
					fos.write(buffer, 0, byteread);
				}
				fos.flush();
			} catch (IOException e) {
				throw e;
			} finally {
				if (null != fis) {
					try {
						fis.close();
					} catch (IOException e) {
					}
				}
				if (null != fos) {
					try {
						fos.close();
					} catch (IOException e) {
					}
				}
			}
		}

		return bytesum;
	}

	/**
	 * 便捷的字符串转字节数组。
	 * 
	 * @param string 指定字符串。
	 * @return 返回字符串的字节数组形式。
	 */
	public static byte[] string2Bytes(String string) {
		return string.getBytes(Charset.forName("UTF-8"));
	}

	/**
	 * 日期类型转字符串格式。
	 * 
	 * @param date 指定日期对象。
	 * @return 返回日期类型转字符串格式。
	 */
	public static String convertDateToSimpleString(Date date) {
		return gsDateFormat.format(date);
	}

	/**
	 * 基于 FNV 的字符串 Hash 函数。
	 * 
	 * @param string 指定字符串。
	 * @return 返回字符串的 Hash 值。
	 */
	public static long hashString(String string) {
		final int p = 16777619;
		long hash = 2166136261L;
		for (int i = 0, len = string.length(); i < len; ++i) {
			hash = (hash ^ string.charAt(i)) * p;
		}
		hash += hash << 13;
		hash ^= hash >> 7;
		hash += hash << 3;
		hash ^= hash >> 17;
		hash += hash << 5;
		return hash;
	}

	/**
	 * 格式化输出指定字节的空间大小。
	 * 
	 * @param size 指定待格式化的字节大小。
	 * @return 返回格式化的字节大小。
	 */
	public static String formatSize(long size) {
		if (size >= CONV_KB && size < CONV_MB) {
			double r = (double)size / (double)CONV_KB;
			return String.format("%.2f KB", r);
		}
		else if (size >= CONV_MB && size < CONV_GB) {
			double r = (double)size / (double)CONV_MB;
			return String.format("%.2f MB", r);
		}
		else if (size >= CONV_GB && size < CONV_TB) {
			double r = (double)size / (double)CONV_GB;
			return String.format("%.2f GB", r);
		}
		else if (size >= CONV_TB) {
			double r = (double)size / (double)CONV_TB;
			return String.format("%.2f TB", r);
		}
		else {
			return size + " bytes";
		}
	}
}
