package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by almo.liu on 2016/11/17.
 */
public abstract class Message {

    protected Header header;
    protected Payload payload;

    private String rawMessage;

    protected Message(Header header, Payload payload, String rawMessage) {
        this.header = header;
        this.payload = payload;
        this.rawMessage = rawMessage;
    }

    public String getName() {
        return header.getName();
    }

    public String getNamespace() {
        return header.getNamespace();
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Header getHeader() {
        return header;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public Payload getPayload() {
        return payload;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public JSONObject toJson() {
        JSONObject event = new JSONObject();
        try {
            event.put("header",header.toJson());
            event.put("payload",payload.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return event;
    }
}

