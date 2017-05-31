package com.goertek.smartear.amazon;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class DirectiveSpeakPayload extends Payload implements AttachedContentPayload {

    private  String token;
    private  String mUrl;
    private  String mFormat;

    private InputStream attachedContent;

    public DirectiveSpeakPayload(String url, String format, String token) {
        this.token = token;
        this.mFormat = format;
        this.mUrl = url;
    }

    public String getToken() {
        return this.token;
    }

    public String getUrl() {
        return this.mUrl;
    }

    public String getFormat() {
        return this.mFormat;
    }

    public void setUrl(String url) {
        // The format we get from the server has the audioContentId as "cid:%CONTENT_ID%" whereas
        // the actual Content-ID HTTP Header value is "%CONTENT_ID%".
        // This normalizes that
        this.mUrl = url.substring(4);
    }

    public void setFormat(String format) {
        this.mFormat = format;
    }

    public void setToken(String token) {
        this.token = token;
    }


    @Override
    public JSONObject toJson() {

        JSONObject speechState = new JSONObject();
        try {
            speechState.put("token", token);
            speechState.put("url", mUrl);
            speechState.put("format", mFormat);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return speechState;
    }

    @Override
    public boolean requiresAttachedContent() {
        return !hasAttachedContent();
    }

    @Override
    public boolean hasAttachedContent() {
        return attachedContent!=null;
    }

    @Override
    public String getAttachedContentId() {
        return mUrl;
    }

    @Override
    public InputStream getAttachedContent() {
        return attachedContent;
    }

    @Override
    public void setAttachedContent(String contentId, InputStream attachmentContent) {
        if (getAttachedContentId().equals(contentId)) {
            this.attachedContent = attachmentContent;
        } else {
            throw new IllegalArgumentException(
                    "Tried to add the wrong audio content to a Speak directive. This cid: "
                            + getAttachedContentId() + " other cid: " + contentId);
        }
    }
}

