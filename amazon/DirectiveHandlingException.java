package com.goertek.smartear.amazon;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class DirectiveHandlingException extends Exception {
    private ExceptionType type;

    public DirectiveHandlingException(ExceptionType type, String message) {
        super(message);
        this.type = type;
    }

    public ExceptionType getType() {
        return type;
    }

    public enum ExceptionType {
        UNEXPECTED_INFORMATION_RECEIVED,
        UNSUPPORTED_OPERATION,
        INTERNAL_ERROR;
    }
}
