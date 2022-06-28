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
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;

/**
 * 简单媒体播放器。
 */
public final class SimpleMediaPlayer {

    private final static SimpleMediaPlayer instance = new SimpleMediaPlayer();

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
        }
    }

    public boolean isPlaying() {
        return (null != this.player && this.player.isPlaying());
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
