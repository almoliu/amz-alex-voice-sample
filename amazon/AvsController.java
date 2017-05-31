package com.goertek.smartear.amazon;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static com.goertek.smartear.amazon.AVSConstants.TimeConstants.PING_AVS_SECS;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class AvsController implements RecordingRMSListener, RequestListener,
        DirectiveDispatcher,RecordingStateListener {

    private static final boolean D = true;
    private static final String TAG = AvsController.class.getSimpleName();

    private Timer mTimer = null;

    private Context mContext;

    private AvsClient mAvsClient;
    private AuthSetup mAuthSetup;

    private AvsAudioPlayer mPlayer;
    private AudioCapture mAudioCapture;

    private PingAvsTask mPingAvsTask = null;

    private BlockableDirectiveThread dependentDirectiveThread;
    private BlockableDirectiveThread independentDirectiveThread;

    private BlockingQueue<Directive> dependentQueue;
    private BlockingQueue<Directive> independentQueue;

    private Thread mAutoEndpoint = null;
    private static final int ENDPOINT_THRESHOLD = 5;
    private static final int ENDPOINT_SECONDS = 1;


    public AvsController(Context context,AuthSetup authSetup) {

        mContext = context;
        mPlayer = new AvsAudioPlayer(context,this);
        mAuthSetup = authSetup;

        mAudioCapture = new AudioCapture(context);

        dependentQueue = new LinkedBlockingDeque<>();
        independentQueue = new LinkedBlockingDeque<>();

        DirectiveEnqueuer directiveEnqueuer =
                new DirectiveEnqueuer(DialogRequestIdAuthority.getInstance(), dependentQueue,independentQueue);
        mAvsClient = new AvsClient(context,mPlayer,directiveEnqueuer);

        dependentDirectiveThread =
                new BlockableDirectiveThread(dependentQueue, this, "DependentDirectiveThread");
        independentDirectiveThread =
                new BlockableDirectiveThread(independentQueue, this, "IndependentDirectiveThread");

        independentDirectiveThread.start();
        dependentDirectiveThread.start();

    }

    public void setupConnection(ConnectionAvailableListener listener,String access) {
        if(D) Log.d(TAG,"setupConnection");
        mAvsClient.setAccessToken(access);
        if(mAvsClient.avsConnInit()) {

            listener.onCntAvailable();
            if (null != mTimer) {
                mTimer.cancel();
            }

            mTimer = new Timer();
            if(mPingAvsTask!=null) {
                mPingAvsTask.cancel();
                mPingAvsTask = null;
            }
            mPingAvsTask = new PingAvsTask(listener);
            mTimer.schedule(mPingAvsTask, PING_AVS_SECS, PING_AVS_SECS);
        }else {
            if(D) Log.d(TAG,"onCntDisavailable");
            listener.onCntDisavailable();
        }
    }

    public void startRecording() {
        mPlayer.handlePauseMusic();
        String dialogId = DialogRequestIdAuthority.getInstance().createNewDialogRequestId();
        InputStream inputStream = mAudioCapture.getAudioInputStream(this,this);
        mAvsClient.sendEvent(RequestMetaDataFactory.createSpeechRecognizerRecognizeEvent(
                dialogId, SpeechProfile.CLOSE_TALK, SpeechProfile.AUDIO_FORMAT.toString(),
                mPlayer.getPlaybackState(), mPlayer.getSpeechState(), mPlayer.getAlertsState(),
                mPlayer.getVolumeState()), this, inputStream);
    }

    public void sendRequest(JSONObject content, RequestListener listener) {
        mAvsClient.sendEvent(content, listener);
    }

    public void stopRecording() {
        mAudioCapture.stopCapture();
    }

    public void release() {
        if(mPingAvsTask!=null) {
            mPingAvsTask.cancel();
            mPingAvsTask = null;
        }
        if(dependentDirectiveThread!=null) {
            dependentDirectiveThread.realease();
            dependentDirectiveThread = null;
        }
        if(independentDirectiveThread!=null) {
            independentDirectiveThread.realease();
            independentDirectiveThread = null;
        }
        if(mAvsClient!=null) {
            mAvsClient.release();
            mAvsClient = null;
        }

        if(mPlayer!=null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void rmsChanged(int rms) {
        if(false) Log.d(TAG,"rms is-------"+rms);
        if ((rms == 0) || (rms > ENDPOINT_THRESHOLD)) {
            if (mAutoEndpoint != null) {
                mAutoEndpoint.interrupt();
                mAutoEndpoint = null;
            }
        } else if (rms < ENDPOINT_THRESHOLD) {
            if (mAutoEndpoint == null) {
                mAutoEndpoint = new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(ENDPOINT_SECONDS * 1000);
                            stopRecording();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                };
                mAutoEndpoint.start();
            }
        }

    }

    @Override
    public void recordingStarted() {

    }

    @Override
    public void recordingCompleted() {

    }

    @Override
    public void onRequestSuccess() {

    }

    @Override
    public void onRequestFailed() {

    }


    @Override
    public void dispatch(Directive directive) {

        if(D) Log.d(TAG,"dispatch...");
        String directiveNamespace = directive.getNamespace();

        String directiveName = directive.getName();

        if (DialogRequestIdAuthority.getInstance().isCurrentDialogRequestId(directive.getDialogRequestId())) {
            //   speechRequestAudioPlayerPauseController.dispatchDirective();
        }
        try {
            if (directiveNamespace.equals(AVSConstants.AVSAPIConstants.SpeechRecognizer.NAMESPACE)) {
                if(D) Log.d(TAG,"SpeechRecognizer");
                handleSpeechRecognizerDirective(directive);
            } else if (directiveNamespace.equals(AVSConstants.AVSAPIConstants.SpeechSynthesizer.NAMESPACE)) {
                if(D) Log.d(TAG,"SpeechSynthesizer");
                handleSpeechSynthesizerDirective(directive);
            } else if (directiveNamespace.equals(AVSConstants.AVSAPIConstants.AudioPlayer.NAMESPACE)) {
                if(D) Log.d(TAG,"AudioPlayer");
                handleAudioPlayerDirective(directive);
            } else if (directiveNamespace.equals(AVSConstants.AVSAPIConstants.Alerts.NAMESPACE)) {
                if(D) Log.d(TAG,"Alerts");
                handleAlertsDirective(directive);
            } else if (directiveNamespace.equals(AVSConstants.AVSAPIConstants.Speaker.NAMESPACE)) {
                if(D) Log.d(TAG,"Speaker");
                handleSpeakerDirective(directive);
            } else if (directiveNamespace.equals(AVSConstants.AVSAPIConstants.System.NAMESPACE)) {
                if(D) Log.d(TAG,"System");
                handleSystemDirective(directive);
            } else {
                throw new DirectiveHandlingException(DirectiveHandlingException.ExceptionType.UNSUPPORTED_OPERATION,
                        "No device side component to handle the directive.");
            }
        } catch (DirectiveHandlingException e) {
            sendExceptionEncounteredEvent(directive.getRawMessage(), e.getType(), e);
        } catch (Exception e) {
            sendExceptionEncounteredEvent(directive.getRawMessage(), DirectiveHandlingException.ExceptionType.INTERNAL_ERROR, e);
            throw e;
        }
    }

    private void handleSpeechRecognizerDirective(Directive directive) {
        if (directive.getName().equals(AVSConstants.AVSAPIConstants.SpeechRecognizer.Directives.ExpectSpeech.NAME)) {
            // If your device cannot handle automatically starting to listen, you must
            // implement a listen timeout event, as described here:
            // https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/rest/speechrecognizer-listentimeout-request
            //  notifyExpectSpeechDirective();
        }
    }
    private void handleSpeechSynthesizerDirective(Directive directive)
            throws DirectiveHandlingException {
        if (directive.getName().equals(AVSConstants.AVSAPIConstants.SpeechSynthesizer.Directives.Speak.NAME)) {
            mPlayer.handleSpeak((DirectiveSpeakPayload) directive.getPayload());
        }
    }

    private void handleAudioPlayerDirective(Directive directive) throws DirectiveHandlingException {
        String directiveName = directive.getName();
        if (directiveName.equals(AVSConstants.AVSAPIConstants.AudioPlayer.Directives.Play.NAME)) {
            mPlayer.handlePlayMusic((DirectiveAudioPlayerPayload)directive.getPayload());
            if(D) Log.d(TAG,"handlePlayMusic:"+directive.getPayload().toJson());
        } else if (directiveName.equals(AVSConstants.AVSAPIConstants.AudioPlayer.Directives.Stop.NAME)) {
              mPlayer.handleStopMusic();
        } else if (directiveName.equals(AVSConstants.AVSAPIConstants.AudioPlayer.Directives.ClearQueue.NAME)) {
            // player.handleClearQueue((AVSAPIConstants.AudioPlayer.Directives.ClearQueue)
            //       directive.getPayload());
        }

    }

    private void handleAlertsDirective(Directive directive) {
        String directiveName = directive.getName();
        if (directiveName.equals(AVSConstants.AVSAPIConstants.Alerts.Directives.SetAlert.NAME)) {
            /*
            SetAlert payload = (SetAlert) directive.getPayload();
            String alertToken = payload.getToken();
            ZonedDateTime scheduledTime = payload.getScheduledTime();
            AlertType type = payload.getType();

            if (alertManager.hasAlert(alertToken)) {
                AlertScheduler scheduler = alertManager.getScheduler(alertToken);
                if (scheduler.getAlert().getScheduledTime().equals(scheduledTime)) {
                    return;
                } else {
                    scheduler.cancel();
                }
            }

            Alert alert = new Alert(alertToken, type, scheduledTime);
            alertManager.add(alert);*/
        } else if (directiveName.equals(AVSConstants.AVSAPIConstants.Alerts.Directives.DeleteAlert.NAME)) {
          /*  DeleteAlert payload = (DeleteAlert) directive.getPayload();
            alertManager.delete(payload.getToken());*/
        }
    }

    private void handleSpeakerDirective(Directive directive) {
        String directiveName = directive.getName();
        if (directiveName.equals(AVSConstants.AVSAPIConstants.Speaker.Directives.SetVolume.NAME)) {
              mPlayer.handleSetVolume((DirectiveVolumePayload)directive.getPayload());
        } else if (directiveName.equals(AVSConstants.AVSAPIConstants.Speaker.Directives.AdjustVolume.NAME)) {
              mPlayer.handleAdjustVolume((DirectiveVolumePayload)directive.getPayload());
        } else if (directiveName.equals(AVSConstants.AVSAPIConstants.Speaker.Directives.SetMute.NAME)) {
              mPlayer.handleSetMute((DirectiveMutePayload)directive.getPayload());
        }
    }

    private void handleSystemDirective(Directive directive) {
        if (directive.getName().equals(AVSConstants.AVSAPIConstants.System.Directives.ResetUserInactivity.NAME)) {
            //  onUserActivity();
        }
    }

    private void sendExceptionEncounteredEvent(String directiveJson, DirectiveHandlingException.ExceptionType type,
                                               Exception e) {
/*
        sendRequest(RequestFactory.createSystemExceptionEncounteredEvent(directiveJson, type,
                e.getMessage(), player.getPlaybackState(), player.getSpeechState(),
                alertManager.getState(), player.getVolumeState()));
 */
    }

    private class PingAvsTask extends TimerTask {

        private ConnectionAvailableListener mListener;

        public  PingAvsTask(ConnectionAvailableListener listener) {
            mListener = listener;
        }

        @Override
        public void run() {
            if(mAvsClient==null)
                return;
            if (!mAvsClient.pingAVS()) {

                Log.w(TAG, "ping the AVS error, create a new connection!");
                mListener.onCntDisavailable();
                mAuthSetup.accessAvs();

            }else {
                Log.d(TAG, "pingAVS successful!");
                mListener.onCntAvailable();
            }
        }
    }

    interface ConnectionAvailableListener {
        void onCntAvailable();
        void onCntDisavailable();
    }

}
