package com.goertek.smartear.amazon;

import java.io.InputStream;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class SpeakItem {
    private final String token;
    private final InputStream audio;

    public SpeakItem(String token, InputStream audio) {
        this.token = token;
        this.audio = audio;
    }

    public String getToken() {
        return token;
    }

    public InputStream getAudio() {
        return audio;
    }
}
