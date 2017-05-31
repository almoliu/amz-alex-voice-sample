package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class DirectiveMutePayload extends Payload{
    private boolean mMute;

    public DirectiveMutePayload(boolean on) {
        mMute = on;
    }

    public boolean getMute() {
        return mMute;
    }

    @Override
    public JSONObject toJson() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("mute",mMute);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return payload;
    }
}
