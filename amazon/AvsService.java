package com.goertek.smartear.amazon;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AuthzConstants;
import com.amazon.identity.auth.device.shared.APIListener;
import com.goertek.smartear.activity.BackGroundService;
import com.goertek.smartear.utils.Constant;
import com.goertek.smartear.utils.DeviceUtils;

public class AvsService extends Service implements APIListener,
        AvsController.ConnectionAvailableListener{

    private static final boolean D = true;
    private static final String TAG = AvsService.class.getSimpleName();

    private static final int START_RECORD_AUDIO = 11;
    private static final int STOP_RECORD_AUDIO = 12;
    private static final int AVS_RECOED_TIME_OUT = 8*1000;//8 sec

    private final IBinder mBinder = new AvsBinder();

    private AuthSetup mAuthSetup;
    private AvsController mAvsController = null;
    private RecordHandler mRecordHandler = null;
    private AudioManager mAudioManager;

    private static boolean mIsAvs = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mAuthSetup = new AuthSetup(this,this);
        if(!AudioFocus.isInit())
            AudioFocus.init(this);
        if(mRecordHandler==null) {
            HandlerThread ht = new HandlerThread("record thread",Thread.NORM_PRIORITY);
            ht.start();
            mRecordHandler = new RecordHandler(ht.getLooper());
        }

        if(mAvsController!=null) {
            mAvsController.release();
            mAvsController = null;
        }

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mAvsController = new AvsController(this,mAuthSetup);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    /**
     * login in amazon to enable voice recognition
     */
    public void loginInAmazon() {
        mAuthSetup.loginInAmazon();
    }

    /**
     * access to avs
     */
    public void accessAvs() {
        if(DeviceUtils.isNetWorkAvailable(this)) {
            if (DeviceUtils.checkSDK())
                mAuthSetup.accessAvs();
            else
                amznVoiceHint(AVSConstants.HindCode.NOT_SUPPORTED_ERROR);
        }else {
            Toast.makeText(this,"no network available!",Toast.LENGTH_SHORT).show();
        }
    }

    public void startAudioRecord() {
        if (mIsAvs)
            amznVoiceHint(AVSConstants.HindCode.START_LISTENING);
        else
            Toast.makeText(this,"no connection to the AVS!",Toast.LENGTH_SHORT).show();
    }

    public void stopAudioRecord() {
        if(mAvsController!=null)
            mAvsController.stopRecording();
    }

    private void amznVoiceHint(final int hintCode) {
        if(mAudioManager.isBluetoothScoOn())
            mAudioManager.setBluetoothScoOn(false);
        try {
            AudioFocus.setFocus();
            AssetFileDescriptor afd = null;
            switch (hintCode) {
                case AVSConstants.HindCode.SAY_AGAIN_ERROR:
                    afd = getAssets().openFd("music/say_again.mp3");
                    break;
                case AVSConstants.HindCode.NOT_SUPPORTED_ERROR:
                    afd = getAssets().openFd("music/not_supported.mp3");
                    break;
                case AVSConstants.HindCode.START_LISTENING:
                    afd = getAssets().openFd("music/start.mp3");
                    break;
                case AVSConstants.HindCode.NO_AUDIO_RSP:
                    afd = getAssets().openFd("music/no_audio_rsp.mp3");
                    break;
            }

            if (null == afd) {
                Log.e(TAG, "amznVoiceHint: AssetFileDescriptor fd == null");
                return;
            }

            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.reset();
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepare();
            mediaPlayer.start();
            afd.close();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                    if (hintCode == AVSConstants.HindCode.START_LISTENING) {
                        if (D) Log.d(TAG, "START_LISTENING");
                       /* if(!mAudioManager.isBluetoothScoOn())
                            return;
                            */
                        // mAudioManager.setBluetoothScoOn(true);
                        // mRecordHandler.sendEmptyMessage(START_RECORD_AUDIO);

                        if (BackGroundService.isA2dpConnected()) {
                            if (!mAudioManager.isBluetoothScoAvailableOffCall()) {
                                if (D) Log.d(TAG, "not supported sco");
                                return;
                            }
                            Log.d(TAG, "start sco---------");
                            mAudioManager.startBluetoothSco();
                            registerReceiver(new BroadcastReceiver() {
                                @Override
                                public void onReceive(Context context, Intent intent) {
                                    int state = intent.getIntExtra(AudioManager
                                            .EXTRA_SCO_AUDIO_STATE, -1);
                                    if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                                        if (D) Log.d(TAG, "SCO_AUDIO_STATE_CONNECTED");
                                        //  mAudioManager.setBluetoothScoOn(true);
                                        mRecordHandler.sendEmptyMessage(START_RECORD_AUDIO);
                                        unregisterReceiver(this);
                                    }
                                }
                            }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
                        }
                    }
                    AudioFocus.abandonFocus();
                    mp.release();
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mp.release();
                    return false;
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "amznVoiceHint() error.");
            e.printStackTrace();
        }
    }


    public static boolean getAmazonLoginState() {
        return mIsAvs;
    }

    @Override
    public void onSuccess(Bundle bundle) {
        final String authzToken = bundle.getString(AuthzConstants.BUNDLE_KEY.TOKEN.val);
        Log.i(TAG, "authzToken: " + authzToken);

        if(authzToken!=null&&!authzToken.equals("")) {
            mAvsController.setupConnection(AvsService.this, authzToken);
        }
        /*
        new Thread(new Runnable() {
            @Override
            public void run() {

            }
        }).start();*/
    }

    @Override
    public void onError(AuthError authError) {
        mIsAvs = false;
        updateAmazonState(false);

    }

    private void updateAmazonState(boolean on) {
        LocalBroadcastManager localMng = LocalBroadcastManager.getInstance(this);
        Intent i = new Intent(Constant.ACTION_UPDATE_AMAZON_LOGIN_STATE);
        i.putExtra(Constant.EXTRA_AMAZON_LOGIN_STATE,on);

        localMng.sendBroadcast(i);
    }

    @Override
    public void onCntAvailable() {
        mIsAvs = true;
        updateAmazonState(true);
    }

    @Override
    public void onCntDisavailable() {
        mIsAvs = false;
        updateAmazonState(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mAvsController!=null) {
            mAvsController.release();
            mAvsController = null;
        }
    }

    public class AvsBinder extends Binder {
        public AvsService getAvsService() {
            return AvsService.this;
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STOP_RECORD_AUDIO:
                    stopAudioRecord();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }

        }
    };

    private class RecordHandler extends Handler {
        public RecordHandler() {
            super();
        }

        public RecordHandler(Callback callback) {
            super(callback);
        }

        public RecordHandler(Looper looper) {
            super(looper);
        }

        public RecordHandler(Looper looper, Callback callback) {
            super(looper, callback);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case START_RECORD_AUDIO:
                    if(mAvsController!=null) {
                        mHandler.sendEmptyMessageDelayed(STOP_RECORD_AUDIO,AVS_RECOED_TIME_OUT);
                        mAvsController.startRecording();
                    }
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    }

}
