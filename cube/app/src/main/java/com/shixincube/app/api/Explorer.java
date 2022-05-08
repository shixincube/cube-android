/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.shixincube.app.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shixincube.app.CubeApp;
import com.shixincube.app.manager.AccountHelper;
import com.shixincube.app.model.request.CheckPhoneRequest;
import com.shixincube.app.model.request.LoginRequest;
import com.shixincube.app.model.request.RegisterRequest;
import com.shixincube.app.model.request.SetAccountInfoRequest;
import com.shixincube.app.model.response.AccountInfoResponse;
import com.shixincube.app.model.response.CheckPhoneResponse;
import com.shixincube.app.model.response.ContactZoneResponse;
import com.shixincube.app.model.response.LoginResponse;
import com.shixincube.app.model.response.RegisterResponse;
import com.shixincube.app.model.response.SearchAccountResultResponse;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import cell.util.NetworkUtils;
import cube.util.LogUtils;
import io.reactivex.rxjava3.core.Observable;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * API Explorer
 */
public class Explorer {

    private static Explorer instance;

    public final static String HOST_URL = "http://10.0.2.2:7777/";

    private AppInterface api;

    private Explorer() {
        Gson gson = new GsonBuilder().setLenient().create();

        this.api = new Retrofit.Builder()
                .baseUrl(HOST_URL)
                .client(getClient())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build()
                .create(AppInterface.class);
    }

    public final static Explorer getInstance() {
        if (null == Explorer.instance) {
            Explorer.instance = new Explorer();
        }
        return Explorer.instance;
    }

    public AppInterface getAppInterface() {
        return this.api;
    }

    /**
     * 校验手机号码是否有效。
     *
     * @param regionCode
     * @param phoneNumber
     * @return
     */
    public Observable<CheckPhoneResponse> checkPhoneAvailable(String regionCode, String phoneNumber, boolean verificationCodeRequired) {
        return this.api.checkPhoneAvailable(getRequestBody(new CheckPhoneRequest(regionCode, phoneNumber, verificationCodeRequired)));
    }

    /**
     * 注册账号。
     *
     * @param phoneNumber
     * @param password
     * @param nickname
     * @param verificationCode
     * @return
     */
    public Observable<RegisterResponse> registerAccount(String phoneNumber, String password, String nickname, String verificationCode) {
        return this.api.register(getRequestBody(new RegisterRequest(phoneNumber, password, nickname, verificationCode)));
    }

    /**
     * 账号登录。
     *
     * @param phoneNumber
     * @param password
     * @param device
     * @return
     */
    public Observable<LoginResponse> login(String phoneNumber, String password, String device) {
        return this.api.login(getRequestBody(new LoginRequest(phoneNumber, password, device)));
    }

    /**
     * 账号登录。
     *
     * @param token
     * @param device
     * @return
     */
    public Observable<LoginResponse> login(String token, String device) {
        return this.api.login(getRequestBody(new LoginRequest(token, device)));
    }

    /**
     * 获取自己的账号信息。
     *
     * @param token
     * @return
     */
    public Observable<AccountInfoResponse> getAccountInfo(String token) {
        return this.api.getAccountInfo(token);
    }

    /**
     * 查询指定联系人的账号。
     *
     * @param id
     * @param token
     * @return
     */
    public Observable<AccountInfoResponse> getAccountInfo(Long id, String token) {
        return this.api.getAccountInfo(id, token);
    }

    /**
     * 设置账号信息。
     *
     * @param token
     * @param name
     * @param avatar
     * @return
     */
    public Observable<AccountInfoResponse> setAccountInfo(String token, String name, String avatar) {
        SetAccountInfoRequest request = new SetAccountInfoRequest(token);
        request.name = name;
        request.avatar = avatar;
        return this.api.setAccountInfo(getRequestBody(request));
    }

    /**
     * 激活内置的演示数据。
     *
     * @param contactId
     * @param domain
     * @param zoneName
     * @return
     */
    public Observable<ContactZoneResponse> activateBuildInData(long contactId,
                                                               String domain,
                                                               String zoneName) {
        return this.api.activateBuildInData(contactId, domain, zoneName);
    }

    /**
     * 搜索指定 ID 的账号。
     *
     * @param accountId
     * @return
     */
    public Observable<SearchAccountResultResponse> searchAccountById(Long accountId) {
        return this.api.searchAccountById(AccountHelper.getInstance().getTokenCode(), accountId);
    }

    /**
     * 搜索指定手机号码的账号。
     *
     * @param phoneNumber
     * @return
     */
    public Observable<SearchAccountResultResponse> searchAccount(String phoneNumber) {
        return this.api.searchAccount(AccountHelper.getInstance().getTokenCode(), phoneNumber);
    }

    private RequestBody getRequestBody(Object object) {
        String content = new Gson().toJson(object);
        MediaType mediaType = MediaType.Companion.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.Companion.create(content, mediaType);
        return body;
    }

    private OkHttpClient getClient() {
        File httpCacheDir = new File(CubeApp.getContext().getCacheDir(), "response");
        int cacheSize = 10 * 1024 * 1024;   // 10 MiB
        Cache cache = new Cache(httpCacheDir, cacheSize);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(REWRITE_HEADER_CONTROL_INTERCEPTOR)
                .addInterceptor(REWRITE_CACHE_CONTROL_INTERCEPTOR)
                .addInterceptor(new LoggingInterceptor())
                .connectTimeout(3, TimeUnit.SECONDS)
                .cache(cache)
                .build();
        return client;
    }

    /**
     * 协议头处理。
     */
    private Interceptor REWRITE_HEADER_CONTROL_INTERCEPTOR = chain -> {
        Request request = chain.request()
                .newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Connection", "keep-alive")
                .build();
        return chain.proceed(request);
    };

    /**
     * 缓存控制。
     */
    private Interceptor REWRITE_CACHE_CONTROL_INTERCEPTOR = chain -> {
        // 通过 CacheControl 控制缓存数据
        CacheControl.Builder cacheBuilder = new CacheControl.Builder();
        cacheBuilder.maxAge(120, TimeUnit.SECONDS);
        cacheBuilder.maxStale(365, TimeUnit.DAYS);
        CacheControl cacheControl = cacheBuilder.build();

        // 设置拦截器
        Request request = chain.request();
        if (!NetworkUtils.isAvailable(CubeApp.getContext())) {
            request = request.newBuilder()
                    .cacheControl(cacheControl)
                    .build();
        }

        Response originalResponse = chain.proceed(request);
        if (NetworkUtils.isAvailable(CubeApp.getContext())) {
            int maxAge = 0;
            return originalResponse.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public ,max-age=" + maxAge)
                    .build();
        }
        else {
            // 4 周 Stale
            int maxStale = 60 * 60 * 24 * 28;
            return originalResponse.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                    .build();
        }
    };

    private class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            LogUtils.p(String.format("Send request [%s]%n%s",
                    request.url(),
                    request.headers()));

            // 请求发起的时间
            long t1 = System.nanoTime();
            Response response = chain.proceed(request);
            // 收到响应的时间
            long t2 = System.nanoTime();

            // Response 中的流会被关闭，创新新的 Response
            ResponseBody responseBody = response.peekBody(1024 * 1024);
            LogUtils.p(String.format("Received response: [%s]%nJSON:%s %.1fms%n%s",
                    response.request().url(),
                    responseBody.string(),
                    (t2 - t1) / 1e6d,
                    response.headers()));
            return response;
        }
    }
}
