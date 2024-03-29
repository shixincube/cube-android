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

package cube.filestorage;

/**
 * 文件存储服务状态描述。
 */
public enum FileStorageState {

    /**
     * 成功。
     */
    Ok(0),

    /**
     * 遇到故障。
     */
    Failure(9),

    /**
     * 无效的域信息。
     */
    InvalidDomain(11),

    /**
     * 无效的参数，禁止访问。
     */
    Forbidden(12),

    /**
     * 未找到指定数据。
     */
    NotFound(13),

    /**
     * 未授权访问。
     */
    Unauthorized(14),

    /**
     * 拒绝操作。
     */
    Reject(15),

    /**
     * 文件标签错误。
     */
    FileLabelError(16),

    /**
     * 正在写入文件。
     */
    Writing(17),

    /**
     * 重名。
     */
    DuplicationOfName(20),

    /**
     * 数据过期。
     */
    DataExpired(21),

    /**
     * 搜索条件错误。
     */
    SearchConditionError(25),

    /**
     * 模块的工作状态未就绪。
     */
    NotReady(101),

    /**
     * 文件 I/O 异常。
     */
    IOException(102),

    /**
     * 读取文件错误。
     */
    ReadFileFailed(103),

    /**
     * 数据传输故障。
     * @type {number}
     */
    TransmitFailed(104),

    /**
     * 获取文件标签失败。
     */
    GetFileLabelFailed(105),

    /**
     * 数据通道未就绪。
     */
    PipelineNotReady(106),

    /**
     * 数据格式错误。
     */
    DataFormatError(107),

    /**
     * 未知的状态。
     */
    Unknown(99);


    /**
     * 状态代码。
     */
    public final int code;

    FileStorageState(int code) {
        this.code = code;
    }

    public static FileStorageState parse(int code) {
        for (FileStorageState state : FileStorageState.values()) {
            if (state.code == code) {
                return state;
            }
        }

        return FileStorageState.Unknown;
    }
}
