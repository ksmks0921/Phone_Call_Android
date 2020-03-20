package com.ajiew.phonecallapp.phonecallui;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.ajiew.phonecallapp.ActivityStack;
import com.ajiew.phonecallapp.R;

import java.util.Timer;
import java.util.TimerTask;

import static com.ajiew.phonecallapp.listenphonecall.CallListenerService.formatPhoneNumber;


/**
 * Provides the interface to make and receive calls, only supports Android M (6.0, API 23) and above systems
 *
 * @author Crystal
 */
@RequiresApi(api = Build.VERSION_CODES.M)
public class PhoneCallActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvCallNumberLabel;
    private TextView tvCallNumber;
    private TextView tvPickUp;
    private TextView tvCallingTime;
    private TextView tvHangUp;

    private PhoneCallManager phoneCallManager;
    private PhoneCallService.CallType callType;
    private String phoneNumber;

    private Timer onGoingCallTimer;
    private int callingTime;

    public static void actionStart(Context context, String phoneNumber,
                                   PhoneCallService.CallType callType) {
        Intent intent = new Intent(context, PhoneCallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, callType);
        intent.putExtra(Intent.EXTRA_PHONE_NUMBER, phoneNumber);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_call);

        ActivityStack.getInstance().addActivity(this);

        initData();

        initView();
    }

    private void initData() {
        phoneCallManager = new PhoneCallManager(this);
        onGoingCallTimer = new Timer();
        if (getIntent() != null) {
            phoneNumber = getIntent().getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            callType = (PhoneCallService.CallType) getIntent().getSerializableExtra(Intent.EXTRA_MIME_TYPES);
        }
    }

    private void initView() {
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION //hide navigationBar
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        tvCallNumberLabel = findViewById(R.id.tv_call_number_label);
        tvCallNumber = findViewById(R.id.tv_call_number);
        tvPickUp = findViewById(R.id.tv_phone_pick_up);
        tvCallingTime = findViewById(R.id.tv_phone_calling_time);
        tvHangUp = findViewById(R.id.tv_phone_hang_up);

        tvCallNumber.setText(formatPhoneNumber(phoneNumber));
        tvPickUp.setOnClickListener(this);
        tvHangUp.setOnClickListener(this);

        // Incoming call
        if (callType == PhoneCallService.CallType.CALL_IN) {
            tvCallNumberLabel.setText("Caller number");
            tvPickUp.setVisibility(View.VISIBLE);
        }
        // 打出的电话
        else if (callType == PhoneCallService.CallType.CALL_OUT) {
            tvCallNumberLabel.setText("Call number");
            tvPickUp.setVisibility(View.GONE);
            phoneCallManager.openSpeaker();
        }

        // turn on and unlock screen
        wakeUpAndUnlock();
    }

    public void wakeUpAndUnlock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if (pm == null) {
            return;
        }

        boolean screenOn = pm.isScreenOn();
        if (!screenOn) {
            PowerManager.WakeLock wl = pm.newWakeLock(
                    PowerManager.ACQUIRE_CAUSES_WAKEUP, getPackageName() + "TAG");
            wl.acquire(10000);
            wl.release();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (keyguardManager == null) {
                return;
            }
            keyguardManager.requestDismissKeyguard(this, null);
        } else {
            this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_phone_pick_up) {
            phoneCallManager.answer();
            tvPickUp.setVisibility(View.GONE);
            tvCallingTime.setVisibility(View.VISIBLE);
            onGoingCallTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            callingTime++;
                            tvCallingTime.setText("calling：" + getCallingTime());
                        }
                    });
                }
            }, 0, 1000);
        } else if (v.getId() == R.id.tv_phone_hang_up) {
            phoneCallManager.disconnect();
            stopTimer();
        }
    }

    private String getCallingTime() {
        int minute = callingTime / 60;
        int second = callingTime % 60;
        return (minute < 10 ? "0" + minute : minute) +
                ":" +
                (second < 10 ? "0" + second : second);
    }

    private void stopTimer() {
        if (onGoingCallTimer != null) {
            onGoingCallTimer.cancel();
        }

        callingTime = 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        phoneCallManager.destroy();
    }
}
