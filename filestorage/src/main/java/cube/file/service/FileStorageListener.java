package cube.file.service;

/**
 * 文件存储监听
 *
 * @author LiuFeng
 * @data 2020/9/9 15:04
 */
public interface FileStorageListener {
    /**
     * 上传中
     *
     * @param file
     * @param progress
     * @param total
     */
    void onUploading(FileInfo file, long progress, long total);

    /**
     * 上传暂停
     *
     * @param file
     * @param progress
     * @param total
     */
    void onUploadPaused(FileInfo file, long progress, long total);

    /**
     * 上传恢复
     *
     * @param file
     * @param progress
     * @param total
     */
    void onUploadResumed(FileInfo file, long progress, long total);

    /**
     * 上传取消
     *
     * @param file
     */
    void onUploadCanceled(FileInfo file);

    /**
     * 上传完成
     *
     * @param file
     */
    void onUploadCompleted(FileInfo file);

    /**
     * 下载中
     *
     * @param file
     * @param progress
     * @param total
     */
    void onDownloading(FileInfo file, long progress, long total);

    /**
     * 下载暂停
     *
     * @param file
     * @param progress
     * @param total
     */
    void onDownloadPaused(FileInfo file, long progress, long total);

    /**
     * 下载恢复
     *
     * @param file
     * @param progress
     * @param total
     */
    void onDownloadResumed(FileInfo file, long progress, long total);

    /**
     * 下载取消
     *
     * @param file
     */
    void onDownloadCanceled(FileInfo file);

    /**
     * 下载完成
     *
     * @param file
     */
    void onDownloadCompleted(FileInfo file);

    /**
     * 文件操作错误
     *
     * @param file
     * @param code
     * @param desc
     */
    void onError(FileInfo file, int code, String desc);
}
