package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class DirectiveVolumePayload extends Payload {
    private long mVolume;

    public DirectiveVolumePayload(long vol) {
        mVolume = vol;
    }

    public long getVolume() {
        return mVolume;
    }

    @Override
    public JSONObject toJson() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("volume",mVolume);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }
}
