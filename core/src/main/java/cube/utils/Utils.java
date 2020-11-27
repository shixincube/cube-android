package cube.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import cube.service.model.DeviceInfo;
import cube.utils.log.LogUtil;

public final class Utils {
    private final static SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
    private final static SimpleDateFormat SN_DateFormat = new SimpleDateFormat("ddHHmmss", Locale.CHINA);
    private static int tick = 0;

    private Utils() {
    }

    public static String stringToMD5(String string) {
        byte[] hash;

        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) {
                hex.append("0");
            }
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }

    /*
     * ! 绝对时间转为日期串。 \param timestamp \return
     */
    public static String timestampToDate(long timestamp) {
        Date date = new Date(timestamp);
        return sDateFormat.format(date);
    }

    public static String getSNToDate(long timestamp) {
        Date date = new Date(timestamp);
        return SN_DateFormat.format(date);
    }

    /*
     * ! 返回线程信息。 \return
     */
    public static String getThreadInfo() {
        return "@[name=" + Thread.currentThread().getName() + ", id=" + Thread.currentThread().getId() + "]";
    }

    /**
     * Information about the current build, taken from system properties.
     */
    public static void logDeviceInfo(String tag) {
        LogUtil.d("Android SDK: " + Build.VERSION.SDK_INT + ", " + "Release: " + Build.VERSION.RELEASE + ", " + "Brand: " + Build.BRAND + ", " + "Device: " + Build.DEVICE + ", " + "Id: " + Build.ID + ", " + "Hardware: " + Build.HARDWARE + ", " + "Manufacturer: " + Build.MANUFACTURER + ", " + "Model: " + Build.MODEL + ", " + "Product: " + Build.PRODUCT);
    }

    /*
     * ! 用于帮助校验指定的方法是否在相同的线程里被调用。
     */
    public static class NonThreadSafe {
        private final Long threadId;

        public NonThreadSafe() {
            // Store thread ID of the creating thread.
            this.threadId = Thread.currentThread().getId();
        }

        /* Checks if the method is called on the valid/creating thread. */
        public boolean calledOnValidThread() {
            return this.threadId.equals(Thread.currentThread().getId());
        }
    }

    /**
     * Helper method which throws an exception when an assertion has failed.
     */
    public static void assertIsTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected condition to be true");
        }
    }

    public static String getUserAgent() {
        String userAgent = Build.VERSION.SDK_INT + ", " + Build.VERSION.RELEASE + ", " + Build.BRAND + ", " + Build.DEVICE + ", " + Build.ID + ", " + Build.HARDWARE + ", " + Build.MANUFACTURER + ", " + Build.MODEL + ", " + Build.PRODUCT;
        return userAgent;
    }

    public static DeviceInfo getDevice(Context context) {
        return GsonUtil.toBean(getDeviceInfo(context), DeviceInfo.class);
    }

    public static JSONObject getDeviceInfo(Context context) {
        JSONObject ret = new JSONObject();
        try {
            ret.put("name", Build.MANUFACTURER + "-" + Build.MODEL);
            ret.put("version", Build.VERSION.RELEASE);
            ret.put("platform", (null != Build.MODEL && !"".equals(Build.MODEL) && Build.MODEL.contains("TV")) ? "AndroidTV" : "Android");
            ret.put("userAgent", getUserAgent());
            ret.put("deviceId", Utils.getID(context));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    //    public static boolean isFileMessage(MessageEntity entity) {
    //        return entity != null && entity instanceof FileMessage;
    //    }
    //
    //    public static boolean isFileMessage(MessageType type) {
    //        return type != null && (type == MessageType.File || type == MessageType.Image || type == MessageType.VoiceClip || type == MessageType.VideoClip || type == MessageType.Whiteboard || type == MessageType.WhiteboardClip);
    //    }

    public static String formatDate(long time) {
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTimeInMillis(time);
        return df.format(mCalendar.getTime());
    }

    public static String findAssetsFile(Context context, String key) throws IOException {
        String[] files = context.getAssets().list("");
        if (files != null) {
            for (String path : files) {
                if (path.contains(key)) {
                    return path;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    /**
     * 序列号自动生成器
     *
     * @return
     */
    public synchronized static long createSN() {
        String sn = SpUtil.getMsgToken() + String.format("%02d", tick) + getSNToDate(System.currentTimeMillis());
        tick++;
        if (tick >= 100) {
            tick = 0;
        }
        //Log.i("fldy", "sn:" + sn + " token:" + CubePreferences.getMsgToken());
        return Long.valueOf(sn);
    }

    /**
     * 获取设备唯一标识
     *
     * @return
     */
    @SuppressLint("MissingPermission")
    public static String getUUID(Context context) {
        String tmDevice, tmSerial, androidId;
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        return deviceUuid.toString();
    }

    /**
     * 获取设备唯一标识
     *
     * @return
     */
    public static String getID(Context context) {
        String androidId = null;
        String uuid = SpUtil.getDeviceId();
        if (uuid == null || "".equals(uuid)) {
            try {
                androidId = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            } catch (Exception e) {
            }
            if (androidId == null || "".equals(androidId)) {
                LogUtil.i("fldy", "uu");
                uuid = UUID.randomUUID().toString();
            } else {
                try {
                    uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf-8")).toString();
                } catch (UnsupportedEncodingException e) {
                    LogUtil.i("fldy", "" + e.getMessage());
                    uuid = UUID.randomUUID().toString();
                }
            }
            SpUtil.setDeviceId(uuid);
        }
        return uuid;
    }
}
