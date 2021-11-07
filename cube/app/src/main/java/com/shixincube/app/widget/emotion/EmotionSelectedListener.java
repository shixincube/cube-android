package com.shixincube.app.widget.emotion;

public interface EmotionSelectedListener {

    void onEmojiSelected(String key);

    void onStickerSelected(String categoryName, String stickerName, String stickerBitmapPath);
}
