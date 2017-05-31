package com.goertek.smartear.amazon;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.util.Log;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class MicroPhoneLine extends AudioRecord {

    public static final int SAMPLE_RATE_IN_HZ = 16000;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private boolean is_open;
    private int mBufferSizeInBytes;
    private Context mContext;
    private AudioManager mAudioManager;

    /**
     * Class constructor.
     * Though some invalid parameters will result in an {@link IllegalArgumentException} exception,
     * other errors do not.  Thus you should call {@link #getState()} immediately after construction
     * to confirm that the object is usable.
     *
     * @param audioSource       the recording source.
     *                          See {@link MediaRecorder.AudioSource} for the recording source definitions.
     * @param sampleRateInHz    the sample rate expressed in Hertz. 44100Hz is currently the only
     *                          rate that is guaranteed to work on all devices, but other rates such as 22050,
     *                          16000, and 11025 may work on some devices.
     * @param channelConfig     describes the configuration of the audio channels.
     *                          See {@link AudioFormat#CHANNEL_IN_MONO} and
     *                          {@link AudioFormat#CHANNEL_IN_STEREO}.  {@link AudioFormat#CHANNEL_IN_MONO} is guaranteed
     *                          to work on all devices.
     * @param audioFormat       the format in which the audio data is to be returned.
     *                          See {@link AudioFormat#ENCODING_PCM_8BIT}, {@link AudioFormat#ENCODING_PCM_16BIT},
     *                          and {@link AudioFormat#ENCODING_PCM_FLOAT}.
     * @param bufferSizeInBytes the total size (in bytes) of the buffer where audio data is written
     *                          to during the recording. New audio data can be read from this buffer in smaller chunks
     *                          than this size. See {@link #getMinBufferSize(int, int, int)} to determine the minimum
     *                          required buffer size for the successful creation of an AudioRecord instance. Using values
     *                          smaller than getMinBufferSize() will result in an initialization failure.
     * @throws IllegalArgumentException
     */
    public MicroPhoneLine(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat,
                          int bufferSizeInBytes, Context context) throws IllegalArgumentException {
        super(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        mBufferSizeInBytes = bufferSizeInBytes;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void release() {
        super.release();
    }

    @Override
    public void startRecording() throws IllegalStateException {
        super.startRecording();
        is_open = true;
    }

    @Override
    public void stop() throws IllegalStateException {
        super.stop();
        if(mAudioManager.isBluetoothScoOn()){
            Log.d("MicroPhoneLine","stopBluetoothSco");
            mAudioManager.setBluetoothScoOn(false);
            mAudioManager.stopBluetoothSco();
        }
        is_open = false;
    }

    public boolean isOpen() {
        return is_open;
    }

    public int getBufferSize() {
        return mBufferSizeInBytes;
    }


}

