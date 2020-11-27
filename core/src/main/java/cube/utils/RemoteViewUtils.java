package cube.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import cube.utils.log.LogUtil;


/**
 * @author Wangxx
 * @date 2017/3/29
 */

public class RemoteViewUtils {
    /**
     * 字符串的解压
     *
     * @param bytes
     *
     * @return 返回解压后的字符串
     *
     * @throws IOException
     */
    public static byte[] uncompress(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            GZIPInputStream gzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = gzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (IOException e) {
            LogUtil.e("gzip uncompress error.", e);
        }

        return out.toByteArray();
    }

    private static Bitmap stringToBitmap(byte[] bitmapArray) {
        // 将字符串转换成Bitmap类型
        return BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
    }

    public static Bitmap unGzip(byte[] data) {
        byte[] unCompress = uncompress(data);
        int width = (unCompress[0] & 0xff)  | (unCompress[1] & 0xff << 8);
        int height = (unCompress[2] & 0xff) | (unCompress[3] & 0xff << 8);

        int dataSize = width * height * 4;


        if (width < 1000 && width > 0 && height > 0 && height < 1000) {
            byte[] bytes = new byte[dataSize];

            //LZ4解压
//            LZ4Factory factory = LZ4Factory.fastestInstance();
//            LZ4FastDecompressor decompresser = factory.fastDecompressor();
//            int decompressLen = decompresser.decompress(unCompress, 4, bytes, 0, dataSize);

            return rgb2Bitmap(bytes, width, height);
        }
        return null;
    }

    /**
     * @方法描述 将RGB字节数组转换成Bitmap，
     */
    static public Bitmap rgb2Bitmap(byte[] data, int width, int height) {
        int[] colors = convertByteToColor(data);    //取RGB值转换为int数组
        if (colors == null) {
            return null;
        }

        Bitmap bmp = Bitmap.createBitmap(colors, 0, width, width, height,
                Bitmap.Config.ARGB_8888);
        return bmp;
    }

    /**
     * 将RGBA数组转化为像素数组
     */
    private static int[] convertByteToColor(byte[] data){
        int size = data.length;
        if (size == 0){
            return null;
        }

        int []color = new int[size/4];


        for(int i = 0; i < color.length; i++){

            //                color[i] = (data[i * 4] << 24 & 0xFF000000) |
            //                        (data[i * 4 + 1] << 16 & 0x00FF0000) |
            //                        (data[i * 4 + 2] << 8 & 0x0000FF00 ) |
            //                        (data[i * 4 + 3] & 0x000000FF );

            color[i] = (data[i * 4] << 16 & 0x00FF0000) |
                    (data[i * 4 + 1] << 8 & 0x0000FF00) |
                    (data[i * 4 + 2] & 0x000000FF) |
                    (data[i * 4 + 3] << 24 & 0xFF000000);
        }


        return color;
    }

    public static byte[] getByteString(byte[] data) {
        int zeroIndex = data.length;
        for (int i = data.length - 1; i > -1; i--) {
            if (data[i] == 0) {
                zeroIndex = i;
            }
            else {
                break;
            }
        }

        byte[] ret = new byte[zeroIndex];
        System.arraycopy(data, 0, ret, 0, ret.length);
        return ret;
    }
}
