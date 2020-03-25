package com.ajiew.phonecallapp.phonecallui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.telecom.Call;
import android.telecom.InCallService;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.ajiew.phonecallapp.ActivityStack;
import com.ajiew.phonecallapp.MainActivity;
import com.ajiew.phonecallapp.R;
import com.ajiew.phonecallapp.listenphonecall.CallListenerService;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A service that monitors the communication status of the phone. To implement this class, you must provide a UI for phone management.
 *
 * @author crystal
 * @see PhoneCallActivity
 * @see android.telecom.InCallService
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class PhoneCallService extends InCallService  {

    private View phoneCallView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private PhoneCallManager phoneCallManager;
    private TextView tvPickUp;
    private TextView tv_call_number;
    private TextView tvHangUp;
    private Timer onGoingCallTimer;

    private Call.Callback callback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            super.onStateChanged(call, state);

            switch (state) {
                case Call.STATE_ACTIVE: {

                    break;
                }

                case Call.STATE_DISCONNECTED: {
                    ActivityStack.getInstance().finishActivity(PhoneCallActivity.class);
                    break;
                }

            }
        }
    };

    @Override
    public void onCallAdded(Call call) {


        super.onCallAdded(call);

        call.registerCallback(callback);
        PhoneCallManager.call = call;

        CallType callType = null;

        if (call.getState() == Call.STATE_RINGING) {
            callType = CallType.CALL_IN;
        } else if (call.getState() == Call.STATE_CONNECTING) {
            callType = CallType.CALL_OUT;
        }

        if (callType != null) {
            Call.Details details = call.getDetails();
            String phoneNumber = details.getHandle().getSchemeSpecificPart();




            FrameLayout interceptorLayout = new FrameLayout(this) {

                @Override
                public boolean dispatchKeyEvent(KeyEvent event) {

                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

                            return true;
                        }
                    }

                    return super.dispatchKeyEvent(event);
                }
            };
            windowManager = (WindowManager) getApplicationContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            int width = windowManager.getDefaultDisplay().getWidth();
            if (callType ==  CallType.CALL_IN) {
                phoneCallManager = new PhoneCallManager(this);

                onGoingCallTimer = new Timer();
                phoneCallView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.activity_phone_call_in, interceptorLayout);
                tvPickUp = phoneCallView.findViewById(R.id.tv_phone_pick_up);
                tv_call_number = phoneCallView.findViewById(R.id.tv_call_number);
                tvHangUp = phoneCallView.findViewById(R.id.tv_phone_hang_up);
                tv_call_number.setText(phoneNumber);
                tvPickUp.setOnClickListener(v -> {

                    phoneCallManager.answer();
                    tvPickUp.setVisibility(View.GONE);

                });
                tvHangUp.setOnClickListener(v -> {

                    phoneCallManager.disconnect();
//                    stopTimer();

                });
            }
            else if (callType == CallType.CALL_OUT) {

                phoneCallView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.activity_phone_call_out, interceptorLayout);
                tvPickUp = phoneCallView.findViewById(R.id.tv_phone_pick_up);
                tv_call_number.setText(phoneNumber);

                tvPickUp.setOnClickListener(v -> {
                    phoneCallManager.disconnect();
                });
            }


            params = new WindowManager.LayoutParams();
            params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
            params.width = width;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

            // Set the picture format, the effect is transparent background
            params.format = PixelFormat.TRANSLUCENT;
            // Set Window flag to system-level popup | Overlay
            params.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                    WindowManager.LayoutParams.TYPE_PHONE;

            // Non-aggregatable (no response to return key) | fullscreen
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            // API 19 and above can also open the transparent status bar and navigation bar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                params.flags = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            }


            windowManager.addView(phoneCallView, params);


        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        windowManager.removeView(phoneCallView);
        call.unregisterCallback(callback);
        PhoneCallManager.call = null;
    }




    public enum CallType {
        CALL_IN,
        CALL_OUT,
    }
}
