package com.goertek.smartear.amazon;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.os.Message;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class AvsAudioPlayer implements MediaPlayer.OnCompletionListener {

    private static final boolean D = true;
    private static final String TAG = AvsAudioPlayer.class.getSimpleName();

    // How long the thread should block on waiting for audio to finish playing
    private static final int TIMEOUT_IN_MS = 1000;

    private Context mContext;

    private MediaPlayer mMediaPlayer;
    private MediaPlayer mSpeaker;
    private AvsController mAvsController;

    private final BlockingQueue<SpeakItem> mSpeakQueue;
    private final Queue<AudioItem> mAudioQueue;

    private int mCurrentVolume = 50;

    private boolean mCurrentlyMuted = false;
    private volatile AlertState alertState = AlertState.FINISHED;
    private volatile SpeechState speechState = SpeechState.FINISHED;
    private volatile AudioPlayerState audioPlayerState = AudioPlayerState.IDLE;

    private String latestStreamToken = "";
    private String latestToken = "";

    // Object on which to lock
    private final Object playLock = new Object();
    private SpeakThread mSpeakThread;
    private MusicHandler mMusicHandler;

    private AudioManager mAudioManager;

    public AvsAudioPlayer(Context context,AvsController avsController) {

        mContext = context;
        mAvsController = avsController;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mSpeakQueue = new LinkedBlockingDeque<>();
        mAudioQueue = new LinkedList<>();

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mSpeaker = new MediaPlayer();
        mSpeakThread = new SpeakThread();
        mSpeakThread.start();
        HandlerThread handlerThread = new HandlerThread("music handler");
        handlerThread.start();
        mMusicHandler = new MusicHandler(handlerThread.getLooper());
        if(!AudioFocus.isInit())
            AudioFocus.init(context);
    }

    public void  handleSpeak(DirectiveSpeakPayload speak) {
        if(D) Log.d(TAG,"handlerSpeak...");
        SpeakItem speakItem = new SpeakItem(speak.getToken(), speak.getAttachedContent());
        mSpeakQueue.add(speakItem);
    }

    public void handlePlayMusic(DirectiveAudioPlayerPayload music) {
        AudioItem audioItem = new AudioItem(music.getToken(),music.getUrl());
        mAudioQueue.add(audioItem);
    }

    public void handleSetVolume(DirectiveVolumePayload volumePayload) {
        mCurrentVolume = (int) volumePayload.getVolume();
        Log.d(TAG,"volume is:"+mCurrentVolume);
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if(D) Log.d(TAG,"max is"+audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,mCurrentVolume/10,AudioManager.FLAG_SHOW_UI);
        resumeMusic();
    }

    public void handleAdjustVolume(DirectiveVolumePayload volumePayload) {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int vol = (int) volumePayload.getVolume();
        if(vol<0) {
            mCurrentVolume -= 10;
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI);
        }else {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI);
            mCurrentVolume += 10;
        }
        resumeMusic();
    }

    public void handleSetMute(DirectiveMutePayload mutePayload) {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if(mutePayload.getMute()) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI);
            mCurrentlyMuted = true;
        }else {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE,
                    AudioManager.FLAG_SHOW_UI);
            mCurrentlyMuted = false;
        }
        resumeMusic();
    }

    public void handleStopMusic() {
        stopMusic();
    }

    public void handlePauseMusic() {
        pauseMusic();
    }

    public PlaybackStatePayload getPlaybackState() {
        long offset = 0;
        return new PlaybackStatePayload(latestStreamToken, offset, audioPlayerState.toString());
    }

    public SpeechStatePayload getSpeechState() {
        String contentId = latestToken;
        return new SpeechStatePayload(contentId, getPlayerPosition(), speechState.name());
    }

    public VolumeStatePayload getVolumeState() {
        return new VolumeStatePayload(getVolume(), isMuted());
    }

    public AlertsStatePayload getAlertsState() {
        return  new AlertsStatePayload();
    }

    public long getVolume() {
        return mCurrentVolume;
    }

    public boolean isMuted() {
        return mCurrentlyMuted;
    }

    public boolean isSpeaking() {
        return speechState == SpeechState.PLAYING;
    }

    public boolean isPlaying() {
        return audioPlayerState==AudioPlayerState.PLAYING;
    }

    public boolean isPaused() {
        return audioPlayerState == AudioPlayerState.PAUSED;
    }

    public void release() {
        if(mMediaPlayer!=null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if(mSpeaker!= null) {
            mSpeaker.release();
            mSpeaker = null;
        }

        if(mSpeakThread!=null) {
            mSpeakThread.release();
        }
    }

    private void startSpeak(final  SpeakItem speak) {
        //   notifyAlexaSpeechStarted();
        if(D) Log.d(TAG,"startSpeak...");
        if(!mAudioManager.isBluetoothScoOn()) {
        //    mAudioManager.setBluetoothScoOn(false);
            mAudioManager.stopBluetoothSco();
        }
        speechState = SpeechState.PLAYING;
        latestToken = speak.getToken();
        mAvsController.sendRequest(RequestMetaDataFactory.createSpeechSynthesizerSpeechStartedEvent
                (speak.getToken()),null);
        //  interruptAlertsAndContent();
        try {
            InputStream inpStream = speak.getAudio();
            if (inpStream == null)
                Log.d(TAG, "inputStream null...");
            File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/temp/");
            if (!path.exists()) {
                path.mkdirs();
            }
            File recordingFile = null;
            recordingFile = File.createTempFile("recording", ".pcm", path);
            FileOutputStream fileOutputStream = new FileOutputStream(recordingFile);
            fileOutputStream.write(IOUtils.toByteArray(inpStream));
            play(recordingFile);
            while (inpStream.available() > 0) {
                playLock.wait(TIMEOUT_IN_MS);
            }
        } catch ( IOException e) {
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void finishedAudioItem() {
        mAvsController.sendRequest(RequestMetaDataFactory.createPlaybackFinishedEvent
                (latestStreamToken),null);
        mMusicHandler.sendEmptyMessage(0);
    }

    private void finishedSpeechItem() {
        speechState = SpeechState.FINISHED;
        mAvsController.sendRequest(RequestMetaDataFactory
                .createSpeechSynthesizerSpeechFinishedEvent(latestToken),null);
        //     notifyAlexaSpeechFinished();
    }

    private  void play(File file) {
        synchronized (playLock) {
            try {
                AudioFocus.setFocus();
                mSpeaker.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if(D) Log.d(TAG,"speak completed...");
                        AudioFocus.abandonFocus();
                        finishedSpeechItem();
                        mMusicHandler.sendEmptyMessage(0);
                    }
                });
                mSpeaker.reset();
                mSpeaker.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mSpeaker.setDataSource(mContext, Uri.fromFile(file));
                mSpeaker.prepare();
                mSpeaker.start();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                file.delete();
            }
        }
    }

    private  void play(String url) {
        synchronized (playLock) {
            try {
                AudioFocus.setFocus();
                mAvsController.sendRequest(RequestMetaDataFactory.createPlaybackStartedEvent
                        (latestStreamToken),null);
                mMediaPlayer.reset();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(url);
                mMediaPlayer.prepare();
                audioPlayerState = AudioPlayerState.PLAYING;
                mMediaPlayer.start();

            } catch (IOException e) {
                e.printStackTrace();
                audioPlayerState = AudioPlayerState.IDLE;
            }
        }
    }

    private void stopMusic() {
        if(mAudioQueue !=null&& mAudioQueue.size()>0) {
            mAudioQueue.clear();
        }
        mMediaPlayer.stop();
        mAvsController.sendRequest(RequestMetaDataFactory.createPlaybackStopEvent
                (latestStreamToken),null);
        audioPlayerState = AudioPlayerState.STOPPED;
    }
    private void pauseMusic() {
        if(isPlaying()) {
            if(D) Log.d(TAG,"pause");
            mMediaPlayer.pause();
            mAvsController.sendRequest(RequestMetaDataFactory.createPlaybackPausedEvent
                    (latestStreamToken),null);
            audioPlayerState = AudioPlayerState.PAUSED;
        }
    }

    private void resumeMusic() {
        if(isPaused()) {
            mMediaPlayer.start();
            audioPlayerState = AudioPlayerState.PLAYING;
            mAvsController.sendRequest(RequestMetaDataFactory.createPlaybackResumedEvent
                    (latestStreamToken),null);
        }
    }

    private synchronized long getPlayerPosition() {
        long offsetInMilliseconds = 0;
        if (mSpeaker != null) {
            offsetInMilliseconds = mSpeaker.getCurrentPosition();
        }
        return offsetInMilliseconds;
    }



    public boolean isAlarming() {
        return alertState == AlertState.PLAYING;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        AudioFocus.abandonFocus();
        audioPlayerState = AudioPlayerState.IDLE;
        finishedAudioItem();
    }

    private enum SpeechState {
        PLAYING,
        FINISHED
    }

    private enum AlertState {
        PLAYING,
        INTERRUPTED,
        FINISHED
    }

    private enum AudioPlayerState {
        IDLE,
        PLAYING,
        PAUSED,
        FINISHED,
        STOPPED,
        BUFFER_UNDERRUN
    }

    private class SpeakThread extends Thread {

        private boolean is_looping = true;
        @Override
        public void run() {
            while(is_looping) {
                try {
                    SpeakItem speakItem = mSpeakQueue.take();
                    startSpeak(speakItem);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void release() {
            is_looping = true;
            Thread.interrupted();
        }
    }

    private class MusicHandler extends Handler {

        public MusicHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            if(mAudioQueue.size()>0) {
                AudioItem audioItem = mAudioQueue.poll();
                latestStreamToken = audioItem.getToken();
                String url = audioItem.getAudioUrl();
                play(url);
            }else {
                resumeMusic();
            }
        }
    }

}
