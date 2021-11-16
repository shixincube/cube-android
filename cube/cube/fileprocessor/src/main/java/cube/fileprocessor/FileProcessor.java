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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Size;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

        File dir = new File(this.cacheDir);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        dir = null;

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
                .ignoreLessThan(100)
                .build();
        List<Biscuit.Result> list = biscuit.syncCompress();
        Biscuit.Result result = list.get(0);

        String path = result.path;

        if (0 == result.outputWidth || 0 == result.outputHeight) {
            Size size = this.getImageSize(path);
            result = result.reset(path, size);
        }

        if (file.getPath().equals(path)) {
            // 没有进行压缩，复制源文件到缓存
            File newFile = new File(this.cacheDir, file.getName());
            if (copyFile(file, newFile)) {
                result = result.reset(newFile.getPath());
                path = result.path;
            }
        }

        FileThumbnail thumbnail = new FileThumbnail(new File(path),
                result.outputWidth, result.outputHeight,
                biscuit.getQuality(),
                result.inputWidth, result.inputHeight);

        LogUtils.d(TAG, "#makeImageThumbnail : " + CalculationUtils.formatByteDataSize(file.length()) +
                " -> " + CalculationUtils.formatByteDataSize(thumbnail.getFile().length()));

        return thumbnail;
    }

    private Size getImageSize(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        if (null == bitmap) {
            return new Size(480, 480);
        }
        return new Size(options.outWidth, options.outHeight);
    }

    private boolean copyFile(File sourceFile, File targetFile) {
        FileInputStream fis = null;
        FileOutputStream fos = null;

        if (targetFile.exists()) {
            targetFile.delete();
        }

        try {
            fis = new FileInputStream(sourceFile);
            fos = new FileOutputStream(targetFile);

            byte[] buf = new byte[128 * 1024];
            int length = 0;
            while ((length = fis.read(buf)) > 0) {
                fos.write(buf, 0, length);
            }
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }
}
