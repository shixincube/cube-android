package cube.api;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import androidx.annotation.NonNull;
import cube.utils.SpUtil;
import cube.utils.log.LogUtil;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * api网络请求管理器
 *
 * @author LiuFeng
 * @data 2020/8/17 16:09
 */
public class ApiManager {
    private static final String TAG = "ApiManager";

    private static final int READ_TIME_OUT = 15;   // 读取超时时间为15秒
    private static final int CONNECT_TIME_OUT = 15;   // 连接超时时间为15秒

    private OkHttpClient mOkHttpClient;  // OkHttpClient实例

    private Map<Class<?>, Object> apiService = new ConcurrentHashMap<>();

    /**
     * 单例
     */
    public static ApiManager getInstance() {
        return ApiManagerHolder.mInstance;
    }

    private static class ApiManagerHolder {
        private static ApiManager mInstance = new ApiManager();
    }

    /**
     * 构造方法
     */
    private ApiManager() {
        this.initOkHttp();
    }

    /**
     * 初始化OkHttp
     */
    private void initOkHttp() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // 打印请求log日志
        if (SpUtil.isDebug()) {
            builder.addInterceptor(getLogInterceptor());
        }
        builder.addInterceptor(getBaseUrlInterceptor());            // BaseUrl拦截替换
        builder.connectTimeout(CONNECT_TIME_OUT, TimeUnit.SECONDS); // 设置连接超时
        builder.readTimeout(READ_TIME_OUT, TimeUnit.SECONDS);       // 设置读取超时
        builder.writeTimeout(READ_TIME_OUT, TimeUnit.SECONDS);      // 设置写入超时
        builder.retryOnConnectionFailure(true);                     // 设置重连
        this.setSSL(builder);
        this.mOkHttpClient = builder.build();
    }

    /**
     * 日志拦截
     *
     * @return
     */
    private Interceptor getLogInterceptor() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(@NonNull String message) {
                LogUtil.i("OkHttp:", message);
            }
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return loggingInterceptor;
    }

    /**
     * BaseUrl拦截处理
     *
     * @return
     */
    private Interceptor getBaseUrlInterceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(@NonNull Chain chain) throws IOException {
                Request request = chain.request();
                return chain.proceed(request);
            }
        };
    }

    /**
     * 获取ApiService
     * 备注：支持多api的调用
     *
     * @param clazz
     * @param baseUrl
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getApiService(Class<T> clazz, String baseUrl) {
        if (!apiService.containsKey(clazz)) {
            synchronized (this) {
                if (!apiService.containsKey(clazz)) {
                    Retrofit retrofit = getRetrofit(baseUrl);
                    T service = retrofit.create(clazz);
                    apiService.put(clazz, service);
                    return service;
                }
            }
        }

        return (T) apiService.get(clazz);
    }

    /**
     * 初始化获取Retrofit
     */
    private Retrofit getRetrofit(String baseUrl) {
        Retrofit.Builder builder = new Retrofit.Builder();
        // base地址
        builder.baseUrl(baseUrl);
        builder.client(this.mOkHttpClient);
        builder.addConverterFactory(GsonConverterFactory.create());
        return builder.build();
    }

    /**
     * 设置忽略ssl证书验证
     *
     * @param builder
     */
    private void setSSL(OkHttpClient.Builder builder) {
        try {
            X509TrustManager xtm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{xtm}, new SecureRandom());

            HostnameVerifier doNotVerify = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            builder.sslSocketFactory(sslContext.getSocketFactory(), xtm);
            builder.hostnameVerifier(doNotVerify);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LogUtil.e(TAG, e);
        }
    }
}
