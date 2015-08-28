package com.example.helloworld.ui.activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.exacttarget.etpushsdk.ETException;
import com.exacttarget.etpushsdk.ETPush;
import com.example.helloworld.HelloWorldApplication;
import com.example.helloworld.R;


public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_PREFS_PUSH_ENABLED = "push_enabled";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor preferencesEditor;
    private ToggleButton toggleButtonEnablePush;
    private TextView countDownTimer;
    private boolean isPushEnabled;
    private long alarmTime;
    private final Runnable displayTimeRemainingRunnable = new Runnable() {
        @Override
        public void run() {
            displayTimeRemaining();
        }
    };
    private ETPush etPush;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(HelloWorldApplication.HELLO_WORLD_PREFERENCES, MODE_PRIVATE);
        preferencesEditor = sharedPreferences.edit();

        try {
            etPush = ETPush.getInstance();
        } catch (ETException e) {
            e.printStackTrace();
        }


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
                        //etPush.enablePush();
                        etPush.addAttribute("FirstName", "EtPushHelloWorld");
                        etPush.addAttribute("LastName", String.valueOf(System.currentTimeMillis()));
                        etPush.addAttribute("Locale", "en-US");
                        etPush.addAttribute("POSListId", "HCOM_USen-US");
                        etPush.addAttribute("AppVersion", "13.0");
                        etPush.addAttribute("POSID", "HCOM_US");
                        etPush.addAttribute("PushOptIn", "1");
                        etPush.addAttribute("hcom_device_id", "c27dd654-6526-45a5-a968-094cc3292cf1");
                        etPush.addAttribute("signInTimeStamp", "08/25/2015");
                        etPush.addAttribute("SignOutFlag", "0");
                        etPush.addAttribute("DossierId", "12345678");
                        etPush.addAttribute("SubscriberKeyAttrib", "email@domain.comHCOM_USen_US");
                        etPush.addAttribute("EmailAddress", "email@domain.com");
//                        for (int i = 1; i <= 100; i++) {
//                            etPush.addAttribute(String.format("test_attribute_%d", i), String.valueOf(i));
//                        }
                    } else {
                        Log.i(TAG, "Disabling push.");
                        //etPush.disablePush();
                        etPush.removeAttribute("FirstName");
                        etPush.removeAttribute("LastName");
                        etPush.removeAttribute("Locale");
                        etPush.removeAttribute("POSListId");
                        etPush.removeAttribute("AppVersion");
                        etPush.removeAttribute("POSID");
                        etPush.removeAttribute("PushOptIn");
                        etPush.removeAttribute("hcom_device_id");
                        etPush.removeAttribute("signInTimeStamp");
                        etPush.removeAttribute("SignOutFlag");
                        etPush.removeAttribute("DossierId");
                        etPush.removeAttribute("SubscriberKeyAttrib");
                        etPush.removeAttribute("EmailAddress");
//                        for (int i = 1; i <= 100; i++) {
//                            etPush.removeAttribute(String.format("test_attribute_%d", i));
//                        }
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
//            ETPush.pushManager().addAttribute("FirstName", "EtPushHelloWorld");
//            ETPush.pushManager().addAttribute("LastName", String.valueOf(System.currentTimeMillis()));
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
        //ETAnalytics.trackPageView(MainActivity.class.getCanonicalName());
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
