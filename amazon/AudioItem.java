package com.goertek.smartear.amazon;

import java.util.stream.Stream;

/**
 * Created by almo.liu on 2016/11/21.
 */

public class AudioItem {
    private String mToken;
    private String mUrl;

    public AudioItem(String token,String url) {
        mToken = token;
        mUrl = url;
    }

    public String getAudioUrl() {
        return mUrl;
    }

    public String getToken() {
         return mToken;
    }

    public void setAudioUrl(String url) {
        mUrl = url;
    }

    public void setToken(String token) {
       mToken = token;
    }
}
