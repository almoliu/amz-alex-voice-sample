package com.goertek.smartear.amazon;

import android.app.Service;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class AudioFocus {
    private static final String TAG = "AudioFocus";
    private static AudioManager sAudioManager = null;

    private static AudioManager.OnAudioFocusChangeListener sFocusListener = null;

    public static boolean isInit() {
        return null != sAudioManager && null != sFocusListener;
    }

    public static void init(Context context) {
        sAudioManager = (AudioManager) context.getApplicationContext().getSystemService(Service.AUDIO_SERVICE);
        sFocusListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                Log.d(TAG, "audio focus changed: " + focusChange);
            }
        };
    }

    public static void setFocus() {
        if (isInit()) {
            sAudioManager.requestAudioFocus(sFocusListener, AudioManager.STREAM_MUSIC, AudioManager
                    .AUDIOFOCUS_GAIN_TRANSIENT);
            Log.d(TAG, "request audio focus");
        } else {
            Log.e(TAG, "request audio focus error: not init");
        }
    }

    public static void abandonFocus() {
        if (isInit()) {
            sAudioManager.abandonAudioFocus(sFocusListener);
            Log.d(TAG, "abandon audio focus");
        } else {
            Log.e(TAG, "request audio focus error: not init");
        }
    }
}
