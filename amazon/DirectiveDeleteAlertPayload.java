package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class DirectiveDeleteAlertPayload extends Payload{

    private String mToken;
    public DirectiveDeleteAlertPayload(String token) {
        mToken = token;
    }

    @Override
    public JSONObject toJson() {
        JSONObject payload= new JSONObject();
        try {
            payload.put("token",mToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }
}
