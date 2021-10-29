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

import com.shixincube.app.model.response.CheckPhoneResponse;
import com.shixincube.app.model.response.AccountInfoResponse;
import com.shixincube.app.model.response.LoginResponse;
import com.shixincube.app.model.response.RegisterResponse;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * 应用程序接口。
 */
public interface AppInterface {

    /**
     * 检查手机号码是否可用。支持设置是否发送验证码。
     *
     * @param body
     * @return
     */
    @POST("/account/check_phone_available/")
    Observable<CheckPhoneResponse> checkPhoneAvailable(@Body RequestBody body);

    /**
     * 注册账号。
     *
     * @param body
     * @return
     */
    @POST("/account/register/")
    Observable<RegisterResponse> register(@Body RequestBody body);

    /**
     * 账号登录。
     *
     * @param body
     * @return
     */
    @POST("/account/login/")
    Observable<LoginResponse> login(@Body RequestBody body);

    /**
     * 查询自己的账号数据。
     *
     * @param token
     * @return
     */
    @GET("/account/info/")
    Observable<AccountInfoResponse> getAccountInfo(@Query("token") String token);

    /**
     * 查询指定联系人的账号。
     *
     * @param id
     * @param token
     * @return
     */
    @GET("/account/info/")
    Observable<AccountInfoResponse> getAccountInfo(@Query("id") Long id, @Query("token") String token);
}
