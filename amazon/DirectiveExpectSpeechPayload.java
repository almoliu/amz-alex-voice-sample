package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class DirectiveExpectSpeechPayload extends Payload {

    private long timeoutInMilliseconds;

    public DirectiveExpectSpeechPayload(long timeout) {
        timeoutInMilliseconds = timeout;
    }

    @Override
    public JSONObject toJson() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("timeoutInMilliseconds",timeoutInMilliseconds);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }
}

