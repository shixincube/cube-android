package cube.utils;

import cube.utils.log.LogUtil;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


/**
 * Java utils 实现的Zip工具
 */

/**
 * 解压缩工具类
 *
 * @author workerinchina@163.com
 */
public class ZipUtils {
    private static final int BUFF_SIZE = 1024 * 1024; // 1M Byte

    public static void zip(String filepath, String zippath, boolean hasFolder) {
        try {
            File file = new File(filepath);
            File zipFile = new File(zippath);
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File fileSec : files) {
                    if (hasFolder) {
                        recursionZip(zos, fileSec, file.getName() + File.separator);
                    }
                    else {
                        recursionZip(zos, fileSec, "");
                    }
                }
            }
            zos.close();
        } catch (Exception e) {
            LogUtil.e(e);
        }
    }

    private static void recursionZip(ZipOutputStream zos, File file, String baseDir) throws Exception {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File fileSec : files) {
                recursionZip(zos, fileSec, baseDir + file.getName() + File.separator);
            }
        }
        else {
            byte[] buf = new byte[BUFF_SIZE];
            InputStream input = new FileInputStream(file);
            zos.putNextEntry(new ZipEntry(baseDir + file.getName()));
            int len;
            while ((len = input.read(buf)) != -1) {
                zos.write(buf, 0, len);
            }
            input.close();
        }
    }

    public static void unzip(File targetDir, File zipFile) throws ZipException, IOException {
//        ZipFile zip = new ZipFile(zipFile);
//        @SuppressWarnings("unchecked")
//        Enumeration<ZipEntry> z = (Enumeration<ZipEntry>) zip.entries();
//        while (z.hasMoreElements()) {
//            ZipEntry entry = z.nextElement();
//            File f = new File(targetDir, entry.getName());
//            if (f.isDirectory()) {
//                if (!f.exists()) {
//                    f.mkdirs();
//                }
//            }
//            else {
//                f.getParentFile().mkdirs();
//                InputStream in = zip.getInputStream(entry);
//                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
//                IOUtils.copy(in, out);
//                in.close();
//                out.flush();
//                out.close();
//            }
//        }
//        zip.close();
    }

    public static void unZipFile(File zipFile, String folderPath) throws ZipException, IOException {
        File desDir = new File(folderPath);
        if (!desDir.exists()) {
            desDir.mkdirs();
        }
        ZipFile zf = new ZipFile(zipFile);
        for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements(); ) {
            ZipEntry entry = ((ZipEntry) entries.nextElement());
            InputStream in = zf.getInputStream(entry);
            String str = folderPath + File.separator + entry.getName();
            // str = new String(str.getBytes("GB2312"), "UTF-8");
            File desFile = new File(str);
            if (entry.isDirectory()) {
                if (!desFile.exists()) {
                    desFile.mkdirs();
                }
                continue;
            }
            if (!desFile.exists()) {
                File fileParentDir = desFile.getParentFile();
                if (!fileParentDir.exists()) {
                    fileParentDir.mkdirs();
                }
                desFile.createNewFile();
            }
            OutputStream out = new FileOutputStream(desFile);
            byte buffer[] = new byte[BUFF_SIZE];
            int realLength;
            while ((realLength = in.read(buffer)) > 0) {
                out.write(buffer, 0, realLength);
            }
            in.close();
            out.close();
        }
        zf.close();
    }

    /**
     * 使用文件内存映射实现
     *
     * @throws IOException
     */
    public static void fileCopy(File source, File target) throws IOException {
        FileInputStream fis = new FileInputStream(source);
        RandomAccessFile faf = new RandomAccessFile(target, "rw");
        FileChannel fcin = fis.getChannel();
        FileChannel fcout = faf.getChannel();
        MappedByteBuffer mbbi = fcin.map(FileChannel.MapMode.READ_ONLY, 0, fcin.size());
        MappedByteBuffer mbbo = fcout.map(FileChannel.MapMode.READ_WRITE, 0, fcin.size());
        mbbo.put(mbbi);
        mbbi.clear();
        mbbo.clear();
        fis.close();
        faf.close();
    }

    /**
     * 使用文件内存映射实现
     *
     * @throws IOException
     */
    public static void fileCopy(String source, String target) throws IOException {
        FileInputStream fis = new FileInputStream(source);
        RandomAccessFile faf = new RandomAccessFile(target, "rw");
        FileChannel fcin = fis.getChannel();
        FileChannel fcout = faf.getChannel();
        MappedByteBuffer mbbi = fcin.map(FileChannel.MapMode.READ_ONLY, 0, fcin.size());
        MappedByteBuffer mbbo = fcout.map(FileChannel.MapMode.READ_WRITE, 0, fcin.size());
        mbbo.put(mbbi);
        mbbi.clear();
        mbbo.clear();
        fis.close();
        faf.close();
    }

    /**
     * 将文本信息写入文件
     *
     * @param text
     * @param file
     *
     * @throws IOException
     */
    public static void writeToFile(String text, File file) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        out.write(text);
        out.close();
    }

    /**
     * 读取文件信息
     *
     * @param file
     *
     * @return
     *
     * @throws IOException
     */
    public static String readFromFile(File file) throws IOException {
        StringBuffer buffer = new StringBuffer();
        BufferedReader in = new BufferedReader(new FileReader(file));
        String text = in.readLine();
        while (text != null) {
            buffer.append(text);
            text = in.readLine();
        }
        in.close();
        return buffer.toString();
    }
}
