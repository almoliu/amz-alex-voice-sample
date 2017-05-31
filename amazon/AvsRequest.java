package com.goertek.smartear.amazon;

import okhttp3.RequestBody;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class AvsRequest {
    private HttpMethods mMethod;
    private String mEndPoint;
    private RequestBody mRequestBody;
    private RequestListener mResultListener;

    private MultipartParser mMultipartParser;



    public AvsRequest(RequestListener listener,HttpMethods method,String endPoint,MultipartParser
            parser) {
        this(listener,method,endPoint,null,parser);
    }


    public AvsRequest(RequestListener listener,HttpMethods method,String endPoint, RequestBody
            body,MultipartParser parser) {
        mMethod = method;
        mEndPoint = endPoint;
        mRequestBody = body;
        mMultipartParser = parser;
        mResultListener = listener;
    }

    public HttpMethods getMethod() {
        return  mMethod;
    }

    public String getEndPoint() {
        return mEndPoint;
    }

    public RequestBody getRequestBody() {
        return mRequestBody;
    }

    public MultipartParser getMultipartParser() {
        return mMultipartParser;
    }

    public RequestListener getResultListener() {
        return mResultListener;
    }
}
