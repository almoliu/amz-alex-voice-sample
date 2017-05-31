package com.goertek.smartear.amazon;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class HttpHeaders {
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String CONTENT_ID = "Content-ID";
    public static final String AUTHORIZATION = "Authorization";

    public static class Parameters {
        public static final String BOUNDARY = "boundary";
        public static final String CHARSET = "charset";
    }

    public static String getHeaderParameter(final String headerValue, final String key) {
        if ((headerValue == null) || (key == null)) {
            return null;
        }
        String[] parts = headerValue.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith(key)) {
                return part.substring(key.length() + 1).replaceAll("(^\")|(\"$)", "").trim();
            }
        }
        return "";
    }
}
