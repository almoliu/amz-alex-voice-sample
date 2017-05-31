package com.goertek.smartear.amazon;

import java.io.InputStream;

/**
 * Created by almo.liu on 2016/11/17.
 */

public interface AttachedContentPayload {
    /**
     * Returns whether or not this payload requires content to be attached. False means either it
     * never required content, or that it has content.
     */
    boolean requiresAttachedContent();

    /**
     * Returns whether or not this payload has content attached.
     */
    boolean hasAttachedContent();

    /**
     * Returns the content id for the required attached content.
     */
    String getAttachedContentId();

    /**
     * Returns the attached content.
     */
    InputStream getAttachedContent();

    /**
     * Attaches the given attachment content if the given content id matches the required content
     * id.
     *
     * @param contentId
     *            - content id of attachementContent
     * @param attachmentContent
     *            - content to attach
     */
    void setAttachedContent(String contentId, InputStream attachmentContent);
}
