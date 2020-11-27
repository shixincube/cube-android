package cube.whiteboard.api;

import java.util.Map;

import cube.api.data.BaseData;
import cube.api.data.ResultData;
import cube.whiteboard.api.data.FileConvertData;
import cube.whiteboard.api.data.FilePageData;
import cube.whiteboard.api.data.HistoryData;
import cube.whiteboard.api.data.QueryFileData;
import cube.whiteboard.api.data.UpdateFileData;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Url;

/**
 * 白板api请求数据服务
 *
 * @author LiuFeng
 * @data 2020/8/17 15:49
 */
public interface WBApiService {

    /**
     * 查询历史数据
     *
     * @param params
     * @return
     */
    @FormUrlEncoded
    @POST("/dgbd/history/by_roomid")
    Call<ResultData<HistoryData>> queryHistory(@FieldMap Map<String, String> params);

    /**
     * 查询文件列表
     *
     * @param params
     * @return
     */
    @FormUrlEncoded
    @POST("/file/list")
    Call<ResultData<QueryFileData>> queryFiles(@FieldMap Map<String, String> params);

    /**
     * 查询文件页数
     *
     * @param params
     * @return
     */
    @FormUrlEncoded
    @POST("/file/page/size")
    Call<ResultData<FilePageData>> queryFilePageSize(@FieldMap Map<String, String> params);

    /**
     * 查询文件转图片信息
     *
     * @param params
     * @return
     */
    @FormUrlEncoded
    @POST("/file/convert/image/info")
    Call<ResultData<FileConvertData>> queryFileConvertImageInfo(@FieldMap Map<String, String> params);

    /**
     * 上传文件
     *
     * @param params
     * @param file
     * @return
     */
    @Multipart
    @POST("/file/upload")
    Call<ResultData<UpdateFileData>> uploadFile(@PartMap Map<String, String> params, @Part MultipartBody.Part file);
}
