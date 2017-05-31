package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class SpeechRecognizerPayload extends Payload {
    private final String profile;
    private final String format;

    public SpeechRecognizerPayload(SpeechProfile profile, String format) {
        this.profile = profile.toString();
        this.format = format;
    }

    public String getProfile() {
        return profile;
    }

    public String getFormat() {
        return format;
    }

    @Override
    public JSONObject toJson() {
        JSONObject sphRecoJson = new JSONObject();
        try {
            sphRecoJson.put("profile", profile);
            sphRecoJson.put("format", format);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return sphRecoJson;
    }
}