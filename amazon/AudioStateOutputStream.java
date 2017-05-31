package com.goertek.smartear.amazon;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by almo.liu on 2016/11/17.
 */

public class AudioStateOutputStream extends PipedOutputStream {

    private RecordingStateListener stateListener;
    private RecordingRMSListener rmsListener;

    protected AudioStateOutputStream(PipedInputStream inputStream,
                                     RecordingStateListener stateListener, final RecordingRMSListener rmsListener)
            throws IOException {
        super(inputStream);
        this.stateListener = stateListener;
        this.rmsListener = rmsListener;
        notifyRecordingStarted();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        calculateDB(b, len);
    }

    @Override
    public void close() throws IOException {
        super.close();
        notifyRecordingCompleted();
        clearRMS();
    }

    private void notifyRecordingStarted() {
        if (stateListener != null) {
            stateListener.recordingStarted();
        }
    }

    private void notifyRecordingCompleted() {
        if (stateListener != null) {
            stateListener.recordingCompleted();
        }
    }

    private void clearRMS() {
        if (rmsListener != null) {
            rmsListener.rmsChanged(0);
        }
    }

    // rmsListener is the AudioRMSListener callback for audio visualizer(optional - can be null)
    // assuming 16bit samples, 1 channel, little endian
    private void calculateDB(byte[] data, int cnt) {
        if ((rmsListener == null) || (cnt < 2)) {
            return;
        }

        final int bytesPerSample = 2;
        int len = cnt / bytesPerSample;
        double avg = 0;

        for (int i = 0; i < cnt; i += bytesPerSample) {
            ByteBuffer bb = ByteBuffer.allocate(bytesPerSample);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.put(data[i]);
            bb.put(data[i + 1]);
            // generate the signed 16 bit number from the 2 bytes
            double dVal = java.lang.Math.abs(bb.getShort(0));
            // scale it from 1 to 100. Use max/2 as values tend to be low
            dVal = ((100 * dVal) / (Short.MAX_VALUE / 2.0)) + 1;
            avg += dVal * dVal; // add the square to the running average
        }
        avg /= len;
        avg = java.lang.Math.sqrt(avg);
        // update the AudioRMSListener callback with the scaled root-mean-squared power value
        rmsListener.rmsChanged((int) avg);
    }
}

