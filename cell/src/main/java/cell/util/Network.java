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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;

import cell.util.log.Logger;

/**
 * 网络相关操作实用函数库。
 */
public final class Network {

	private static final Class<?> TAG = Network.class;

	/**
	 * 网络类型。
	 */
	public enum NetworkType {
		/** 未知的网络类型。 */
		Unknown(0),

		/** WIFI 类型。 */
		Wifi(1),

		/** 移动网络 2G 类型。 */
		Mobile2G(2),

		/** 移动网络 3G 类型。 */
		Mobile3G(3),

		/** 移动网络 4G 类型。 */
		Mobile4G(4),

		/**移动网络 5G 类型。  */
		Mobile5G(5);

		private int code;

		NetworkType(int code) {
			this.code = code;
		}
	}

	/**
	 * 判断网络连接是否有效（此时可传输数据）。
	 *
	 * @param context 应用程序上下文。
	 * @return 该方法仅对是否连入网络进行判断，无论是 Wifi 还是 Mobile 接入网络，
	 * 只要当前设备接入网络就返回 {@code true} ，否则返回 {@code false} 。
	 */
	public static boolean isConnected(Context context) {
		NetworkInfo net = getConnectivityManager(context).getActiveNetworkInfo();
		return net != null && net.isConnected();
	}

	/**
	 * 判断有无网络正在连接中（查找网络、校验、获取IP等）。
	 *
	 * @param context 应用程序上下文。
	 * @return 如果有设备正在查找网络返回 {@code true}。
	 */
	public static boolean isConnectedOrConnecting(Context context) {
		NetworkInfo[] nets = getConnectivityManager(context).getAllNetworkInfo();
		if (nets != null) {
			for (NetworkInfo net : nets) {
				if (net.isConnectedOrConnecting()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 获取 ConnectivityManager 。
	 *
	 * @param context 应用程序上下文。
	 * @return 返回 ConnectivityManager 。
	 */
	private static ConnectivityManager getConnectivityManager(Context context) {
		return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	/**
	 * 获取 TelephonyManager 。
	 *
	 * @param context 应用程序上下文。
	 * @return 返回 TelephonyManager 。
	 */
	private static TelephonyManager getTelephonyManager(Context context) {
		return (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	}

	/**
	 * 是否存在有效的 WIFI 连接
	 *
	 * @param context 应用程序上下文。
	 * @return 如果当前有有效接入网络的 WIFI 连接返回 {@code true} 。
	 */
	public static boolean isWifiConnected(Context context) {
		NetworkInfo net = getConnectivityManager(context).getActiveNetworkInfo();
		return net != null && net.getType() == ConnectivityManager.TYPE_WIFI && net.isConnected();
	}

	/**
	 * 是否存在有效的移动网络连接。
	 *
	 * @param context 应用程序上下文。
	 * @return 如果当前有有效接入网络的移动网络连接返回 {@code true} 。
	 */
	public static boolean isMobileConnected(Context context) {
		NetworkInfo net = getConnectivityManager(context).getActiveNetworkInfo();
		return net != null && net.getType() == ConnectivityManager.TYPE_MOBILE && net.isConnected();
	}

	/**
	 * 检测网络是否为可用状态。
	 *
	 * @param context 应用程序上下文。
	 * @return 如果当前有任意网络类型可用返回 {@code true} 。
	 */
	public static boolean isAvailable(Context context) {
		return isWifiAvailable(context) || (isMobileAvailable(context) && isMobileEnabled(context))
				|| isEthernetAvailable(context);
	}

	/**
	 * 判断是否有可用状态的 Wifi 网络。以下情况返回 {@code false}：
	 * <ul>
	 *     <li>设备 WIFI 开关已关闭</li>
	 *     <li>已经打开飞行模式</li>
	 *     <li>设备所在区域没有信号覆盖</li>
	 *     <li>设备在漫游区域，且关闭了网络漫游</li>
	 * </ul>
	 *
	 * @param context 应用程序上下文。
	 * @return 返回 WIFI 是否可用，不判断是否 WIFI 接入网络。
	 */
	public static boolean isWifiAvailable(Context context) {
		NetworkInfo[] nets = getConnectivityManager(context).getAllNetworkInfo();
		if (nets != null) {
			for (NetworkInfo net : nets) {
				if (net.getType() == ConnectivityManager.TYPE_WIFI) {
					return net.isAvailable();
				}
			}
		}
		return false;
	}

	/**
	 * 判断有无可用状态的移动网络，注意关掉设备移动网络直接不影响此函数。
	 * 也就是即使关掉移动网络，那么移动网络也可能是可用的（彩信等服务）。
	 * <p>
	 * 以下情况它是不可用的，将返回 {@code false}：
	 * <ul>
	 *     <li>设备打开飞行模式</li>
	 *     <li>设备所在区域没有信号覆盖</li>
	 *     <li>设备在漫游区域，且关闭了网络漫游</li>
	 * </ul>
	 * </p>
	 *
	 * @param context 应用程序上下文。
	 * @return 返回移动网络是否可用。
	 */
	public static boolean isMobileAvailable(Context context) {
		NetworkInfo[] nets = getConnectivityManager(context).getAllNetworkInfo();
		if (nets != null) {
			for (NetworkInfo net : nets) {
				if (net.getType() == ConnectivityManager.TYPE_MOBILE) {
					return net.isAvailable();
				}
			}
		}
		return false;
	}

	/**
	 * 设备是否打开移动网络开关。
	 *
	 * @param context 应用程序上下文。
	 * @return 如果移动网络有数据即表示移动网络开启。
	 */
	public static boolean isMobileEnabled(Context context) {
		try {
			Method getMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethod("getMobileDataEnabled");
			getMobileDataEnabledMethod.setAccessible(true);
			return (Boolean) getMobileDataEnabledMethod.invoke(getConnectivityManager(context));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		// 反射失败，默认开启
		return true;
	}

	/**
	 * 判断是否有可用状态的以太网络。
	 *
	 * @param context 应用程序上下文。
	 * @return 返回以太网络是否可用。
	 */
	private static boolean isEthernetAvailable(Context context) {
		NetworkInfo[] nets = getConnectivityManager(context).getAllNetworkInfo();
		if (nets != null) {
			for (NetworkInfo net : nets) {
				if (net.getType() == ConnectivityManager.TYPE_ETHERNET) {
					return net.isAvailable();
				}
			}
		}
		return false;
	}

	/**
	 * 获取 {@link NetworkType} 表示的当前可用的网络连接类型。
	 *
	 * <code>
	 * GPRS    2G(2.5) General Packet Radia Service 114kbps
	 * EDGE    2G(2.75G) Enhanced Data Rate for GSM Evolution 384kbps
	 * UMTS    3G WCDMA 联通3G Universal Mobile Telecommunication System 完整的3G移动通信技术标准
	 * CDMA    2G 电信 Code Division Multiple Access 码分多址
	 * EVDO_0    3G (EVDO 全程 CDMA2000 1xEV-DO) Evolution - Data Only (Data Optimized) 153.6kps - 2.4mbps 属于3G
	 * EVDO_A    3G 1.8mbps - 3.1mbps 属于3G过渡，3.5G
	 * 1xRTT    G CDMA2000 1xRTT (RTT - 无线电传输技术) 144kbps 2G的过渡,
	 * HSDPA    3.5G 高速下行分组接入 3.5G WCDMA High Speed Downlink Packet Access 14.4mbps
	 * HSUPA    3.5G High Speed Uplink Packet Access 高速上行链路分组接入 1.4 - 5.8 mbps
	 * HSPA    3G (分HSDPA,HSUPA) High Speed Packet Access
	 * IDEN    2G Integrated Dispatch Enhanced Networks 集成数字增强型网络 （属于2G，来自维基百科）
	 * EVDO_B    3G EV-DO Rev.B 14.7Mbps 下行 3.5G
	 * LTE    4G Long Term Evolution FDD-LTE 和 TDD-LTE , 3G过渡，升级版 LTE Advanced 才是4G
	 * EHRPD    3G CDMA2000向LTE 4G的中间产物 Evolved High Rate Packet Data HRPD的升级
	 * HSPAP    3G HSPAP 比 HSDPA 快些
	 * </code>
	 *
	 * @param context 应用程序上下文。
	 * @return 返回 {@link NetworkType} 表示的网络连接类型。
	 */
	public static NetworkType getNetworkType(Context context) {
		int type = 0;
		NetworkInfo net = getConnectivityManager(context).getActiveNetworkInfo();
		if (net != null) {
			Logger.d(TAG, "NetworkInfo: " + net.toString());
			type = net.getType();
		}

		switch (type) {
			case ConnectivityManager.TYPE_WIFI:
				return NetworkType.Wifi;
			case ConnectivityManager.TYPE_MOBILE:
			case ConnectivityManager.TYPE_MOBILE_DUN:
			case ConnectivityManager.TYPE_MOBILE_HIPRI:
			case ConnectivityManager.TYPE_MOBILE_MMS:
			case ConnectivityManager.TYPE_MOBILE_SUPL:
				int teleType = getTelephonyManager(context).getNetworkType();
				switch (teleType) {
					case TelephonyManager.NETWORK_TYPE_GPRS:
					case TelephonyManager.NETWORK_TYPE_EDGE:
					case TelephonyManager.NETWORK_TYPE_CDMA:
					case TelephonyManager.NETWORK_TYPE_1xRTT:
					case TelephonyManager.NETWORK_TYPE_IDEN:
						return NetworkType.Mobile2G;
					case TelephonyManager.NETWORK_TYPE_UMTS:
					case TelephonyManager.NETWORK_TYPE_EVDO_0:
					case TelephonyManager.NETWORK_TYPE_EVDO_A:
					case TelephonyManager.NETWORK_TYPE_HSDPA:
					case TelephonyManager.NETWORK_TYPE_HSUPA:
					case TelephonyManager.NETWORK_TYPE_HSPA:
					case TelephonyManager.NETWORK_TYPE_EVDO_B:
					case TelephonyManager.NETWORK_TYPE_EHRPD:
					case TelephonyManager.NETWORK_TYPE_HSPAP:
						return NetworkType.Mobile3G;
					case TelephonyManager.NETWORK_TYPE_LTE:
						return NetworkType.Mobile4G;
					case TelephonyManager.NETWORK_TYPE_NR:
						return NetworkType.Mobile5G;
					default:
						return NetworkType.Unknown;
				}
			default:
				return NetworkType.Unknown;
		}
	}
}
