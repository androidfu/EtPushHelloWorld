package com.example.helloworld.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.exacttarget.etpushsdk.ETException;
import com.exacttarget.etpushsdk.ETLocationManager;
import com.exacttarget.etpushsdk.ETPush;
import com.example.helloworld.HelloWorldApplication;
import com.example.helloworld.R;
import com.radiusnetworks.ibeacon.BleNotAvailableException;


public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_PREFS_PUSH_ENABLED = "push_enabled";
    private final Runnable displayTimeRemainingRunnable = new Runnable() {
        @Override
        public void run() {
            displayTimeRemaining();
        }
    };
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor preferencesEditor;

    private ToggleButton toggleButtonEnablePush;
    private TextView countDownTimer;

    private boolean isPushEnabled;
    private long alarmTime;

     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(HelloWorldApplication.HELLO_WORLD_PREFERENCES, MODE_PRIVATE);
        preferencesEditor = sharedPreferences.edit();

        /*
            How long until any changes here are reflected in the Marketing Cloud?
         */
        alarmTime = sharedPreferences.getLong(HelloWorldApplication.KEY_PREFS_ALARM_TIME, 0);

        /*
            Get the last saved user state.
         */
        isPushEnabled = sharedPreferences.getBoolean(KEY_PREFS_PUSH_ENABLED, true);

        /*
            Our countdown timer view.  Shows the seconds until the middle tier updates are
            propagated to the Marketing Cloud servers.
         */
        countDownTimer = (TextView) findViewById(R.id.tv_countdown_timer);

        /*
            Our toggle buttons.  Set their state based off the preferences and create
            a click listener.  Be sure to update the user's selected state and store it when it
            changes.
         */
        toggleButtonEnablePush = (ToggleButton) findViewById(R.id.toggle_enablePush);
        toggleButtonEnablePush.setChecked(isPushEnabled);
        toggleButtonEnablePush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleButtonEnablePush.toggle();
                isPushEnabled = !isPushEnabled;
                try {
                    if (isPushEnabled) {
                        Log.i(TAG, "Enabling push.");
                        ETPush.pushManager().enablePush();
                    } else {
                        Log.i(TAG, "Disabling push.");
                        ETPush.pushManager().disablePush();
                    }
                    ((ToggleButton) v).setChecked(isPushEnabled);
                    preferencesEditor.putBoolean(KEY_PREFS_PUSH_ENABLED, isPushEnabled).apply();
                } catch (ETException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        });

        try {
            /*
                Add First & Last Name Attributes & a Subscriber Key
             */
            Log.i(TAG, "Adding attributes.");
            ETPush.pushManager().addAttribute("FirstName", "EtPushHelloWorld");
            ETPush.pushManager().addAttribute("LastName", getString(R.string.gcm_sender_id));
            Log.i(TAG, "Adding subscriber key.");
            ETPush.pushManager().setSubscriberKey("bmote@exacttarget.com");

            if (isPushEnabled) {
                Log.i(TAG, "Push is enabled.");
                ETPush.pushManager().enablePush();
            }

        } catch (ETException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        TextView sdkInformation = (TextView) findViewById(R.id.tv_sdkInfo);
        sdkInformation.setText(String.format("JB4A SDK v%1$s", ETPush.getSdkVersionName() /* ETPush.ETPushSDKVersionString in version 2014-08 */));

        TextView apiInformation = (TextView) findViewById(R.id.tv_apiInfo);
        apiInformation.setText(String.format("Android API %1$s (v%2$s)\n%3$s", Build.VERSION.SDK_INT, Build.VERSION.RELEASE, Build.PRODUCT));

        TextView psInformation = (TextView) findViewById(R.id.tv_psInfo);
        psInformation.setText(String.format("Google Play Services v%1$s", getResources().getInteger(R.integer.google_play_services_version)));
    }

    /**
     * Calculate the number of seconds remaining and update our view.  Also, call our runnable
     * 1 sec. from now which will return us here ;)
     */
    private void displayTimeRemaining() {
        long millisecondsRemaining = alarmTime - System.currentTimeMillis();
        int secondsRemaining = (int) (millisecondsRemaining / DateUtils.SECOND_IN_MILLIS);
        countDownTimer.removeCallbacks(displayTimeRemainingRunnable);
        if (secondsRemaining > 0) {
            countDownTimer.setText(String.format(getString(R.string.countdown_timer_text), secondsRemaining, getResources().getQuantityString(R.plurals.seconds, secondsRemaining)));
            countDownTimer.postDelayed(displayTimeRemainingRunnable, DateUtils.SECOND_IN_MILLIS);
            toggleScreenWake(true);
        } else {
            countDownTimer.setText(getString(R.string.no_pending_alarms));
            toggleScreenWake(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
            This is only for the displaying of our countdown timer and is not required by the
            JB4A SDK.
         */
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        displayTimeRemaining();
    }

    @Override
    protected void onPause() {
        /*
            Tidy up.
         */
        countDownTimer.removeCallbacks(displayTimeRemainingRunnable);
        /*
            This is only for the displaying of our countdown timer and is not required by the
            JB4A SDK.
         */
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        toggleScreenWake(false);
        super.onPause();
    }

    /*
        Our onEvent() EventBus callback is in our Application Class.  It will update our alarm time
        via SharedPreferences so implement the onSharedPreferenceChanged() interface to reflect the
        changes in our view.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(HelloWorldApplication.KEY_PREFS_ALARM_TIME)) {
            alarmTime = sharedPreferences.getLong(HelloWorldApplication.KEY_PREFS_ALARM_TIME, 0);
            displayTimeRemaining();
        }
    }

    /**
     * Toggles the screen wake.  Currently being used to keep the screen on if a timer is running.
     */
    public void toggleScreenWake(boolean keepAwake) {
        if (keepAwake) {
            this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

    }
}
