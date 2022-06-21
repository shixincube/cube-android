package cube.fileprocessor.biscuit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 */
public class Utils {

    private final static String DEFAULT_IMAGE_CACHE_PATH = "cube_cache";

    static ArrayList<String> format = new ArrayList<>();
    static String JPG = ".jpg";
    static String JPEG = ".jpeg";
    static String PNG = ".png";
    static String WebP = ".webp";
    static String GIF = ".gif";

    static float REFERENCE_WIDTH = 1080f;
    static float SCALE_REFERENCE_WIDTH = 1280f;
    static float LIMITED_WIDTH = 1000f;
    static float MIN_WIDTH = 640f;

    static int DEFAULT_QUALITY = 80;
    static int DEFAULT_LOW_QUALITY = 70;
    static int DEFAULT_HEIGHT_QUALITY = 88;
    static int DEFAULT_X_HEIGHT_QUALITY = 92;
    static int DEFAULT_XX_HEIGHT_QUALITY = 99;

    static boolean loggingEnabled = true;

    static {
        format.add(JPG);
        format.add(JPEG);
        format.add(PNG);
        format.add(WebP);
        format.add(GIF);
    }

    public static boolean isImage(String imgPath) {
        if (TextUtils.isEmpty(imgPath)) return false;
        int begin = imgPath.lastIndexOf(".");
        int end = imgPath.length();
        if (begin == -1) return false;
        String imageType = imgPath.substring(begin, end);
        return format.contains(imageType.toLowerCase());
    }

    // default cache dir
    public static String getCacheDir(Context context) {
        File cacheDir = new File(context.getExternalCacheDir(), DEFAULT_IMAGE_CACHE_PATH);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        return cacheDir.getAbsolutePath();
    }

    // delete all cache image
    public static void clearCache(Context context) {
        clearCache(getCacheDir(context));
    }

    public static void clearCache(String dir) {
        File file = new File(dir);
        File[] files = file.listFiles();
        if (files.length > 0) {
            for (File f : files) {
                f.delete();
            }
        }
    }

    /**
     * Resolve some phone when take photo rotate some degree
     *
     * @param path
     * @return
     */
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    // rotate to correct angle
    public static Bitmap rotateBitmap(Bitmap bitmap, int degree) {
        if (bitmap != null) {
            Matrix m = new Matrix();
            m.postRotate(degree);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), m, true);
            return bitmap;
        }
        return bitmap;
    }

    static void log(String tag, String msg) {
        if (loggingEnabled) {
            Log.d(tag, msg);
        }
    }

    /**
     * if the quality not set by user, will set a default value base on device's density.
     *
     * @param context
     * @return
     */
    static int getDefaultQuality(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
        float density = dm.density;
        if (density > 3f) {
            return DEFAULT_LOW_QUALITY;
        } else if (density > 2.5f && density <= 3f) {
            return DEFAULT_QUALITY;
        } else if (density > 2f && density <= 2.5f) {
            return DEFAULT_HEIGHT_QUALITY;
        } else if (density > 1.5f && density <= 2f) {
            return DEFAULT_X_HEIGHT_QUALITY;
        } else {
            return DEFAULT_XX_HEIGHT_QUALITY;
        }
    }

    /**
     * Copies one file into the other with the given paths.
     * In the event that the paths are the same, trying to copy one file to the other
     * will cause both files to become null.
     * Simply skipping this step if the paths are identical.
     */
    public static void copyFile(@NonNull String pathFrom, @NonNull String pathTo) throws IOException {
        if (pathFrom.equalsIgnoreCase(pathTo)) {
            return;
        }
        FileChannel outputChannel = null;
        FileChannel inputChannel = null;
        try {
            inputChannel = new FileInputStream(new File(pathFrom)).getChannel();
            outputChannel = new FileOutputStream(new File(pathTo)).getChannel();
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            inputChannel.close();
        } finally {
            if (inputChannel != null) inputChannel.close();
            if (outputChannel != null) outputChannel.close();
        }
    }

    public static void saveExif(String oldFilePath, String newFilePath) throws Exception {
        ExifInterface oldExif=new ExifInterface(oldFilePath);
        ExifInterface newExif=new ExifInterface(newFilePath);
        Class<ExifInterface> cls = ExifInterface.class;
        Field[] fields = cls.getFields();
        for (int i = 0; i < fields.length; i++) {
            String fieldName = fields[i].getName();
            if (!TextUtils.isEmpty(fieldName) && fieldName.startsWith("TAG")) {
                String fieldValue = fields[i].get(cls).toString();
                String attribute = oldExif.getAttribute(fieldValue);
                if (attribute != null) {
                    newExif.setAttribute(fieldValue, attribute);
                }
            }
        }
        newExif.saveAttributes();
    }

}
