package com.example.helloworld;

import android.app.Application;
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

    // Enabling location here also triggers work in our Activity that must be done.
    public static final boolean LOCATION_ENABLED = false;
    public static final boolean ANALYTICS_ENABLED = false;
    public static final boolean CLOUD_PAGES_ENABLED = false;

    private static final String TAG = HelloWorldApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        // Register this Application to process events from the SDK.
        EventBus.getDefault().register(this);

        try {
            ETPush.setLogLevel(BuildConfig.DEBUG ? Log.VERBOSE : Log.ERROR);
            // TODO Replace the values in readyAimFire() here or create your own Strings Resource File
            ETPush.readyAimFire(
                    this, // Application Context
                    getString(R.string.et_app_id), // I store the IDs in res/values/secrets.xml which is excluded from
                    getString(R.string.access_token), // source control as a security precaution.  Recreate a Strings
                    getString(R.string.gcm_sender_id), // resource file with your own IDs or replace these values.
                    ANALYTICS_ENABLED,
                    LOCATION_ENABLED,
                    CLOUD_PAGES_ENABLED
            );
        } catch (ETException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * EventBus callback
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
