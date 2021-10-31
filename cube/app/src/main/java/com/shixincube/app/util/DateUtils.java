/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
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

import java.util.Calendar;
import java.util.Date;

/**
 * 日期实用函数。
 */
public class DateUtils {

    private DateUtils() {
    }

    public static String formatConversationTime(long timestamp) {
        return DateUtils.formatConversationTime(new Date(timestamp));
    }

    public static String formatConversationTime(Date date) {
        Calendar now = Calendar.getInstance();
        Calendar time = Calendar.getInstance();
        time.setTime(date);

        int days = Math.abs(daysBetween(now, time));
        if (days < 1) {
            // 今天
            return formatHourMinute(time);
        }
        else if (days == 1) {
            // 昨天
            return "昨天 " + formatToday(time);
        }
        else if (days <= 7) {
            // 星期
            int week = getDayOfWeek(time);
            return "星期" + formatChineseWeek(week) + " " + formatHourMinute(time);
        }
        else {
            return (time.get(Calendar.MONTH) + 1) + "月" + time.get(Calendar.DAY_OF_MONTH) + "日 "
                    + formatHourMinute(time);
        }
    }

    public static int daysBetween(Calendar calendar1, Calendar calendar2) {
        int d1 = calendar1.get(Calendar.DAY_OF_YEAR);
        int d2 = calendar2.get(Calendar.DAY_OF_YEAR);
        return d1 - d2;
    }

    public static String formatToday(Calendar time) {
        int hourOfDay = time.get(Calendar.HOUR_OF_DAY);

        String when = null;
        if (hourOfDay >= 18) {
            when = "晚间";
        }
        else if (hourOfDay >= 12) {
            when = "下午";
        }
        else if (hourOfDay >= 8) {
            when = "上午";
        }
        else if (hourOfDay >= 5) {
            when = "早间";
        }
        else {
            when = "凌晨";
        }

        return when + " " + time.get(Calendar.HOUR) + ":" + time.get(Calendar.MINUTE);
    }

    private static String formatHourMinute(Calendar time) {
        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);
        String strHour = hour <= 9 ? "0" : "";
        String strMinute = minute <= 9 ? "0" : "";
        return strHour + hour + ":" + strMinute + minute;
    }

    public static int getDayOfWeek(Calendar time) {
        boolean firstSunday = time.getFirstDayOfWeek() == Calendar.SUNDAY;
        int week = time.get(Calendar.DAY_OF_WEEK);
        if (firstSunday) {
            week = week - 1;
            if (week == 0) {
                week = 7;
            }
        }
        return week;
    }

    private static String formatChineseWeek(int number) {
        switch (number) {
            case 1:
                return "一";
            case 2:
                return "二";
            case 3:
                return "三";
            case 4:
                return "四";
            case 5:
                return "五";
            case 6:
                return "六";
            case 7:
                return "日";
            default:
                return "";
        }
    }
}
