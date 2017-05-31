package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/17.
 */
public class VolumeStatePayload extends Payload {
    private final long volume;
    private final boolean muted;

    public VolumeStatePayload(long volume, boolean muted) {
        this.volume = volume;
        this.muted = muted;
    }

    public long getVolume() {
        return volume;
    }

    public boolean getMuted() {
        return muted;
    }

    @Override
    public JSONObject toJson() {
        JSONObject volState = new JSONObject();
        try {
            volState.put("volume", volume);
            volState.put("muted", muted);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return volState;
    }
}
