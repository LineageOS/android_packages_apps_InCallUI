package com.android.callrecorder;

import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;
import com.android.dialer.calllog.CallDetailHistoryAdapter;
import com.android.services.callrecorder.CallRecorderService;
import com.android.services.callrecorder.common.CallRecording;

import java.io.File;
import java.io.IOException;

/**
 * Simple playback for call recordings
 */
public class CallRecordingPlayer implements MediaPlayer.OnCompletionListener {

    private MediaPlayer mPlayer = null;
    private boolean mPlaying = false;
    private CallDetailHistoryAdapter.PlayButton mButton;

    public void play(String fileName, CallDetailHistoryAdapter.PlayButton button) {
        if (mPlayer != null) {
            // stop and cleanup current session first
            stop();
        }

        mButton = button;

        File file =
                new File(Environment.getExternalStoragePublicDirectory(CallRecording.PUBLIC_DIRECTORY_NAME), fileName);
        String filePath = file.getAbsolutePath();

        mPlayer = new MediaPlayer();
        mPlayer.setOnCompletionListener(this);
        try {
            mPlayer.setDataSource(filePath);
            mPlayer.prepare();
        }
        catch (IOException e) {
            Log.w(CallRecorder.TAG, "Error opening " + filePath, e);
            return;
        }

        try {
            mPlayer.start();
            mPlaying = true;
        }
        catch (IllegalStateException e) {
            Log.w(CallRecorder.TAG, "Could not start player", e);
        }
    }

    public void stop() {
        if (mPlayer != null) {
            try {
                mPlayer.stop();
            }
            catch (IllegalStateException e) {
                Log.w(CallRecorder.TAG, "Exception stopping player", e);
            }
            mPlayer.release();
            mPlayer = null;
            resetButton();
        }
        mPlaying = false;
    }

    public boolean isPlaying() {
        return mPlaying;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        resetButton();

        mPlayer.release();
        mPlayer = null;
        mPlaying = false;
    }

    private void resetButton() {
        if (mButton != null) {
            mButton.reset();
            mButton = null;
        }
    }
}
