package com.example.helloworld;

import android.app.Application;
import android.content.pm.PackageManager;
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
    public static final boolean LOCATION_ENABLED = false;
    public static final boolean ANALYTICS_ENABLED = false;
    public static final boolean CLOUD_PAGES_ENABLED = false;

    public static String VERSION_NAME;
    public static int VERSION_CODE;

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

            // Register to recieve push notifications.
            ETPush.readyAimFire(
                    this,
                    getString(R.string.et_app_id),      // TODO Replace with Your App Center Application ID
                    getString(R.string.access_token),   // TODO Replace with Your App Center Access Token
                    getString(R.string.gcm_sender_id),  // TODO Replace with Your GCM Sender ID
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

        }

    }

}
