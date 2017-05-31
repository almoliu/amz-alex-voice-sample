package com.goertek.smartear.amazon;

import java.util.UUID;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class DialogRequestIdAuthority {
    private static final DialogRequestIdAuthority instance;

    static {
        instance = new DialogRequestIdAuthority();
    }

    private String currentDialogRequestId;

    private DialogRequestIdAuthority() {
    }

    public static DialogRequestIdAuthority getInstance() {
        return instance;
    }

    public String createNewDialogRequestId() {
        currentDialogRequestId = UUID.randomUUID().toString();
        return currentDialogRequestId;
    }

    public boolean isCurrentDialogRequestId(String candidateRequestId) {
        return currentDialogRequestId != null && currentDialogRequestId.equals(candidateRequestId);
    }

}
