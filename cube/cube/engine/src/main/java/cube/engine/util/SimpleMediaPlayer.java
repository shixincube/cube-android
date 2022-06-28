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

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;

import java.io.IOException;

/**
 * 简单媒体播放器。
 */
public final class SimpleMediaPlayer {

    private final static SimpleMediaPlayer instance = new SimpleMediaPlayer();

    private Context context;

    private MediaPlayer player;

    private OnPlayListener playListener;

    private Uri currentSource;

    private SimpleMediaPlayer() {
    }

    public static SimpleMediaPlayer getInstance() {
        return SimpleMediaPlayer.instance;
    }

    public void play(Context context, String path, OnPlayListener playListener) {
        if (null == this.player) {
            this.player = new MediaPlayer();
        }

        if (this.isPlaying()) {
            this.stop();
        }

        this.context = context;
        this.currentSource = Uri.parse(path);
        this.playListener = playListener;

        try {
            this.player.reset();
            this.player.setDataSource(context, this.currentSource);
            this.player.prepare();

            this.player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    playListener.onCompletion(currentSource);
                }
            });

            this.player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    playListener.onPrepared(currentSource);
                    player.start();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (this.isPlaying()) {
            this.player.stop();
            this.playListener.onStop(this.currentSource);

            this.context = null;
        }
    }

    public boolean isPlaying() {
        return (null != this.player && this.player.isPlaying());
    }

    /**
     * 切换到扬声器。
     *
     * @param audioManager
     */
    public void changeToSpeaker(AudioManager audioManager) {
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(true);
    }

    /**
     * 切换到听筒。
     *
     * @param audioManager
     */
    public void changeToReceiver(AudioManager audioManager) {
        audioManager.setSpeakerphoneOn(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        }
        else {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
        }
    }

    /**
     * 切换到耳机模式。
     *
     * @param audioManager
     */
    public void changeToHeadset(AudioManager audioManager) {
        audioManager.setSpeakerphoneOn(false);
    }

    /**
     * 是否接入了耳机。
     *
     * @param audioManager
     * @return
     */
    public boolean isHeadsetOn(AudioManager audioManager) {
        if (null == audioManager) {
            return false;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return audioManager.isWiredHeadsetOn() || audioManager.isBluetoothScoOn()
                    || audioManager.isBluetoothA2dpOn();
        }
        else {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                    || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                    || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 播放状态监听器。
     */
    public interface OnPlayListener {

        /**
         * 媒体数据已就绪。
         *
         * @param uri
         */
        void onPrepared(Uri uri);

        /**
         * 媒体数据播放完毕。
         *
         * @param uri
         */
        void onCompletion(Uri uri);

        /**
         * 媒体数据播放停止。
         *
         * @param uri
         */
        void onStop(Uri uri);
    }
}
