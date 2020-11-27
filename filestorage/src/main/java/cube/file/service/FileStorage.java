package cube.file.service;

import cube.common.callback.CubeCallback1;

/**
 * 文件存储接口
 *
 * @author LiuFeng
 * @data 2020/9/8 17:22
 */
public interface FileStorage {

    /**
     * 添加文件监听
     *
     * @param listener
     */
    void addListener(FileStorageListener listener);

    /**
     * 删除文件监听
     *
     * @param listener
     */
    void removeListener(FileStorageListener listener);

    /**
     * 上传文件
     *
     * @param fileInfo
     */
    void upload(FileInfo fileInfo, CubeCallback1<FileInfo> callback);

    /**
     * 暂停上传文件
     *
     * @param fileInfo
     */
    void pauseUpload(FileInfo fileInfo, CubeCallback1<FileInfo> callback);

    /**
     * 恢复上传文件
     *
     * @param fileInfo
     */
    void resumeUpload(FileInfo fileInfo, CubeCallback1<FileInfo> callback);

    /**
     * 取消上传
     *
     * @param fileInfo
     */
    void cancelUpload(FileInfo fileInfo, CubeCallback1<FileInfo> callback);

    /**
     * 下载文件
     *
     * @param fileInfo
     */
    void download(FileInfo fileInfo, CubeCallback1<FileInfo> callback);

    /**
     * 暂停下载文件
     *
     * @param fileInfo
     */
    void pauseDownload(FileInfo fileInfo, CubeCallback1<FileInfo> callback);

    /**
     * 恢复下载文件
     *
     * @param fileInfo
     */
    void resumeDownload(FileInfo fileInfo, CubeCallback1<FileInfo> callback);

    /**
     * 取消下载
     *
     * @param fileInfo
     */
    void cancelDownload(FileInfo fileInfo, CubeCallback1<FileInfo> callback);
}
