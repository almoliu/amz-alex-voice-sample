package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class PlaybackStatePayload extends PlaybackPayload{

    private final String playerActivity;

    public PlaybackStatePayload(String token, long offsetInMilliseconds, String playerActivity) {
       super(token,offsetInMilliseconds);
        this.playerActivity = playerActivity;
    }


    public String getPlayerActivity() {
        return playerActivity;
    }

    @Override
    public JSONObject toJson() {
        JSONObject playbackState = new JSONObject();
        try {
            playbackState.put("token",getToken());
            playbackState.put("offsetInMilliseconds",getOffsetInMilliseconds());
            playbackState.put("playerActivity",playerActivity);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return playbackState;
    }
}
