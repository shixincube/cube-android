/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Shixin Cube Team.
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

package com.shixincube.app.util;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;

/**
 * 设备信息。
 */
public final class DeviceUtils {

    private DeviceUtils() {
    }

    public static String getSystemVersion() {
        return Build.VERSION.RELEASE;
    }

    public static String getSystemModel() {
        return Build.MODEL;
    }

    public static String getSystemDevice() {
        return Build.DEVICE;
    }

    public static String getDeviceBrand() {
        return Build.BRAND;
    }

    public static String getDeviceManufacturer() {
        return Build.MANUFACTURER;
    }

    public static String getDeviceSerial(Context context) {
        String serial = Build.SERIAL;

        if (null == serial || serial.length() == 0 || serial.equalsIgnoreCase("unknown")) {
            serial = Settings.System.getString(context.getContentResolver(), Settings.System.ANDROID_ID);

            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (null != wifiManager && wifiManager.isWifiEnabled()) {
                WifiInfo info = wifiManager.getConnectionInfo();
                if (null != info) {
                    String macAddress = info.getMacAddress();
                    serial = serial + "-" + macAddress.replaceAll(":", "");
                }
            }
        }
        else {
            serial = serial + "-" +  Settings.System.getString(context.getContentResolver(), Settings.System.ANDROID_ID);
        }

        // 进行散列
        serial = HashUtils.makeMD5(serial);

        return serial;
    }

    /**
     * 获取设备描述串。
     *
     * @param context
     * @return
     */
    public static String getDeviceDescription(Context context) {
        StringBuilder builder = new StringBuilder("Android");
        builder.append("/").append(getSystemVersion());
        builder.append("/").append(getSystemModel());
        builder.append(" ").append(getSystemDevice());
        builder.append("/").append(getDeviceBrand());
        builder.append("/").append(getDeviceSerial(context));
        return builder.toString();
    }
}
