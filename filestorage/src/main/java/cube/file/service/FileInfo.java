package cube.file.service;

import java.io.File;
import java.io.Serializable;

import androidx.annotation.NonNull;

/**
 * 文件信息实体
 *
 * @author LiuFeng
 * @data 2020/9/9 14:50
 */
public class FileInfo implements Serializable {
    public String  fileId;                        //文件Id
    public String  identifier;                    //文件标识
    public String  name;                          //文件名称
    public long    progress;                      //进度
    public long    size;                          //文件大小
    public String  type;                          //文件类型
    public String  md5;                           //文件MD5
    public long    createTime;                    //文件创建时间
    public long    expires;                       //文件有效期
    public int     permission;                    //文件可操作权限
    public String  parentId;                      //文件父目录id
    public String  url;                           //文件地址
    public String  path;                          //文件路径
    public File    file;                          //文件

    public FileInfo() {}

    public FileInfo(String identifier, @NonNull File file) {
        this.identifier = identifier;
        if (file.exists()) {
            this.file = file;
            this.name = file.getName();
            this.size = file.length();
            this.path = file.getAbsolutePath();
        }
    }
}
