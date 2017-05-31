package com.goertek.smartear.amazon;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class RequestBodyFactory {
    private static final boolean D = true;
    private static final String TAG = RequestBodyFactory.class.getSimpleName();

    private static final String METADATA_NAME = "metadata";
    private static final String AUDIO_NAME = "audio";


    public static RequestBody createRequestBody(String json, byte[]bytes, int len) {

        MediaType META_TYPE_MARKDOWN
                = MediaType.parse("application/json;charset=UTF-8");
        MediaType AUDIO_TYPE_MARKDOWN
                = MediaType.parse("application/octet-stream");

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM).addPart(Headers.of("Content-Disposition", "form-data; name=\"" + METADATA_NAME + "\""),
                        RequestBody.create(META_TYPE_MARKDOWN, json))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"" + AUDIO_NAME + "\""),
                        RequestBody.create(AUDIO_TYPE_MARKDOWN, bytes,0,len))
                .build();

        return requestBody;
    }

    public static RequestBody createRequestBody(String json,File file) {

        MediaType META_TYPE_MARKDOWN
                = MediaType.parse("application/json;charset=UTF-8");
        MediaType AUDIO_TYPE_MARKDOWN
                = MediaType.parse("application/octet-stream");

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"" + METADATA_NAME +"\""),
                        RequestBody.create(META_TYPE_MARKDOWN, json))
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"" + AUDIO_NAME + "\""),
                        RequestBody.create(AUDIO_TYPE_MARKDOWN, file))
                .build();

        return requestBody;
    }

    public static RequestBody createRequestBody(String json, final InputStream inputStream) {

        MediaType META_TYPE_MARKDOWN
                = MediaType.parse("application/json;charset=UTF-8");
        final MediaType AUDIO_TYPE_MARKDOWN
                = MediaType.parse("application/octet-stream");

        RequestBody requestBody = null;

        try {
            requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"" + METADATA_NAME + "\""),
                            RequestBody.create(META_TYPE_MARKDOWN, json))
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"" + AUDIO_NAME + "\""),
                            RequestBody.create(AUDIO_TYPE_MARKDOWN, IOUtils.toByteArray(inputStream)))
                    .build();
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            IOUtils.closeQuietly(inputStream);
        }

        return requestBody;
    }

    public static RequestBody createRequestBodyWithoutAudio(String content) {

        MediaType META_TYPE_MARKDOWN
                = MediaType.parse("application/json;charset=UTF-8");
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"" + METADATA_NAME + "\""),
                        RequestBody.create(META_TYPE_MARKDOWN, content))
                .build();
        return requestBody;
    }
}
