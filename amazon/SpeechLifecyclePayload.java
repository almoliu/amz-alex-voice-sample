package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/17.
 */
public class SpeechLifecyclePayload extends Payload {

    private final String token;

    public SpeechLifecyclePayload(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    @Override
    public JSONObject toJson() {
        JSONObject lifeCycle = new JSONObject();
        try {
            lifeCycle.put("token",token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return lifeCycle;

    }
}
