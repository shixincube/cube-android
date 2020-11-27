package cube.auth.service;

import org.json.JSONObject;

import cube.common.JSONable;
import cube.utils.GsonUtil;

/**
 * 主访问主机信息。
 *
 * @author LiuFeng
 * @data 2020/10/10 11:07
 */
public class PrimaryDescription implements JSONable {


    /**
     * address : 127.0.0.1
     * primaryContent : {"FileStorage":{"uploadURL":"http://127.0.0.1:9090/api/v1/upload"}}
     */

    private String address;
    private PrimaryContent primaryContent;

    @Override
    public JSONObject toJSON() {
        return GsonUtil.toJSONObject(this);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public PrimaryContent getPrimaryContent() {
        return primaryContent;
    }

    public void setPrimaryContent(PrimaryContent primaryContent) {
        this.primaryContent = primaryContent;
    }

    public static class PrimaryContent {
        /**
         * FileStorage : {"uploadURL":"http://127.0.0.1:9090/api/v1/upload"}
         */

        private FileStorageBean FileStorage;

        public FileStorageBean getFileStorage() {
            return FileStorage;
        }

        public void setFileStorage(FileStorageBean FileStorage) {
            this.FileStorage = FileStorage;
        }

        public static class FileStorageBean {
            /**
             * uploadURL : http://127.0.0.1:9090/api/v1/upload
             */

            private String uploadURL;

            public String getUploadURL() {
                return uploadURL;
            }

            public void setUploadURL(String uploadURL) {
                this.uploadURL = uploadURL;
            }
        }
    }
}
