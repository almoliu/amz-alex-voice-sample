package com.goertek.smartear.amazon;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class Directive extends Message{
    private final String mDialogRequestId;
    public Directive(Header header,Payload payload,String rawMessage) {
        super(header,payload,rawMessage);
        mDialogRequestId = extractDialogRequestId();
    }

    public String getDialogRequestId() {
        return mDialogRequestId;
    }


    private String extractDialogRequestId() {
        if (header instanceof Msg2DialogIdHeader) {
            Msg2DialogIdHeader dialogRequestIdHeader = (Msg2DialogIdHeader) header;
            return dialogRequestIdHeader.getDialogRequestId();
        } else {
            return null;
        }
    }
}

