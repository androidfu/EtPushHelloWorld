package com.example.helloworld;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;

import com.exacttarget.etpushsdk.ETException;
import com.exacttarget.etpushsdk.ETPush;
import com.exacttarget.etpushsdk.ETPushConfig;
import com.exacttarget.etpushsdk.data.Attribute;
import com.exacttarget.etpushsdk.event.RegistrationEvent;
import com.exacttarget.etpushsdk.util.EventBus;

/**
 * Created by bmote on 12/11/14.
 */
public class HelloWorldApplication extends Application {

    public static final String TAG = "HelloWorldApplication";

    public static final boolean ANALYTICS_ENABLED = true;
    public static final boolean CLOUD_PAGES_ENABLED = true;
    public static final boolean WAMA_ENABLED = true;
    public static final long MIDDLE_TIER_PROPAGATION_MIN_DELAY = DateUtils.MINUTE_IN_MILLIS * 5; // 5 min.
    public static final String EXTRAS_REGISTRATION_EVENT = "event";
    public static final String HELLO_WORLD_PREFERENCES = "hello_world_preferences";
    public static final String KEY_PREFS_ALARM_TIME = "mt_alarm_time";
    public static final String INTENT_ACTION_STRING = "mt_propagation_alarm";

    public static String VERSION_NAME;
    public static int VERSION_CODE;
    private static long okToCheckMiddleTier;
    private SharedPreferences.Editor preferencesEditor;

    @Override
    public void onCreate() {
        super.onCreate();

        VERSION_NAME = getAppVersionName();
        VERSION_CODE = getAppVersionCode();

        SharedPreferences sharedPreferences = getSharedPreferences(HELLO_WORLD_PREFERENCES, MODE_PRIVATE);
        preferencesEditor = sharedPreferences.edit();

        /*
            A good practice is to register your application to listen for events posted to a private
            communication bus by the SDK.
         */
        EventBus.getInstance().register(this);

        try {

            // Register to receive push notifications.
            ETPush.readyAimFire(new ETPushConfig.Builder(this)
                            .setEtAppId(getString(R.string.et_app_id))
                            .setAccessToken(getString(R.string.access_token))
                            .setGcmSenderId(getString(R.string.gcm_sender_id))
                            .setLogLevel(BuildConfig.DEBUG ? Log.VERBOSE : Log.ERROR)
                            .setAnalyticsEnabled(ANALYTICS_ENABLED)
                            .setPiAnalyticsEnabled(WAMA_ENABLED)
                            .setCloudPagesEnabled(CLOUD_PAGES_ENABLED)
                            //.setLocationEnabled(true)
                            //.setProximityEnabled(true)
                            .build()
            );
        } catch (ETException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Return the application version name as recorded in the app's build.gradle file.  If this is
     * a debug release then append a "d" to denote such.
     *
     * @return a String representing the application version name
     */
    private String getAppVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Return the application version code as recorded in the app's build.gradle file.
     *
     * @return an int representing the application version code
     */
    private int getAppVersionCode() {
        try {
            return this.getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * EventBus callback listening for a RegistrationEvent.
     *
     * @param event the type of event we're listening for.
     */
    public void onEvent(final RegistrationEvent event) {

        if (ETPush.getLogLevel() <= Log.DEBUG) {
            Log.d(TAG, "Marketing Cloud update occurred.");
            Log.d(TAG, "Device ID:" + event.getDeviceId());
            Log.d(TAG, "Device Token:" + event.getSystemToken());
            Log.d(TAG, "Subscriber key:" + event.getSubscriberKey());
            for (Object attribute : event.getAttributes()) {
                Log.d(TAG, "Attribute " + ((Attribute) attribute).getKey() + ": [" + ((Attribute) attribute).getValue() + "]");
            }
            Log.d(TAG, "Tags: " + event.getTags());
            Log.d(TAG, "Language: " + event.getLocale());
            Log.d(TAG, String.format("Last sent: %1$d", System.currentTimeMillis()));
        }

        /**
         * BEGIN Developer Helper Notification
         *
         * Notify me when my changes have been propagated by the Middle Tier to the Marketing
         * Cloud.  This should never be required in a production application.
         */
        if (!BuildConfig.DEBUG) {
            return;
        }

        /*
            The middle tier has a 15 min. delay in data propagation.  Make sure we're waiting an
            appropriate amount of time before having our tests run.
        */
        long proposedCheckTime = System.currentTimeMillis() + MIDDLE_TIER_PROPAGATION_MIN_DELAY;
        /*
            Because we have async tasks, never set an alarm for the middle tier that would be
            earlier than any previous alarm.

            This could be expanded to handle multiple alarms but that is overkill at the moment.
         */
        if (proposedCheckTime < okToCheckMiddleTier) {
            return;
        }
        // getLastSent() is returning 0, but I need to discuss with team.
        event.setLastSent(System.currentTimeMillis());

        okToCheckMiddleTier = proposedCheckTime;
        Log.v(TAG, String.format("Setting an alarm for %3$dms from %1$d (alarm time: %2$d)", System.currentTimeMillis(), okToCheckMiddleTier, okToCheckMiddleTier - System.currentTimeMillis()));
        Intent intent = new Intent(INTENT_ACTION_STRING);
        Bundle bundle = new Bundle();
        bundle.putSerializable(EXTRAS_REGISTRATION_EVENT, event);
        intent.putExtras(bundle);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, R.id.mt_alarm, intent, 0);
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Service.ALARM_SERVICE);
        /*
            Cancel any existing alarms as we're about to set one that will account for the latest
            change.
         */
        alarmManager.cancel(pendingIntent);
        alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                okToCheckMiddleTier,
                pendingIntent
        );

        preferencesEditor.putLong(KEY_PREFS_ALARM_TIME, okToCheckMiddleTier).apply();
    }
}
