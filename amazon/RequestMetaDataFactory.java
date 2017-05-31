package com.goertek.smartear.amazon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class RequestMetaDataFactory {

    public static JSONObject createSpeechRecognizerRecognizeEvent(String dialogRequestId,
                                                                  SpeechProfile profile,
                                                                  String format,
                                                                  PlaybackStatePayload playerState,
                                                                  SpeechStatePayload speechState,
                                                                  AlertsStatePayload alertState,
                                                                  VolumeStatePayload volumeState) {


        Header header = new Msg2DialogIdHeader(AVSConstants.AVSAPIConstants.SpeechRecognizer.NAMESPACE,
                AVSConstants.AVSAPIConstants.SpeechRecognizer.Events.Recognize.NAME,dialogRequestId);

        SpeechRecognizerPayload sphRgzPayload = new SpeechRecognizerPayload(profile,format);

        Event event = new Event(header,sphRgzPayload);


        return createRequestWithAllState(event,playerState,speechState,alertState,volumeState);
    }

    public static JSONObject createSystemSynchronizeStateEvent(PlaybackStatePayload playerState,
                                                               SpeechStatePayload speechState,
                                                               AlertsStatePayload alertState,
                                                               VolumeStatePayload volumeState) {
        Header header = new MessageIdHeader(AVSConstants.AVSAPIConstants.System.NAMESPACE,
                AVSConstants.AVSAPIConstants.System.Events.SynchronizeState.NAME);
        Event event = new Event(header, new Payload());
        return createRequestWithAllState(event, playerState, speechState, alertState, volumeState);
    }

    public static JSONObject createSpeechSynthesizerSpeechStartedEvent(String speakToken) {
        return createSpeechSynthesizerEvent(AVSConstants.AVSAPIConstants.SpeechSynthesizer.Events
                .SpeechStarted.NAME, speakToken);
    }

    public static JSONObject createSpeechSynthesizerSpeechFinishedEvent(String speakToken) {
        return createSpeechSynthesizerEvent(AVSConstants.AVSAPIConstants.SpeechSynthesizer.Events
                .SpeechFinished.NAME,speakToken);
    }


    public static JSONObject createPlaybackFinishedEvent(String streamToken) {
        return createPlaybackEvent(AVSConstants.AVSAPIConstants.AudioPlayer.Events
                .PlaybackFinished.NAME,streamToken);
    }
    public static JSONObject createPlaybackStartedEvent(String streamToken) {
        return createPlaybackEvent(AVSConstants.AVSAPIConstants.AudioPlayer.Events
                .PlaybackStarted.NAME,streamToken);
    }
    public static JSONObject createPlaybackPausedEvent(String streamToken) {
        return createPlaybackEvent(AVSConstants.AVSAPIConstants.AudioPlayer.Events
                .PlaybackPaused.NAME,streamToken);
    }
    public static JSONObject createPlaybackStopEvent(String  streamToken) {
        return createPlaybackEvent(AVSConstants.AVSAPIConstants.AudioPlayer.Events
                .PlaybackStopped.NAME,streamToken);
    }
    public static JSONObject createPlaybackResumedEvent(String streamToken) {
        return createPlaybackEvent(AVSConstants.AVSAPIConstants.AudioPlayer.Events
                .PlaybackResumed.NAME,streamToken);
    }


    private static JSONObject createPlaybackEvent(String state,String streamtoken) {
        Header header = new MessageIdHeader(AVSConstants.AVSAPIConstants.AudioPlayer.NAMESPACE,
                state);
        Event event = new Event(header,new PlaybackPayload(streamtoken,0));
        JSONObject metaData = new JSONObject();
        try {
            metaData.put("event",event.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return metaData;
    }

    private static JSONObject createSpeechSynthesizerEvent(String name, String speakToken) {
        Header header = new MessageIdHeader(AVSConstants.AVSAPIConstants.SpeechSynthesizer.NAMESPACE, name);
        Event event = new Event(header, new SpeechLifecyclePayload(speakToken));

        JSONObject metaData = new JSONObject();
        try {
            metaData.put("event",event.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return metaData;
    }

    private static JSONObject createRequestWithAllState(Event event,
                                                        PlaybackStatePayload playbackState,
                                                        SpeechStatePayload speechState,
                                                        AlertsStatePayload alertState,
                                                        VolumeStatePayload volumeState) {

        JSONArray context =  RequestContextFactory.createContextWithAllState(playbackState,
                speechState, alertState, volumeState);

        JSONObject metaData = new JSONObject();
        try {
            metaData.put("event",event.toJson());
            metaData.put("context",context);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return metaData;
    }
}
