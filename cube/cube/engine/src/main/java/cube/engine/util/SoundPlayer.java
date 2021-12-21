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
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

import cube.engine.R;

/**
 * 音效播放器。
 */
public class SoundPlayer {

    private SoundPool soundPool;

    private int soundRinging;
    private int soundOutgoing;
    private int soundHangup;

    public SoundPlayer(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.soundPool = new SoundPool.Builder().setMaxStreams(4).build();
        }
        else {
            this.soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        }

        this.soundRinging = this.soundPool.load(context, R.raw.ringing, 1);
        this.soundOutgoing = this.soundPool.load(context, R.raw.outgoing, 1);
        this.soundHangup = this.soundPool.load(context, R.raw.hungup, 1);
    }

    public void playRinging() {
        this.soundPool.play(this.soundRinging, 1, 1, 0, -1, 1);
    }

    public void stopRinging() {
        this.soundPool.stop(this.soundRinging);
    }

    public void playOutgoing() {
        this.soundPool.play(this.soundOutgoing, 1, 1, 0, -1, 1);
    }

    public void stopOutgoing() {
        this.soundPool.stop(this.soundOutgoing);
    }

    public void playHangup() {
        this.soundPool.play(this.soundHangup, 1, 1, 0, 0, 1);
    }

    public void stopHangup() {
        this.soundPool.stop(this.soundHangup);
    }
}
