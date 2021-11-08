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

import android.os.Build;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import cube.contact.ContactService;
import cube.contact.ContactServiceEvent;
import cube.contact.model.Self;
import cube.core.Module;
import cube.core.ModuleError;
import cube.filestorage.handler.UploadFileHandler;
import cube.util.LogUtils;
import cube.util.ObservableEvent;
import cube.util.Observer;

/**
 * 文件存储服务。
 */
public class FileStorage extends Module {

    public final static String NAME = "FileStorage";

    private Self self;

    private String fileURL = "http://cube.shixincube.com/filestorage/file/";

    private String fileSecureURL = "https://cube.shixincube.com/filestorage/file/";

    private int fileBlockSize = 10 * 1024;

    public FileStorage() {
        super(NAME);
    }

    @Override
    public boolean start() {
        if (!super.start()) {
            return false;
        }

        ContactService contactService = (ContactService) this.kernel.getModule(ContactService.NAME);
        contactService.attachWithName(ContactServiceEvent.SelfReady, new Observer() {
            @Override
            public void update(ObservableEvent event) {
                self = contactService.getSelf();
            }
        });
        this.self = contactService.getSelf();

        return true;
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    protected void config(@Nullable JSONObject configData) {
        try {
            this.fileURL = configData.getString("fileURL");
            this.fileSecureURL = configData.getString("fileSecureURL");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (Build.SERIAL.contains("unknown")) {
            // FIXME 以下判断仅用于测试，Release 时务必使用域名
            // 模拟器里将 127.0.0.1 修改为 10.0.2.2
            this.fileURL = this.fileURL.replace("127.0.0.1", "10.0.2.2");
            this.fileSecureURL = this.fileSecureURL.replace("127.0.0.1", "10.0.2.2");
        }
    }

    public void uploadFile(File file, UploadFileHandler handler) {

    }

    public void uploadFile(String filename, InputStream inputStream, UploadFileHandler handler) {
        if (!this.hasStarted() || null == this.self) {
            if (handler.isInMainThread()) {
                this.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        ModuleError error = new ModuleError(NAME, 1);
                        handler.handleFailure(error);
                    }
                });
            }
            else {
                this.execute(new Runnable() {
                    @Override
                    public void run() {
                        ModuleError error = new ModuleError(NAME, 1);
                        handler.handleFailure(error);
                    }
                });
            }
            return;
        }

        long lastModified = System.currentTimeMillis();
        try {
            long fileSize = inputStream.available();
            byte[] data = new byte[this.fileBlockSize];
            int cursor = 0;
            int length = 0;
            while ((length = inputStream.read(data)) > 0) {
                FileFormData formData = new FileFormData(this.self.id, filename, fileSize, lastModified, cursor, length);
                formData.setData(data, 0, length);


            }
        } catch (IOException e) {
            LogUtils.w(this.getClass().getSimpleName(), e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
