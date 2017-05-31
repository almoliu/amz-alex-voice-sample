package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class MessageIdHeader extends Header{

    private String messageId;

    public MessageIdHeader() {
        // For Jackson
    }

    public MessageIdHeader(String namespace, String name) {
        super(namespace, name);
        this.messageId = UUID.randomUUID().toString();
    }

    public final void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public final String getMessageId() {
        return messageId;
    }

    @Override
    public String toString() {
        return String.format("%1$s id:%2$s", super.toString(), messageId);
    }

    public JSONObject toJson() {
        JSONObject header = new JSONObject();
        try {
            header.put("namespace",super.getNamespace());
            header.put("name",super.getName());
            header.put("messageId",messageId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return header;
    }
}
