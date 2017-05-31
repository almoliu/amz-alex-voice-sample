package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/17.
 */
public class SpeechStatePayload extends Payload {
    private final String token;
    private final long offsetInMilliseconds;
    private final String playerActivity;

    public SpeechStatePayload(String token, long offsetInMilliseconds, String playerActivity) {
        this.token = token;
        this.offsetInMilliseconds = offsetInMilliseconds;
        this.playerActivity = playerActivity;
    }

    public String getToken() {
        return this.token;
    }

    public long getOffsetInMilliseconds() {
        return this.offsetInMilliseconds;
    }

    public String getPlayerActivity() {
        return this.playerActivity;
    }


    @Override
    public JSONObject toJson() {

        JSONObject speechState = new JSONObject();
        try {
            speechState.put("token", token);
            speechState.put("offsetInMilliseconds", offsetInMilliseconds);
            speechState.put("playerActivity", playerActivity);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return speechState;
    }
}