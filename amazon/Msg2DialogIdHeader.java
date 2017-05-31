package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class Msg2DialogIdHeader extends Header{
    private String messageId;
    private String dialogRequestId;

    public Msg2DialogIdHeader() {
        // For Jackson
    }

    public Msg2DialogIdHeader(String namespace, String name,String dialogRequestId) {
        super(namespace, name);
        this.messageId = UUID.randomUUID().toString();
        this.dialogRequestId = dialogRequestId;
    }

    public final void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public final String getMessageId() {
        return messageId;
    }

    public final void setDialogRequestId(String dialogRequestId) {
        this.dialogRequestId = dialogRequestId;
    }

    public final String getDialogRequestId() {
        return dialogRequestId;
    }

    @Override
    public String toString() {
        return String.format("%1$s id:%2$s dialogRequestId:%2$s", super.toString(), messageId,dialogRequestId);
    }

    public JSONObject toJson() {
        JSONObject header = new JSONObject();
        try {
            header.put("namespace",super.getNamespace());
            header.put("name",super.getName());
            header.put("messageId",messageId);
            header.put("dialogRequestId",dialogRequestId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return header;
    }
}
