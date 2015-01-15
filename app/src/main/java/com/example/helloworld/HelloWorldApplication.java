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
import com.exacttarget.etpushsdk.data.Attribute;
import com.exacttarget.etpushsdk.event.RegistrationEvent;
import com.exacttarget.etpushsdk.util.EventBus;

/**
 * Created by bmote on 12/11/14.
 */
public class HelloWorldApplication extends Application {

    public static final String TAG = HelloWorldApplication.class.getSimpleName();

    // Enabling location here also triggers work in our Activity that must be done.
    public static final boolean LOCATION_ENABLED = true;
    public static final boolean ANALYTICS_ENABLED = true;
    public static final boolean CLOUD_PAGES_ENABLED = false;
    public static final long MIDDLE_TIER_PROPAGATION_MIN_DELAY = DateUtils.MINUTE_IN_MILLIS * 15; // 15 min.
    public static final String EXTRAS_REGISTRATION_EVENT = "event";
    public static final String HELLO_WORLD_PREFERENCES = "hello_world_preferences";
    public static final String KEY_PREFS_ALARM_TIME = "mt_alarm_time";
    public static final String INTENT_ACTION_STRING = "mt_propagation_alarm";

    public static String VERSION_NAME;
    public static int VERSION_CODE;

    private static long okToCheckMiddleTier;

    @Override
    public void onCreate() {
        super.onCreate();

        VERSION_NAME = getAppVersionName();
        VERSION_CODE = getAppVersionCode();

        /*
            A good practice is to register your application to listen for events posted to a private
            communication bus by the SDK.
         */
        EventBus.getDefault().register(this);

        try {
            // Set the log level based on the build type.
            ETPush.setLogLevel(BuildConfig.DEBUG ? Log.VERBOSE : Log.ERROR);

            // Register to receive push notifications.
            ETPush.readyAimFire(
                    this,
                    getString(R.string.et_app_id),   // TODO Replace with Your App Center Application ID
                    getString(R.string.access_token),               // TODO Replace with Your App Center Access Token
                    getString(R.string.gcm_sender_id),                           // TODO Replace with Your GCM Sender ID
                    ANALYTICS_ENABLED,
                    LOCATION_ENABLED,
                    CLOUD_PAGES_ENABLED
            );
            /*
                A good practice is to add the application's version name as a tag that can later
                be used to target push notifications to specific application versions.
             */
            ETPush pushManager = ETPush.pushManager();
            pushManager.addTag(VERSION_NAME);
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
        String developerBuild = BuildConfig.DEBUG ? "d" : "";
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName + developerBuild;
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
            Log.d(TAG, "Device Token:" + event.getDeviceToken());
            Log.d(TAG, "Subscriber key:" + event.getSubscriberKey());
            for (Attribute attribute : event.getAttributes()) {
                Log.d(TAG, "Attribute " + attribute.getKey() + ": [" + attribute.getValue() + "]");
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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

        SharedPreferences.Editor preferencesEditor = getSharedPreferences(HELLO_WORLD_PREFERENCES, MODE_PRIVATE).edit();
        preferencesEditor.putLong(KEY_PREFS_ALARM_TIME, okToCheckMiddleTier).apply();
    }
}
