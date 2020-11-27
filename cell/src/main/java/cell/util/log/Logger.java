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

package cell.util.log;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 系统通用日志接口。
 */
public final class Logger {

	private Logger() {
	}

	/**
	 * 打印 DEBUG 级别日志。
	 * 
	 * @param clazz 记录日志的类。
	 * @param log 日志内容。
	 */
	public static void d(Class<?> clazz, String log) {
		LogManager.getInstance().log(LogLevel.DEBUG, clazz.getName(), log);
	}

	/**
	 * 打印 DEBUG 级别日志。
	 * 
	 * @param tag 该条日志的标签。
	 * @param log 日志内容。
	 */
	public static void d(String tag, String log) {
		LogManager.getInstance().log(LogLevel.DEBUG, tag, log);
	}

	/**
	 * 打印 INFO 级别日志。
	 * 
	 * @param clazz 记录日志的类。
	 * @param log 日志内容。
	 */
	public static void i(Class<?> clazz, String log) {
		LogManager.getInstance().log(LogLevel.INFO, clazz.getName(), log);
	}

	/**
	 * 打印 INFO 级别日志。
	 * 
	 * @param tag 该条日志的标签。
	 * @param log 日志内容。
	 */
	public static void i(String tag, String log) {
		LogManager.getInstance().log(LogLevel.INFO, tag, log);
	}

	/**
	 * 打印 WARNING 级别日志。
	 * 
	 * @param clazz 记录日志的类。
	 * @param log 日志内容。
	 */
	public static void w(Class<?> clazz, String log) {
		LogManager.getInstance().log(LogLevel.WARNING, clazz.getName(), log);
	}

	/**
	 * 打印 WARNING 级别日志。
	 * 
	 * @param tag 该条日志的标签。
	 * @param log 日志内容。
	 */
	public static void w(String tag, String log) {
		LogManager.getInstance().log(LogLevel.WARNING, tag, log);
	}

	/**
	 * 打印 WARNING 级别日志。
	 * 
	 * @param clazz 记录日志的类。
	 * @param log 日志内容。
	 * @param exception 日志包含的异常。
	 */
	public static void w(Class<?> clazz, String log, Throwable exception) {
		Logger.log(clazz, log, exception, LogLevel.WARNING);
	}

	/**
	 * 打印 WARNING 级别日志。
	 * 
	 * @param tag 该条日志的标签。
	 * @param log 日志内容。
	 * @param exception 日志包含的异常。
	 */
	public static void w(String tag, String log, Throwable exception) {
		Logger.log(tag, log, exception, LogLevel.WARNING);
	}

	/**
	 * 打印 ERROR 级别日志。
	 * 
	 * @param clazz 记录日志的类。
	 * @param log 日志内容。
	 */
	public static void e(Class<?> clazz, String log) {
		LogManager.getInstance().log(LogLevel.ERROR, clazz.getName(), log);
	}

	/**
	 * 打印 ERROR 级别日志。
	 * 
	 * @param tag 该条日志的标签。
	 * @param log 日志内容。
	 */
	public static void e(String tag, String log) {
		LogManager.getInstance().log(LogLevel.ERROR, tag, log);
	}

	/**
	 * 打印 ERROR 级别日志。
	 * 
	 * @param clazz 记录日志的类。
	 * @param log 日志内容。
	 * @param exception 日志包含的异常。
	 */
	public static void e(Class<?> clazz, String log, Throwable exception) {
		Logger.log(clazz, log, exception, LogLevel.ERROR);
	}

	/**
	 * 打印 ERROR 级别日志。
	 * 
	 * @param tag 该条日志的标签。
	 * @param log 日志内容。
	 * @param exception 日志包含的异常。
	 */
	public static void e(String tag, String log, Throwable exception) {
		Logger.log(tag, log, exception, LogLevel.ERROR);
	}

	/**
	 * 日志管理器是否设置为 DEBUG 等级。
	 *
	 * @return 如果当前日志等级为 DEBUG 返回 {@code true} 。
	 */
	public static boolean isDebugLevel() {
		return (LogManager.getInstance().getLevel() == LogLevel.DEBUG);
	}

	/**
	 * 打印指定等级的包含异常信息的日志。
	 * 
	 * @param clazz 记录日志的类。
	 * @param exception 指定记录的异常。
	 * @param level 日志等级 {@link LogLevel} 。
	 */
	public static void log(Class<?> clazz, Throwable exception, LogLevel level) {
		if (LogManager.getInstance().getLevel().getCode() > level.getCode()) {
			return;
		}

		StringWriter sw = null;
		PrintWriter pw = null;
		try {
			sw = new StringWriter();
			pw = new PrintWriter(sw);
			exception.printStackTrace(pw);
			LogManager.getInstance().log(level, clazz.getName(), "\nCatched exception: " + sw.toString());
		} catch (Exception ie) {
			// Nothing
		} finally {
			try {
				pw.close();
				sw.close();
			} catch (Exception oe) {
				// Nothing
			}
		}
	}

	/**
	 * 打印指定等级的包含异常信息的日志。
	 * 
	 * @param clazz 记录日志的类。
	 * @param log 日志内容。
	 * @param exception 指定记录的异常。
	 * @param level 日志等级 {@link LogLevel} 。
	 */
	public static void log(Class<?> clazz, String log, Throwable exception, LogLevel level) {
		Logger.log(clazz.getName(), log, exception, level);
	}

	/**
	 * 打印指定等级的包含异常信息的日志。
	 * 
	 * @param tag 该条日志的标签。
	 * @param log 日志内容。
	 * @param exception 指定记录的异常。
	 * @param level 日志等级 {@link LogLevel} 。
	 */
	public static void log(String tag, String log, Throwable exception, LogLevel level) {
		if (LogManager.getInstance().getLevel().getCode() > level.getCode()) {
			return;
		}

		StringWriter sw = null;
		PrintWriter pw = null;
		try {
			sw = new StringWriter();
			pw = new PrintWriter(sw);
			exception.printStackTrace(pw);
			LogManager.getInstance().log(level, tag, log + "\nCatched exception: " + sw.toString());
		} catch (Exception ie) {
			// Nothing
		} finally {
			try {
				pw.close();
				sw.close();
			} catch (Exception oe) {
				// Nothing
			}
		}
	}

}
