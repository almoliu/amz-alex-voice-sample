package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/21.
 */

public class PlaybackPayload extends Payload{
    private final String token;
    private final long offsetInMilliseconds;

    public PlaybackPayload(String token, long offsetInMilliseconds) {
        this.token = token;
        this.offsetInMilliseconds = offsetInMilliseconds;
    }

    public String getToken() {
        return token;
    }

    public long getOffsetInMilliseconds() {
        return offsetInMilliseconds;
    }


    @Override
    public JSONObject toJson() {
        JSONObject playbackState = new JSONObject();
        try {
            playbackState.put("token",token);
            playbackState.put("offsetInMilliseconds",offsetInMilliseconds);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return playbackState;
    }
}
