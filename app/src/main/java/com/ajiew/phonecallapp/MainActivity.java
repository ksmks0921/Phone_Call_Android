package com.ajiew.phonecallapp;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telecom.TelecomManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.ajiew.phonecallapp.listenphonecall.CallListenerService;

import java.lang.reflect.Field;

import ezy.assist.compat.SettingsCompat;


public class MainActivity extends AppCompatActivity {

    private Switch switchPhoneCall;

    private Switch switchListenCall;

    private CompoundButton.OnCheckedChangeListener switchCallCheckChangeListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        switchPhoneCall = findViewById(R.id.switch_default_phone_call);
        switchListenCall = findViewById(R.id.switch_call_listenr);

        switchPhoneCall.setOnClickListener(v -> {
            // Request to make this app the default phone app, only Android M and above are supported
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (switchPhoneCall.isChecked()) {
                    Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
                    intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                            getPackageName());
                    startActivity(intent);
                } else {
                    // Jump to the default settings page when canceling
                    startActivity(new Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"));
                }
            } else {
                Toast.makeText(MainActivity.this, "Android 6.0 Only the above supports modifying the default phone application！", Toast.LENGTH_LONG).show();
                switchPhoneCall.setChecked(false);
            }

        });

        // Check if permissions are enabled using SettingsCompat
        switchCallCheckChangeListener = (buttonView, isChecked) -> {
            if (isChecked && !SettingsCompat.canDrawOverlays(MainActivity.this)) {

                // Request hover permissions
                askForDrawOverlay();

                // Clear the selected state when not open, while avoiding callbacks
                switchListenCall.setOnCheckedChangeListener(null);
                switchListenCall.setChecked(false);
                switchListenCall.setOnCheckedChangeListener(switchCallCheckChangeListener);
                return;
            }

            Intent callListener = new Intent(MainActivity.this, CallListenerService.class);
            if (isChecked) {
                startService(callListener);
                Toast.makeText(this, "Call monitoring service is turned on", Toast.LENGTH_SHORT).show();
            } else {
                stopService(callListener);
                Toast.makeText(this, "Call monitoring service is off", Toast.LENGTH_SHORT).show();
            }
        };
        switchListenCall.setOnCheckedChangeListener(switchCallCheckChangeListener);
    }

    private void askForDrawOverlay() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                .setTitle("Allow floating boxes to be displayed")
                .setMessage("For phone monitoring service to work properly, allow this permission")
                .setPositiveButton("Go to settings", (dialog, which) -> {
                    openDrawOverlaySettings();
                    dialog.dismiss();
                })
                .setNegativeButton("Talk later", (dialog, which) -> dialog.dismiss());

        alertDialog.show();
    }

    /**
     * Jump to floating window management settings interface
     */
    private void openDrawOverlaySettings() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M and above guide the user to open the allow floating window in the system settings
            // Use reflection to guarantee usability on most models with as little code as possible
            try {
                Context context = this;
                Class clazz = Settings.class;
                Field field = clazz.getDeclaredField("ACTION_MANAGE_OVERLAY_PERMISSION");
                Intent intent = new Intent(field.get(null).toString());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Please open permissions in floating window management", Toast.LENGTH_LONG).show();
            }
        } else {
            // Below 6.0, use the interface provided in SettingsCompat directly, only available on domestic mobile phones
            SettingsCompat.manageDrawOverlays(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        switchPhoneCall.setChecked(isDefaultPhoneCallApp());
        switchListenCall.setChecked(isServiceRunning(CallListenerService.class));
    }

    /**
     * Android M and above check if it is the system default phone app
     */
    public boolean isDefaultPhoneCallApp() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            TelecomManager manger = (TelecomManager) getSystemService(TELECOM_SERVICE);
            if (manger != null && manger.getDefaultDialerPackage() != null) {
                return manger.getDefaultDialerPackage().equals(getPackageName());
            }
        }
        return false;
    }


    public boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return false;

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }
}
