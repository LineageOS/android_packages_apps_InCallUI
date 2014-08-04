package com.android.callrecorder;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.incallui.CallList;
import com.android.services.callrecorder.CallRecorderService;
import com.android.services.callrecorder.common.CallRecording;
import com.android.services.callrecorder.common.ICallRecorderService;
import com.android.services.telephony.common.Call;

import com.android.dialer.R;

import java.util.Date;

/**
 * InCall UI's interface to the call recorder
 *
 * Manages the call recorder service lifecycle.  We bind to the service whenever an active call is
 * established, and unbind when all calls have been disconnected.
 */
public class CallRecorder  implements CallList.Listener {

    public static final String TAG = "CallRecorder";

    private static final String ENABLE_PROPERTY = "persist.call_recording.enabled";

    private Context mContext;
    private static CallRecorder sInstance = null;
    private static boolean sEnabled;

    private boolean mInitialized = false;
    private ICallRecorderService mService = null;

    private RecordingProgressListener mProgressListener = null;
    private Handler mHandler;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ICallRecorderService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    public static CallRecorder getInstance() {
        if (sInstance == null) {
            sInstance = new CallRecorder();
            sEnabled = isEnabled();
        }
        return sInstance;
    }

    public static boolean isEnabled() {
        return SystemProperties.getBoolean(ENABLE_PROPERTY, false);
    }

    private void initialize() {
        if (isEnabled() && !mInitialized) {
            Intent serviceIntent = getServiceIntent();
            mContext.bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
            mInitialized = true;
        }
    }

    private void deinitialize() {
        if (mInitialized) {
            mContext.unbindService(mConnection);
            mInitialized = false;
        }
    }

    public boolean startRecording(final String phoneNumber, final Date creationTime) {
        if (mService == null)
            return false;

        boolean success = false;
        try {
            success = mService.startRecording(phoneNumber, creationTime.getTime());
            if (success) {
                if (mProgressListener != null) {
                    mProgressListener.onStartRecording();
                    mUpdateRecordingProgressTask.run();
                }
            }
            else {
                Toast toast = Toast.makeText(mContext, R.string.call_recording_failed_message, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        catch (Exception e) {
            Log.w(TAG, "Failed to start recording " + phoneNumber + ", " + creationTime, e);
        }

        return success;
    }

    public boolean isRecording() {
        if (mService == null)
            return false;

        try {
            return mService.isRecording();
        }
        catch (RemoteException e) {
            Log.w(TAG, "Exception checking recording status", e);
        }
        return false;
    }

    public CallRecording getActiveRecording() {
        if (mService == null)
            return null;

        try {
            return mService.getActiveRecording();
        }
        catch (RemoteException e) {
            Log.w("Exception getting active recording", e);
        }
        return null;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    private CallRecorder() {
        mHandler = new Handler();
    }

    private Intent getServiceIntent() {
        return new Intent(mContext, CallRecorderService.class);
    }

    public void finishRecording() {
        try {
            final CallRecording recording = mService.stopRecording();
            if (recording != null) {
                new Thread(new Runnable() {
                    public void run() {
                        CallRecordingDataStore dataStore = new CallRecordingDataStore();
                        dataStore.open(mContext);
                        dataStore.putRecording(recording);
                        dataStore.close();
                    }
                }).start();
            }
        }
        catch (Exception e) {
            Log.w(TAG, "Failed to stop recording", e);
        }

        if (mProgressListener != null) {
            mProgressListener.onStopRecording();
        }
        mHandler.removeCallbacks(mUpdateRecordingProgressTask);
    }

    //
    // Call list listener methods.
    //
    @Override
    public void onIncomingCall(Call call) {} // do nothing

    @Override
    public void onCallListChange(final CallList callList) {

        if (!mInitialized && callList.getActiveCall() != null) {
            // we'll come here if this is the first active call
            initialize();
        }
        else {
            // we can come down this branch to resume a call that was on hold
            CallRecording active = getActiveRecording();
            if (active != null) {
                for (Call call : callList.getCalls()) {
                    if (TextUtils.equals(call.getNumber(), active.phoneNumber) && call.getState() == Call.State.ONHOLD) {
                        // the call associated with the active recording has been placed on hold.
                        // stop the recording
                        finishRecording();
                    }
                }
            }
        }
    }

    @Override
    public void onDisconnect(final Call call) {
        CallRecording active = getActiveRecording();
        if (active != null) {
            if (TextUtils.equals(call.getNumber(), active.phoneNumber)) {
                // finish the current recording if the call gets disconnected
                finishRecording();
            }
        }

        // tear down the service if there are no more active calls
        if (CallList.getInstance().getActiveCall() == null) {
            deinitialize();
        }
    }

    // allow clients to listen for recording progress updates
    public interface RecordingProgressListener {
        public void onStartRecording();
        public void onStopRecording();
        public void onRecordingTimeProgress(long elapsedTimeMs);
    }

    public void setRecordingProgressListener(RecordingProgressListener listener) {
        mProgressListener = listener;
    }

    private static final int UPDATE_INTERVAL = 500;

    private Runnable mUpdateRecordingProgressTask = new Runnable() {
        @Override
        public void run() {
            CallRecording active = getActiveRecording();
            if (mProgressListener != null && active != null) {
                long elapsed = System.currentTimeMillis() - active.startRecordingTime;
                mProgressListener.onRecordingTimeProgress(elapsed);
            }
            mHandler.postDelayed(mUpdateRecordingProgressTask, UPDATE_INTERVAL);
        }
    };
}
