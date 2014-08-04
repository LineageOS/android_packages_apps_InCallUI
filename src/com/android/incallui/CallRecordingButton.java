package com.android.incallui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import com.android.callrecorder.CallRecorder;
import com.android.services.telephony.common.Call;

import java.util.Date;

public class CallRecordingButton extends ImageButton
        implements CallList.Listener, View.OnClickListener {

    public CallRecordingButton(Context context) {
        super(context);
    }

    public CallRecordingButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CallRecordingButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onClick(View v) {
        // toggle recording depending on button state
        final CallRecorder recorder = CallRecorder.getInstance();
        if (recorder.isRecording()) {
            recorder.finishRecording();
            setImageResource(R.drawable.ic_record_holo_dark);
        }
        else {
            Call call = CallList.getInstance().getActiveCall();
            // can't start recording with no active call
            if (call != null) {
                long createTime = call.getCreateTime();
                boolean success = recorder.startRecording(call.getNumber(), new Date(createTime));
                if (success) {
                    setImageResource(R.drawable.ic_record_stop_holo_dark);
                }
                else {
                    setImageResource(R.drawable.ic_record_holo_dark);
                }
            }
        }
    }

    @Override
    public void onIncomingCall(Call call) {
        // nothing to do
    }

    @Override
    public void onCallListChange(CallList callList) {
        Call activeCall = callList.getActiveCall();
        if (activeCall != null) {
            // update button state based on recording status
            setEnabled(true);
            if (CallRecorder.getInstance().isRecording()) {
                setImageResource(R.drawable.ic_record_stop_holo_dark);
            }
            else {
                setImageResource(R.drawable.ic_record_holo_dark);
            }
        }
    }

    @Override
    public void onDisconnect(Call call) {
        // nothing to do
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        CallList.getInstance().addListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        CallList.getInstance().removeListener(this);
    }
}
