package com.goertek.smartear.amazon;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.goertek.smartear.amazon.AVSConstants.EndPointConstants.AVS_EVENT_POINT;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class AvsClient {

    private static final boolean D = true;
    private static final String TAG = AvsClient.class.getSimpleName();

    private final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType OCTET_TYPE = MediaType.parse("application/octet-stream");


    private final String BOUNDARY = "this-is-a-boundary";
    private final String AUTHORIZATION = "authorization";
    private final String BEARER = "Bearer ";
    private final String CONTENT_TYPE = "content-type";
    private final String MULTI_DATA = "multipart/form-data; boundary=" + BOUNDARY;
    private final String DISPOSITION = "Content-Disposition";
    private final String META_DATA = "form-data; name=\"metadata\"";
    private final String AUDIO_DATA = "form-data; name=\"audio\"";

    private final String AMZN_RSP_NAME = "/amzn_response.mp3";
    private final String AMZN_PLAYBACK_NAME = "/amzn_playback";
    private final String STR_DOT_MP3 = ".mp3";

    private String mAccessToken;
    private AvsAudioPlayer mPlayer;
    private Context mContext;


    private MultipartParser mRequestResponseParser;
    private RequestThread mRequestThread;

    private static final BlockingQueue<AvsRequest> mRequestQueue = new LinkedBlockingDeque<>();

    private OkHttpClient mOKHttpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.MINUTES)
            .readTimeout(60, TimeUnit.MINUTES)
            .writeTimeout(60, TimeUnit.SECONDS).build();

    public AvsClient(Context context,AvsAudioPlayer player, MultipartParserConsumer consumer) {
        mContext = context;
        mPlayer = player;
        mRequestResponseParser = new MultipartParser(consumer);
        mRequestThread = new RequestThread(mRequestQueue);
        mRequestThread.start();
    }

    public void setAccessToken(String token) {
        mAccessToken = token;
    }

    public boolean pingAVS() {
        boolean ret = false;
        Response pingRsp = null;
        Log.d(TAG, "pingAVS()");
        try {
            if (null == mOKHttpClient) {
                Log.e(TAG, "mClient is null!");
                return false;
            }

            Request request = new Request.Builder().addHeader(AUTHORIZATION, BEARER + mAccessToken)
                    .url(AVSConstants.EndPointConstants.AVS_PING_POINT)
                    .build();
            Log.i(TAG, "[http call] ping avs request");
            pingRsp = mOKHttpClient.newCall(request).execute();
            if (pingRsp.isSuccessful()) {
                Log.i(TAG, "pingRsp.isSuccessful() is true, " + pingRsp.code());
               ret = true;
            } else {
                Log.e(TAG, "pingRsp.isSuccessful() is false, " + pingRsp.code());
                ret = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "pingAVS error.");
            e.printStackTrace();
            return false;
        } finally {
            if(pingRsp!=null) {
                pingRsp.close();
                pingRsp = null;
            }
        }
        return  ret;
    }

    public synchronized boolean avsConnInit() {
        if(D) Log.d(TAG,"avsConnInit");
        return setupDownChannel() && sendSynchronizeStateEvent();
    }

    public void sendEvent(JSONObject content, RequestListener listener) {
        if(D) Log.d(TAG,"content is:\n"+content.toString());
        AvsRequest avsRequest = new AvsRequest(listener,HttpMethods.POST,AVS_EVENT_POINT,
                RequestBodyFactory.createRequestBodyWithoutAudio(content.toString()),
                mRequestResponseParser);
        enqueueRequest(avsRequest);
    }

    public void sendEvent(JSONObject content,RequestListener listener,InputStream inputStream) {

        if(D) Log.d(TAG,"content is:\n"+content.toString());

        AvsRequest avsRequest = new AvsRequest(listener,HttpMethods.POST,AVS_EVENT_POINT,
                RequestBodyFactory.createRequestBody(content.toString(),inputStream),mRequestResponseParser);
        enqueueRequest(avsRequest);
    }

    public void release() {
        if(mRequestThread!=null) {
            mRequestThread.release();
            mRequestThread = null;
        }
    }

    private synchronized boolean setupDownChannel() {

        Request request = new Request.Builder() .header("scheme", "https")
                .addHeader(AUTHORIZATION, BEARER + mAccessToken)
                .url(AVSConstants.EndPointConstants.AVS_DIRECTIVE_POINT)
                .build();
        try {
            Response downRsp = mOKHttpClient.newCall(request).execute();
            if (downRsp.isSuccessful()) {
                Log.i(TAG, "downRsp.isSuccessful() is true, " + downRsp.code());
                return true;
            } else {
                Log.e(TAG, "downRsp.isSuccessful() is false, " + downRsp.code());
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private synchronized boolean sendSynchronizeStateEvent() {

        Request request = new Request.Builder() .header("scheme", "https")
                .addHeader(AUTHORIZATION, BEARER + mAccessToken)
                .url(AVS_EVENT_POINT)
                .post(RequestBodyFactory.createRequestBodyWithoutAudio(RequestMetaDataFactory
                        .createSystemSynchronizeStateEvent(mPlayer.getPlaybackState(),
                                mPlayer.getSpeechState(),
                                mPlayer.getAlertsState(),
                                mPlayer.getVolumeState()).toString()))
                .build();
        try {
            Response synRsp = mOKHttpClient.newCall(request).execute();
            if (synRsp.isSuccessful()) {
                Log.i(TAG, "downRsp.isSuccessful() is true, " + synRsp.code());
                return true;
            } else {
                Log.e(TAG, "downRsp.isSuccessful() is false, " + synRsp.code());
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }

    private void enqueueRequest(AvsRequest request) {
        if (!mRequestQueue.offer(request)) {
            if(D) Log.d(TAG,"wrong request....");
        }
    }

    private void  doRequest(AvsRequest avsRequest) {

        Request request = null;
        Response response = null;
        ResponseBody body = null;
        String boundary = null;

        synchronized (this) {
            switch (avsRequest.getMethod()) {
                case POST:
                    request = new Request.Builder()
                            .header("scheme", "https")
                            .addHeader("authorization", "Bearer " + mAccessToken)
                            .url(avsRequest.getEndPoint()).post(avsRequest.getRequestBody()).build();
                    break;
                case GET:
                    request = new Request.Builder()
                            .header("scheme", "https")
                            .addHeader("authorization", "Bearer " + mAccessToken)
                            .url(avsRequest.getEndPoint()).build();
                    break;
                default:
                    break;
            }


            try {
                response = mOKHttpClient.newCall(request).execute();
                Log.d(TAG, "response code is:\n" + response.code());
                RequestListener requestListener = avsRequest.getResultListener();
                if (!response.isSuccessful()) {
                   amznVoiceHint(AVSConstants.HindCode.SAY_AGAIN_ERROR);
                    if (requestListener != null)
                        avsRequest.getResultListener().onRequestFailed();
                        response.close();
                    return;
                }


                if(requestListener!=null)
                    requestListener.onRequestSuccess();

                if (response.code() == 204) {
                    response.close();
                    return;
                }

                boundary = HttpHeaders.getHeaderParameter(response.headers().toString(),
                        HttpHeaders.Parameters.BOUNDARY);

                if (boundary == null || boundary.equals("")) {
                    throw new IllegalStateException(
                            "A boundary is missing from the response headers. "
                                    + "Unable to parse multipart stream.");
                }

                body = response.body();

            } catch (IOException e) {
                e.printStackTrace();
            }

            MultipartParser multipartParser = avsRequest.getMultipartParser();

            try {
                if(multipartParser != null) {
                    //if(D) Log.d(TAG,"msg is:\n"+new String(body.bytes()));
                    if( body != null)
                        multipartParser.parseStream(body.byteStream(), boundary);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {

                if(D) Log.d(TAG,"close the response...");
                if(response!=null)
                    response.close();
                if(body!=null)
                    body.close();
            }
        }
    }

    private void amznVoiceHint(final int hintCode) {
        try {
            AudioFocus.setFocus();
            AssetFileDescriptor afd = null;
            switch (hintCode) {
                case AVSConstants.HindCode.SAY_AGAIN_ERROR:
                    afd = mContext.getAssets().openFd("music/say_again.mp3");
                    break;
                case AVSConstants.HindCode.NOT_SUPPORTED_ERROR:
                    afd = mContext.getAssets().openFd("music/not_supported.mp3");
                    break;
                case AVSConstants.HindCode.START_LISTENING:
                    afd = mContext.getAssets().openFd("music/ding.mp3");
                    break;
                case AVSConstants.HindCode.NO_AUDIO_RSP:
                    afd = mContext.getAssets().openFd("music/no_audio_rsp.mp3");
                    break;
            }

            if (null == afd) {
                Log.e(TAG, "amznVoiceHint: AssetFileDescriptor fd == null");
                return;
            }

            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepare();
            mediaPlayer.start();
            afd.close();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                    AudioFocus.abandonFocus();
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mp.release();
                    return true;
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "amznVoiceHint() error.");
            e.printStackTrace();
        }
    }

    private class RequestThread extends Thread {

        private boolean is_looping = true;
        private BlockingQueue<AvsRequest> queue;

        public RequestThread(BlockingQueue<AvsRequest> queue) {
            this.queue = queue;
            setName(this.getClass().getSimpleName());
        }

        @Override
        public void run() {
            while (is_looping) {
                try {
                    AvsRequest request = queue.take();
                    doRequest(request);
                }catch (InterruptedException e) {

                }
            }
        }

        public void release() {
            is_looping = false;
        }
    }
}





