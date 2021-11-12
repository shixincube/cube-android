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

package cube.fileprocessor;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

import cube.core.Module;
import cube.fileprocessor.biscuit.Biscuit;
import cube.fileprocessor.model.FileThumbnail;
import cube.fileprocessor.util.CalculationUtils;
import cube.filestorage.FileStorage;
import cube.util.LogUtils;

/**
 * 文件处理模块。
 */
public class FileProcessor extends Module {

    public final static String NAME = "FileProcessor";

    private final static String TAG = FileProcessor.class.getSimpleName();

    private String cacheDir;

    public FileProcessor() {
        super(FileProcessor.NAME);
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        if (this.kernel.hasModule(FileStorage.NAME)) {
            FileStorage fileStorage = (FileStorage) this.kernel.getModule(FileStorage.NAME);
            fileStorage.start();

            this.cacheDir = fileStorage.getFileCachePath() + "cache/";
        }
        else {
            this.cacheDir = getContext().getCacheDir().getAbsoluteFile() + "/cache/";
        }

        LogUtils.d(this.getClass().getSimpleName(), "Cache path: " + this.cacheDir);

        return true;
    }

    @Override
    protected void config(@Nullable JSONObject configData) {
    }

    @Override
    public boolean isReady() {
        return true;
    }

    /**
     * 制作图像文件的缩略图。
     *
     * @param file
     * @return
     */
    public FileThumbnail makeImageThumbnail(File file) {
        Biscuit biscuit = Biscuit.with(getContext())
                .path(file.getPath())
                .targetDir(this.cacheDir)
                .originalName(true)
                .ignoreLessThan(200)
                .build();
        List<Biscuit.Result> list = biscuit.syncCompress();
        Biscuit.Result result = list.get(0);

        String path = result.path;
        FileThumbnail thumbnail = new FileThumbnail(new File(path),
                result.outputWidth, result.outputHeight,
                biscuit.getQuality() / 100.0f,
                result.inputWidth, result.inputHeight);

        LogUtils.d(TAG, "#makeImageThumbnail : " + CalculationUtils.formatByteDataSize(file.length()) +
                " -> " + CalculationUtils.formatByteDataSize(thumbnail.getFile().length()));

        return thumbnail;
    }
}
