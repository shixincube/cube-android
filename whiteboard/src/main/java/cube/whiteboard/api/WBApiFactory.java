package cube.whiteboard.api;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import cube.api.ApiManager;
import cube.api.data.BaseData;
import cube.api.data.ResultData;
import cube.common.callback.CubeCallback1;
import cube.utils.SpUtil;
import cube.utils.log.LogUtil;
import cube.whiteboard.api.data.FileConvertData;
import cube.whiteboard.api.data.FilePageData;
import cube.whiteboard.api.data.HistoryData;
import cube.whiteboard.api.data.QueryFileData;
import cube.whiteboard.api.data.UpdateFileData;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 白板api工厂类
 *
 * @author LiuFeng
 * @data 2020/8/17 16:16
 */
public class WBApiFactory {
    private static WBApiFactory instance = new WBApiFactory();

    private WBApiService apiService;

    private WBApiFactory() {
        String baseUrl = WBApiConfig.getBaseUrl();
        apiService = ApiManager.getInstance().getApiService(WBApiService.class, baseUrl);
    }

    /**
     * api工厂单例
     *
     * @return
     */
    public static WBApiFactory getInstance() {
        return instance;
    }

    /**
     * 查询白板的文件列表
     *
     * @param roomId
     * @param token
     * @param callback
     */
    public void queryFiles(String roomId, String token, CubeCallback1<QueryFileData> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("roomId", roomId);
        params.put("token", token);
        params.put("uid", SpUtil.getCubeId());
        apiService.queryFiles(params).enqueue(convertCallback(callback));
    }

    /**
     * 查询历史数据
     *
     * @param roomId
     * @param token
     * @param callback
     */
    public void queryHistory(String roomId, String token, CubeCallback1<HistoryData> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("roomId", roomId);
        params.put("token", token);
        params.put("uid", SpUtil.getCubeId());
        apiService.queryHistory(params).enqueue(convertCallback(callback));
    }

    /**
     * 查询白板的文件列表
     *
     * @param roomId
     * @param fileId
     * @param token
     * @param callback
     */
    public void queryFilePageSize(String roomId, String fileId, String token, CubeCallback1<FilePageData> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("roomId", roomId);
        params.put("fid", fileId);
        params.put("token", token);
        apiService.queryFilePageSize(params).enqueue(convertCallback(callback));
    }

    /**
     * 查询白板的文件转图片信息
     *
     * @param roomId
     * @param fileId
     * @param page
     * @param token
     * @param callback
     */
    public void queryFileConvertImageInfo(String roomId, String fileId, int page, String token, CubeCallback1<FileConvertData> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("roomId", roomId);
        params.put("fid", fileId);
        params.put("index", String.valueOf(page));
        params.put("token", token);
        params.put("format", "jpg");
        apiService.queryFileConvertImageInfo(params).enqueue(convertCallback(callback));
    }

    /**
     * 上传文件到白板中
     *
     * @param roomId
     * @param token
     * @param file
     * @param callback
     */
    public void uploadFile(String roomId, String token, File file, CubeCallback1<UpdateFileData> callback) {
        String fileName = file.getName();
        Map<String, String> params = new HashMap<>();
        params.put("roomId", roomId);
        params.put("token", token);
        params.put("fileName", fileName);
        params.put("uid", SpUtil.getCubeId());

        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", encode(fileName), requestFile);
        apiService.uploadFile(params, body).enqueue(convertCallback(callback));
    }

    /**
     * 对文件名进行encode编码
     * 备注：Okhttp请求头不支持中文，所以需要编码转换
     *
     * @param fileName
     * @return
     */
    private String encode(String fileName) {
        try {
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LogUtil.e("encode", e);
        }

        return fileName;
    }

    /**
     * 封装Callback转换回调
     *
     * @param callback
     * @param <T>
     * @return
     */
    private <T> Callback<ResultData<T>> convertCallback(CubeCallback1<T> callback) {
        return new Callback<ResultData<T>>() {
            @Override
            public void onResponse(@NonNull Call<ResultData<T>> call, @NonNull Response<ResultData<T>> response) {
                int responseCode = response.code();
                if (responseCode != 200 || response.body() == null) {
                    String desc = null;
                    try {
                        if (response.errorBody() != null) {
                            desc = response.errorBody().string();
                        }
                    } catch (IOException ignored) {
                    }
                    callback.onError(responseCode, desc);
                    return;
                }

                ResultData<T> resultData = response.body();
                if (resultData.code != 200) {
                    callback.onError(resultData.code, resultData.desc);
                    return;
                }

                callback.onSuccess(resultData.data);
            }

            @Override
            public void onFailure(@NonNull Call<ResultData<T>> call, @NonNull Throwable t) {
                callback.onError(400, t.getMessage());
            }
        };
    }
}
