package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class DirectiveAudioPlayerPayload extends Payload {

    private String mAudioItemId;
    private int mOffset;
    private String mExpiryTime;
    private String mUrl;
    private String mToken;
    private String mPlayBehavior;

    public DirectiveAudioPlayerPayload(String audioItemId, int offset, String expifyTime, String
            url, String token, String playBehivor) {
        mAudioItemId = audioItemId;
        mOffset = offset;
        mExpiryTime = expifyTime;
        mUrl = url;
        mToken = token;
        mPlayBehavior = playBehivor;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getToken() {
        return mToken;
    }

    @Override
    public JSONObject toJson() {

        JSONObject payload = new JSONObject();
        JSONObject audioItem = new JSONObject();

        try {
            audioItem.put("audioItemId", mAudioItemId);
            JSONObject stream = new JSONObject();
            stream.put("offsetInMilliseconds", mOffset);
            stream.put("expiryTime", mExpiryTime);
            stream.put("url", mUrl);
            stream.put("token", mToken);
            audioItem.put("stream", stream);
            payload.put("audioItem", audioItem);
            payload.put("playBehavior", mPlayBehavior);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }
}
