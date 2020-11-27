package cube.utils;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;

public class DesUtil {
    public static String name     = "cube.impl.license";
    static        String original = "file:///android_assets/" + name;
    static        String target   = "/Users/workerinchina@163.com/cube.impl.license";
    static        String new_dest = "/Users/workerinchina@163.com/abcnew.txt";

    /**
     * <p>
     * DES解密文件
     *
     * @param file 需要解密的文件
     * @param dest 解密后的文件
     *
     * @throws Exception
     */
    public static String decrypt(Context context, String dest) throws Exception {
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.DECRYPT_MODE, getKey());
        InputStream is = context.getAssets().open(name);
        OutputStream out = new FileOutputStream(dest);
        CipherOutputStream cos = new CipherOutputStream(out, cipher);
        byte[] buffer = new byte[1024];
        int r;
        while ((r = is.read(buffer)) >= 0) {
            cos.write(buffer, 0, r);
        }
        cos.close();
        out.close();
        is.close();
        return dest;
    }

    /**
     * <p>
     * DES解密文件
     *
     * @param file 需要解密的文件
     * @param dest 解密后的文件
     *
     * @throws Exception
     */
    public static void decrypt(String file, String dest) throws Exception {
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.DECRYPT_MODE, getKey());
        InputStream is = new FileInputStream(file);
        OutputStream out = new FileOutputStream(dest);
        CipherOutputStream cos = new CipherOutputStream(out, cipher);
        byte[] buffer = new byte[1024];
        int r;
        while ((r = is.read(buffer)) >= 0) {
            cos.write(buffer, 0, r);
        }
        cos.close();
        out.close();
        is.close();
    }

    /**
     * <p>
     * DES加密文件
     *
     * @param file     源文件
     * @param destFile 加密后的文件
     *
     * @throws Exception
     */
    public static void encrypt(String file, String destFile) throws Exception {
        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.ENCRYPT_MODE, getKey());
        InputStream is = new FileInputStream(file);
        OutputStream out = new FileOutputStream(destFile);
        CipherInputStream cis = new CipherInputStream(is, cipher);
        byte[] buffer = new byte[1024];
        int r;
        while ((r = cis.read(buffer)) > 0) {
            out.write(buffer, 0, r);
        }
        cis.close();
        is.close();
        out.close();
    }

    // private static Key getKey() {
    // Key kp = null;
    // try {
    // String fileName = keyfileName;
    // InputStream is = new FileInputStream(fileName);
    // ObjectInputStream oos = new ObjectInputStream(is);
    // kp = (Key)"123456";//(Key) oos.readObject();
    // oos.close();
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // return kp;
    // }

    public static Key getKey() {
        String keyRule = "cubeengine";
        Key kp = null;
        byte[] keyByte = keyRule.getBytes();
        // 创建一个空的八位数组,默认情况下为0
        byte[] byteTemp = new byte[8];
        // 将用户指定的规则转换成八位数组
        for (int i = 0; i < byteTemp.length && i < keyByte.length; i++) {
            byteTemp[i] = keyByte[i];
        }
        kp = new SecretKeySpec(byteTemp, "DES");
        return kp;
    }

    public static void main(String[] args) throws Exception {
        //		DesUtil.saveDesKey();
        DesUtil.encrypt(original, target);
        DesUtil.decrypt(target, new_dest);
        System.out.println("DES说");
        // desinput.txt 经过加密和解密后生成的 desinput2.txt 应该与源文件一样
    }

    /**
     * <p>
     * 生成KEY，并保存
     */
    //	public static void saveDesKey() {
    //		try {
    //			SecureRandom sr = new SecureRandom();
    //			// 为我们选择的DES算法生成一个KeyGenerator对象
    //			KeyGenerator kg = KeyGenerator.getInstance("DES");
    //			kg.init(sr);
    //			FileOutputStream fos = new FileOutputStream(keyfileName);
    //			ObjectOutputStream oos = new ObjectOutputStream(fos);
    //			// 生成密钥
    //			Key key = kg.generateKey();
    //			oos.writeObject(key);
    //			oos.close();
    //		} catch (Exception e) {
    //			e.printStackTrace();
    //		}
    //	}
}