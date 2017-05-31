package com.goertek.smartear.amazon;

import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class AudioCapture {
    private static final boolean D = true;
    private static final String TAG = AudioCapture.class.getSimpleName();

    private Context mContext;
    private MicroPhoneLine mMicPhoneLine;
    private Handler mRecordHandler;
    private AudioBufferRunnable mAudioBufRunnable;

    public AudioCapture (Context context) {
        mContext = context;
        int bufSize = AudioRecord.getMinBufferSize(MicroPhoneLine.SAMPLE_RATE_IN_HZ,
                MicroPhoneLine.CHANNEL_CONFIG,
                MicroPhoneLine.AUDIO_FORMAT);
        mMicPhoneLine = new MicroPhoneLine(MediaRecorder.AudioSource.DEFAULT,MicroPhoneLine
                .SAMPLE_RATE_IN_HZ,MicroPhoneLine.CHANNEL_CONFIG,MicroPhoneLine.AUDIO_FORMAT,bufSize,context);
        if(mMicPhoneLine.getState()!=AudioRecord.STATE_INITIALIZED)
            Log.i(TAG,"invalid AudioRecord initialization.");

        HandlerThread ht = new HandlerThread("recording thread",Thread.NORM_PRIORITY);
        ht.start();
        mRecordHandler = new Handler(ht.getLooper());

    }

    public InputStream getAudioInputStream(final RecordingStateListener stateListener,
                                           final RecordingRMSListener rmsListener) {
        mMicPhoneLine.startRecording();
        PipedInputStream inputStream = new PipedInputStream(mMicPhoneLine.getBufferSize()*2);
        try {
            if(mAudioBufRunnable!=null)
                mAudioBufRunnable.closePipedOutputStream();
            mAudioBufRunnable = new AudioBufferRunnable(inputStream, stateListener, rmsListener);
            mRecordHandler.post(mAudioBufRunnable);
        } catch (IOException e) {
            mMicPhoneLine.stop();
            e.printStackTrace();
        }
        return inputStream;
    }

    public void stopCapture() {
        mMicPhoneLine.stop();
    }


    private class AudioBufferRunnable implements Runnable {

        private final AudioStateOutputStream audioStateOutputStream;

        public AudioBufferRunnable(PipedInputStream inputStream,
                                   RecordingStateListener recordingStateListener,
                                   RecordingRMSListener rmsListener) throws IOException {
            audioStateOutputStream =
                    new AudioStateOutputStream(inputStream, recordingStateListener, rmsListener);
        }

        @Override
        public void run() {

            if (D) Log.d(TAG, "startRecording...");
            /*
            int mCounter = 0;// first record counter, after >5(about 1s) detect no sound
            int mTimeCounter = 0; // interval counter, 1 means about 0.04s
            int mNoSoundCounter = 0; // >5 means no sound, then stop record
            */

            while (mMicPhoneLine.isOpen()) {
                 copyAudioBytesFromInputToOutput();
                /*
                mTimeCounter++;
                mCounter++;

                if (mTimeCounter > 4 && mCounter > 40) {
                    if(sound)
                        mNoSoundCounter = 0;
                    else
                        mNoSoundCounter++;
                }

                if (mNoSoundCounter > 3) {
                    if(D) Log.d(TAG,"stop micphoneline----");
                   // mMicPhoneLine.stop();
                }*/
            }
            closePipedOutputStream();
            if (D) Log.d(TAG, "stop recording...");
        }

        private boolean copyAudioBytesFromInputToOutput() {

            byte[] data = new byte[mMicPhoneLine.getBufferSize() / 5];
            int numBytesRead = mMicPhoneLine.read(data, 0, data.length);

            try {
                if (numBytesRead > 0) {
                    audioStateOutputStream.write(data, 0, numBytesRead);
                    /*
                    File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                            + "/temp/");
                    if (!path.exists()) {
                        path.mkdirs();
                    }
                    File recordingFile = null;
                    recordingFile = File.createTempFile("recording", ".pcm", path);
                    FileOutputStream fileOutputStream = new FileOutputStream(recordingFile);
                    fileOutputStream.write(data,0,numBytesRead);
                    MediaPlayer mediaPlayer  = new MediaPlayer();
                    mediaPlayer.reset();
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mediaPlayer.setDataSource(mContext, Uri.fromFile(recordingFile));
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    */
/*
                    long sum = 0;
                    for (int i = 0; i < data.length; i++) {
                        sum += Math.abs(data[i]);
                    }
                    double rawAmplitude = sum / (double) numBytesRead;


                    if (rawAmplitude < 33.3 && rawAmplitude > 25) {
                        Log.d(TAG, "no sound dectected");
                        return false;
                    } else if (rawAmplitude > 33.3) {
                        Log.d(TAG, "sound dectected");
                        return true;
                    }
*/
                }

            } catch (IOException e) {
                mMicPhoneLine.stop();
            }
            return false;
        }

        private void closePipedOutputStream() {
            try {
                audioStateOutputStream.close();
            } catch (IOException e) {

            }
        }
    }
}
