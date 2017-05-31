package com.goertek.smartear.amazon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class RequestContextFactory {
    public static JSONArray createContextWithAllState(PlaybackStatePayload playbackState,
                                                      SpeechStatePayload speechState,
                                                      AlertsStatePayload alertState,
                                                      VolumeStatePayload volumeState) {

        JSONArray context  = new JSONArray();
        try {
            context.put(0,createPlaybackState(playbackState));
            context.put(1,createSpeechState(speechState));
            context.put(2,createAlertsState(alertState));
            context.put(3,createVolumeState(volumeState));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return context;
    }

    private static JSONObject createPlaybackState(PlaybackStatePayload payload) {
        Header header = new Header(AVSConstants.AVSAPIConstants.AudioPlayer.NAMESPACE, AVSConstants.AVSAPIConstants
                .AudioPlayer.Events.PlaybackState.NAME);
        JSONObject plyJson = new JSONObject();
        try {
            plyJson.put("header",header.toJson());
            plyJson.put("payload",payload.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return plyJson;
    }

    private static JSONObject createSpeechState(SpeechStatePayload payload) {
        Header header = new Header(AVSConstants.AVSAPIConstants.SpeechSynthesizer.NAMESPACE, AVSConstants.AVSAPIConstants
                .SpeechSynthesizer.Events.SpeechState.NAME);
        JSONObject spkJson = new JSONObject();

        try {
            spkJson.put("header",header.toJson());
            spkJson.put("payload",payload.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return spkJson;
    }

    private static JSONObject createAlertsState(AlertsStatePayload payload) {
        JSONObject alertJson = new JSONObject();

        Header header = new Header(AVSConstants.AVSAPIConstants.Alerts.NAMESPACE, AVSConstants.AVSAPIConstants.Alerts.Events
                .AlertsState.NAME);
        try {
            alertJson.put("header",header.toJson());
            alertJson.put("payload",payload.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return alertJson;
    }

    private static JSONObject createVolumeState(VolumeStatePayload payload) {
        JSONObject volJson = new JSONObject();
        Header header = new Header(AVSConstants.AVSAPIConstants.Speaker.NAMESPACE, AVSConstants.AVSAPIConstants.Speaker
                .Events.VolumeState.NAME);
        try {
            volJson.put("header",header.toJson());
            volJson.put("payload",payload.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return volJson;
    }


}
