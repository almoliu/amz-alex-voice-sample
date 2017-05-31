package com.goertek.smartear.amazon;

/**
 * Created by almo.liu on 2016/11/17.
 */

public enum SpeechProfile {
    CLOSE_TALK("CLOSE_TALK"),
    AUDIO_FORMAT("AUDIO_L16_RATE_16000_CHANNELS_1");

    private final String profileName;

    SpeechProfile(String profileName) {
        this.profileName = profileName;
    }

    @Override
    public String toString() {
        return this.profileName;
    }
}
