package com.ajiew.phonecallapp.phonecallui;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.telecom.Call;
import android.telecom.VideoProfile;


@RequiresApi(api = Build.VERSION_CODES.M)
public class PhoneCallManager {

    public static Call call;

    private Context context;
    private AudioManager audioManager;

    public PhoneCallManager(Context context) {
        this.context = context;

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    /**
     * answer the phone
     */
    public void answer() {
        if (call != null) {
            call.answer(VideoProfile.STATE_AUDIO_ONLY);
            openSpeaker();
        }
    }
    /**
     * Disconnect calls, including rejection of incoming calls and hang up after answering
     */
    public void disconnect() {
        if (call != null) {
            call.disconnect();
        }
    }
    /**
     * Open handsfree
     */
    public void openSpeaker() {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(true);
        }
    }
    /**
     * Destruction of resources
     * */
    public void destroy() {
        call = null;
        context = null;
        audioManager = null;
    }
}
