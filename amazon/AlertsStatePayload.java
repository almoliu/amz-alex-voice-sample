package com.goertek.smartear.amazon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class AlertsStatePayload extends Payload {

    @Override
    public JSONObject toJson() {
        JSONObject alertsState = new JSONObject();
        JSONArray allAlerts = new JSONArray();
        JSONArray activeAlerts = new JSONArray();
        try {
            alertsState.put("allAlerts",allAlerts);
            alertsState.put("activeAlerts",activeAlerts);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return alertsState;
    }
}
