package com.goertek.smartear.amazon;

import android.util.Log;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class MultipartParser {
    private static final boolean D = true;
    private static final String TAG = MultipartParser.class.getSimpleName();

    private static final int MULTIPART_BUFFER_SIZE = 1024*1024;

    private final MultipartParserConsumer consumer;
    private final AtomicBoolean shutdown;
    private MultipartStream mMultipartStream;
    private Map<String, String> headers;

    public MultipartParser(MultipartParserConsumer consumer) {
        this.consumer = consumer;
        this.shutdown = new AtomicBoolean(false);
    }

    public void shutdownGracefully() {
        shutdown.set(false);
    }

    public void parseStream(InputStream inputStream, String boundary) throws IOException {
        shutdown.set(false);
        mMultipartStream =
                new MultipartStream(inputStream, boundary.getBytes(), MULTIPART_BUFFER_SIZE, null);

        loopStream();
    }

    private void loopStream() throws IOException {

        try {
            boolean hasNextPart = mMultipartStream.skipPreamble();// here may cause blocking...
            while (hasNextPart) {
                handlePart();
                hasNextPart = mMultipartStream.readBoundary();
            }
        } catch (IOException e) {
            if (!shutdown.get()) {
                throw e;
            }
        }
    }

    private void handlePart() throws IOException {

        headers = getPartHeaders();
        byte[] partBytes = getPartBytes();

        if(D) Log.d(TAG,"partBytes is:\n"+new String(partBytes));
        boolean isMetadata = isPartJSON(headers);

        if (isMetadata) {
            handleMetadata(partBytes);
        } else {
            handleAudio(partBytes);
        }
    }

    private void handleMetadata(byte[] partBytes) throws IOException {

        Directive directive = parseResponseBody(partBytes);
        if (directive != null) {
            consumer.onDirective(directive);
        }else {
            if(D) Log.d(TAG,"wrong directive...");
        }

    }

    private void handleAudio(byte[] partBytes) {
        String contentId = getMultipartContentId(headers);
        InputStream attachmentContent = new ByteArrayInputStream(partBytes);
        consumer.onDirectiveAttachment(contentId, attachmentContent);
    }

    private Directive parseResponseBody(byte[] bytes) throws IOException {

        if(D) Log.d(TAG,"bytes in  parseResponseBody ---"+new String(bytes));

        ByteArrayInputStream data = new ByteArrayInputStream(bytes);
        JsonReader dataReader = Json.createReader(data);

        JsonObject metaData = dataReader.readObject();

        JsonObject directiveJson = metaData.getJsonObject("directive");
        JsonObject headerJson = directiveJson.getJsonObject("header");

        String namespace = headerJson.getString("namespace");

        if (namespace.equals(AVSConstants.AVSAPIConstants.SpeechRecognizer.NAMESPACE)) {
            return isSpeechRecognizerDirective(metaData);
        } else if (namespace.equals(AVSConstants.AVSAPIConstants.Alerts.NAMESPACE)) {
            return isAlertsDirective(metaData);
        } else if (namespace.equals(AVSConstants.AVSAPIConstants.AudioPlayer.NAMESPACE)) {
            return isAudioPlayerDirective(metaData);
        } else if (namespace.equals(AVSConstants.AVSAPIConstants.Speaker.NAMESPACE)) {
            return isSpeakerDirective(metaData);
        } else if (namespace.equals(AVSConstants.AVSAPIConstants.SpeechSynthesizer.NAMESPACE)) {
            return isSpeechSynthesizerDirective(metaData);
        } else if (namespace.equals(AVSConstants.AVSAPIConstants.System.NAMESPACE)) {
            return isSystemDirective(metaData);
        } else if (namespace.equals(AVSConstants.AVSAPIConstants.PlaybackController.NAMESPACE)) {
            throw new IllegalArgumentException("no PlaybackController ");
        }

        return null;
    }

    private byte[] getPartBytes() throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        mMultipartStream.readBodyData(data);

        return data.toByteArray();
    }


    private String getMultipartHeaderValue(Map<String, String> headers, String searchHeader) {
        return headers.get(searchHeader.toLowerCase());
    }

    private String getMultipartContentId(Map<String, String> headers) {
        String contentId = getMultipartHeaderValue(headers, HttpHeaders.CONTENT_ID);
        contentId = contentId.substring(1, contentId.length() - 1);
        return contentId;
    }

    private boolean isPartJSON(Map<String, String> headers) {
        String contentType = getMultipartHeaderValue(headers, HttpHeaders.CONTENT_TYPE);
        return StringUtils.contains(contentType, ContentTypes.JSON);
    }

    private Map<String, String> getPartHeaders() throws IOException {
        String headers = mMultipartStream.readHeaders();
        BufferedReader reader = new BufferedReader(new StringReader(headers));
        Map<String, String> headerMap = new HashMap<>();
        try {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                line = line.trim();
                if (!StringUtils.isBlank(line) && line.contains(":")) {
                    int colon = line.indexOf(":");
                    String headerName = line.substring(0, colon).trim();
                    String headerValue = line.substring(colon + 1).trim();
                    headerMap.put(headerName.toLowerCase(), headerValue);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return headerMap;
    }

    private Directive isSpeechRecognizerDirective(JsonObject metaData) {

        JsonObject directiveJson  = metaData.getJsonObject("directive");
        JsonObject headerJson = directiveJson.getJsonObject("header");

        Msg2DialogIdHeader header = new Msg2DialogIdHeader(headerJson.getString("namespace"),
                headerJson.getString("name"),headerJson.getString("dialogRequestId"));
        header.setMessageId(headerJson.getString("messageId"));

        JsonObject payloadJson = directiveJson.getJsonObject("payload");

        Payload payload = new DirectiveExpectSpeechPayload(payloadJson.getInt("timeoutInMilliseconds"));

        return new Directive(header,payload,null);
    }

    private Directive isSpeechSynthesizerDirective(JsonObject metaData) {
        JsonObject directiveJson  = metaData.getJsonObject("directive");
        JsonObject headerJson = directiveJson.getJsonObject("header");

        Msg2DialogIdHeader header = new Msg2DialogIdHeader(headerJson.getString("namespace"),
                headerJson.getString("name"),headerJson.getString("dialogRequestId"));
        header.setMessageId(headerJson.getString("messageId"));

        JsonObject payloadJson = directiveJson.getJsonObject("payload");

        Payload payload = new DirectiveSpeakPayload(payloadJson.getString("url").replace("cid:",""),//test
                payloadJson.getString("format"),payloadJson.getString("token"));

        return new Directive(header,payload,null);
    }

    private Directive isAudioPlayerDirective(JsonObject metaData) {


        if(D) Log.d(TAG,"isAudioPlayerDirective -----"+metaData.toString());

        JsonObject directiveJson  = metaData.getJsonObject("directive");
        JsonObject headerJson = directiveJson.getJsonObject("header");

        Msg2DialogIdHeader header = new Msg2DialogIdHeader(headerJson.getString("namespace"),
                headerJson.getString("name"),headerJson.getString("dialogRequestId"));
        header.setMessageId(headerJson.getString("messageId"));

        Payload payload = null;

        if(headerJson.getString("name").equals("Stop")) {
            payload = new Payload();
        }else if(headerJson.getString("name").equals("Play")) {

            JsonObject payloadJson = directiveJson.getJsonObject("payload");
            JsonObject audioItem = payloadJson.getJsonObject("audioItem");
            String playBehavior = payloadJson.getString("playBehavior");
            String audioItemId = audioItem.getString("audioItemId");
            JsonObject stream = audioItem.getJsonObject("stream");
            int offset = stream.getInt("offsetInMilliseconds");
//            String expiryTime = stream.getString("expiryTime");
            String expiryTime = "18:00";
            String url = stream.getString("url");
            String token = stream.getString("token");


            if (D) Log.d(TAG, "payloadJson is:\n" + payloadJson);

            payload = new DirectiveAudioPlayerPayload(audioItemId, offset, expiryTime, url,
                    token, playBehavior);
        }else if(headerJson.getString("name").equals("Pause")) {

        }
        return new Directive(header,payload,null);
    }

    private Directive isAlertsDirective(JsonObject metaData) {
        JsonObject directiveJson  = metaData.getJsonObject("directive");
        JsonObject headerJson = directiveJson.getJsonObject("header");

        Msg2DialogIdHeader header = new Msg2DialogIdHeader(headerJson.getString("namespace"),
                headerJson.getString("name"),headerJson.getString("dialogRequestId"));
        header.setMessageId(headerJson.getString("messageId"));

        JsonObject payloadJson = directiveJson.getJsonObject("payload");

        Payload payload = new DirectiveDeleteAlertPayload(payloadJson.getString("token"));

        return new Directive(header,payload,null);
    }

    private Directive isSpeakerDirective(JsonObject metaData) {
        JsonObject directiveJson  = metaData.getJsonObject("directive");
        JsonObject headerJson = directiveJson.getJsonObject("header");

        Msg2DialogIdHeader header = new Msg2DialogIdHeader(headerJson.getString("namespace"),
                headerJson.getString("name"),headerJson.getString("dialogRequestId"));
        header.setMessageId(headerJson.getString("messageId"));
        JsonObject payloadJson = directiveJson.getJsonObject("payload");
        Payload payload = null;
        String name = headerJson.getString("name");
        if(name.equals(AVSConstants.AVSAPIConstants.Speaker.Directives.SetMute.NAME)) {
            payload = new DirectiveMutePayload(payloadJson.getBoolean("mute"));
        }else if(name.equals(AVSConstants.AVSAPIConstants.Speaker.Directives
                .AdjustVolume.NAME)||name.equals(AVSConstants.AVSAPIConstants.Speaker.Directives.SetVolume
                .NAME)) {
            payload = new DirectiveVolumePayload(payloadJson.getInt("volume"));
        }

        return new Directive(header,payload,null);
    }

    private Directive isSystemDirective(JsonObject metaData) {
        JsonObject directiveJson  = metaData.getJsonObject("directive");
        JsonObject headerJson = directiveJson.getJsonObject("header");

        Msg2DialogIdHeader header = new Msg2DialogIdHeader(headerJson.getString("namespace"),
                headerJson.getString("name"),headerJson.getString("dialogRequestId"));
        header.setMessageId(headerJson.getString("messageId"));

        JsonObject payloadJson = directiveJson.getJsonObject("payload");

        Payload payload = new Payload();

        return new Directive(header,payload,null);
    }

}
