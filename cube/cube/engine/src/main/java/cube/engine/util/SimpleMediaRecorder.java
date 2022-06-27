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

package cube.engine.util;

import android.media.MediaRecorder;

import java.io.IOException;

import cube.util.LogUtils;

/**
 * 录音功能辅助函数库。
 */
public final class SimpleMediaRecorder {

    private final static String TAG = "MediaRecorderUtil";

    private String filePath;

    private MediaRecorder recorder;

    private Type type;

    public SimpleMediaRecorder(String filePath) {
        this(filePath, Type.AAC_M4A);
    }

    public SimpleMediaRecorder(String filePath, Type type) {
        this.filePath = filePath;
        this.type = type;
    }

    public boolean start() {
        if (null != this.recorder) {
            return false;
        }

        this.recorder = new MediaRecorder();
        this.recorder.setOutputFile(this.filePath);
        try {
            this.recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        } catch (Exception e) {
            LogUtils.w(TAG, e);
            this.recorder = null;
            return false;
        }

        this.recorder.setOutputFormat(this.type.outputFormat);
        this.recorder.setAudioEncoder(this.type.audioEncoder);

        try {
            this.recorder.prepare();
        } catch (IOException e) {
            LogUtils.e(TAG, e);
            return false;
        }

        this.recorder.start();
        return true;
    }

    public void stop() {
        if (null == this.recorder) {
            return;
        }

        this.recorder.stop();
        this.recorder.release();
        this.recorder = null;
    }

    public float getVoiceLevel() {
        float level = 0.0f;

        if (null != this.recorder) {
            // getMaxAmplitude() 值范围 1-32767
            // mediaRecorder.getMaxAmplitude() / 32768
            // 8 * value 表示分为 9 级，0 - 8 ，共 9 级
            level = 8 * (((float) this.recorder.getMaxAmplitude()) / 32768f);
        }

        return level;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public enum Type {
        AAC_M4A(".m4a", MediaRecorder.AudioEncoder.AAC_ELD, MediaRecorder.OutputFormat.MPEG_4),
        VOR_OGG(".ogg", MediaRecorder.AudioEncoder.VORBIS, MediaRecorder.OutputFormat.OGG),
        AAC_AAC(".aac", MediaRecorder.AudioEncoder.AAC, MediaRecorder.OutputFormat.AAC_ADTS),
        AMR_AMR(".amr", MediaRecorder.AudioEncoder.AMR_NB, MediaRecorder.OutputFormat.AMR_NB);

        public final String ext;
        public final int audioEncoder;
        public final int outputFormat;

        Type(String ext, int audioEncoder, int outputFormat) {
            this.ext = ext;
            this.audioEncoder = audioEncoder;
            this.outputFormat = outputFormat;
        }
    }
}
