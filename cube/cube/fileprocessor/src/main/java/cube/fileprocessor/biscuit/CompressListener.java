package cube.fileprocessor.biscuit;

/**
 * 压缩监听器。
 */
public interface CompressListener {

    void onSuccess(String compressedPath);

    void onError(CompressException exception);
}
