package cube.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import cube.utils.log.LogUtil;


public class StreamUtils {
    /**
     * 序列化
     *
     * @param list
     * @param file
     * @param <T>
     *
     * @return
     */
    public static <T> boolean writeObject(List<T> list, File file) {
        T[] array = (T[]) list.toArray();
        ObjectOutputStream out = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(array);
            out.flush();
            return true;
        } catch (IOException e) {
            LogUtil.e(e);
            return false;
        }
    }

    /**
     * 反序列化
     *
     * @param file
     * @param <E>
     *
     * @return
     */
    public static <E> List<E> readObjectForList(File file) {
        E[] object;
        ObjectInputStream out = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            out = new ObjectInputStream(new FileInputStream(file));
            object = (E[]) out.readObject();
            return new LinkedList<E>(Arrays.asList(object));
        } catch (IOException e) {
            LogUtil.e(e);
        } catch (ClassNotFoundException e) {
            LogUtil.e(e);
        }
        return new ArrayList<E>();
    }
}
