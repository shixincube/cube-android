/*
 * This file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 Shixin Cube Team.
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

package cube.core;

/**
 * 数据通道状态。
 */
public enum PipelineState {

    /**
     * 成功。
     */
    Ok(1000, "Ok"),

    /**
     * 数据请求错误。
     */
    BadRequest(1400, "Bad request"),

    /**
     * 未知的请求命令。
     */
    NotFound(1404, "Can not find pipeline destination"),

    /**
     * 没有找到授权码。
     */
    NoAuthToken(1501, "No auth token in parameters"),

    /**
     * 请求服务超时。
     */
    ServiceTimeout(2001, "Service timeout"),

    /**
     * 负载格式错误。
     */
    PayloadFormat(2002, "Packet payload format error"),

    /**
     * 参数错误。
     */
    InvalidParameter(2003, "Invalid parameter"),

    /**
     * 网关错误。
     */
    GatewayError(2101, "Gateway server error")

    ;

    /**
     * 状态码。
     */
    public final int code;

    /**
     * 状态描述。
     */
    public final String description;

    PipelineState(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PipelineState parse(int code) {
        for (PipelineState state : PipelineState.values()) {
            if (state.code == code) {
                return state;
            }
        }

        return PipelineState.GatewayError;
    }
}
