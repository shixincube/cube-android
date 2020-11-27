package cube.message.service.model;

import java.io.File;

import cube.message.service.MessageType;

/**
 * 文件消息
 *
 * @author LiuFeng
 * @data 2020/9/3 14:50
 */
public class FileMessage extends Message {
    private File file;

    public FileMessage(File file) {
        super(0, MessageType.File);
        this.file = file;
    }

    public File getFile() {
        return this.file;
    }

    public String getFileName() {
        return file.getName();
    }

    public long getFileSize() {
        return file.length();
    }
}
