package com.evidon.privacy.appnoticesdk;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.util.Log;

import com.evidon.privacy.appnoticesdk.callbacks.AppNotice_Callback;
import com.evidon.privacy.appnoticesdk.callbacks.JSONGetterCallback;
import com.evidon.privacy.appnoticesdk.model.AppNoticeData;
import com.evidon.privacy.appnoticesdk.utils.AppData;
import com.evidon.privacy.appnoticesdk.utils.Util;

import java.util.HashMap;

/**
 * Created by Steven.Overson on 2/4/2015.
 */
public class AppNotice {

    public final static String sdkVersionName = BuildConfig.VERSION_NAME;
    public final static int sdkVersionCode = BuildConfig.VERSION_CODE;
    private static final String TAG = "AppNotice";
    private AppNoticeData appNoticeData;
    private AppNotice_Callback appNotice_callback;
    private static Activity extActivity = null;
//    private static Context appContext;
    private static final HashMap<String, Object> sessionMap = new HashMap<String, Object>();
    public static boolean isImpliedMode = true;
    private static boolean usingToken = true;
    private static int implied30dayDisplayMax = 0;  // Default to mode-0. 0 displays on first start and every notice ID change. 1+ is the max number of times to display the consent screen on start up in a 30-day period.

    /**
     * AppNotice constructor -- implied mode
     * @param activity: Usually your start-up activity.
     * @param appNoticeToken: The notice token for the configuration created for this app.
     * @param appNotice_callback: An AppNotice_Callback object that handles the various callbacks from the SDK to the host app.
     */
    public AppNotice(Activity activity, String appNoticeToken, AppNotice_Callback appNotice_callback) {
        initAppNotice(activity, appNoticeToken, appNotice_callback, true);  // Init the SDK in implied mode
    }

    /**
     * AppNotice constructor
     * @param activity: Usually your start-up activity.
     * @param appNoticeToken: The notice token for the configuration created for this app.
     * @param appNotice_callback: An AppNotice_Callback object that handles the various callbacks from the SDK to the host app.
     * @param isImpliedMode: Initialize the SDK in either implied or explicit mode: true = implied; false = explicit.
     */
    public AppNotice(Activity activity, String appNoticeToken, AppNotice_Callback appNotice_callback, boolean isImpliedMode) {
        initAppNotice(activity, appNoticeToken, appNotice_callback, isImpliedMode);
    }

    /**
     * AppNotice initializer: Used for common functionality between the constructors
     * @param activity: Usually your start-up activity.
     * @param appNoticeToken: The notice token for the configuration created for this app.
     * @param appNotice_callback: An AppNotice_Callback object that handles the various callbacks from the SDK to the host app.
     * @param isImpliedMode: Initialize the SDK in either implied or explicit mode: true = implied; false = explicit.
     */
    private void initAppNotice(Activity activity, String appNoticeToken, AppNotice_Callback appNotice_callback, boolean isImpliedMode) {
        this.isImpliedMode = isImpliedMode;
        AppNoticeData.usingToken = true;
        extActivity = activity;
        if (appNoticeToken == null || appNoticeToken.isEmpty()) {
            throw(new IllegalArgumentException("Notice token must be a valid identifier."));
        }

        // Remember the provided callback
        this.appNotice_callback = appNotice_callback;
        AppNotice_Activity.appNotice_callback = appNotice_callback;

        // Get either a new or initialized tracker config object
        appNoticeData = AppNoticeData.getInstance(activity);

        // Keep track of the company ID and the notice ID
        appNoticeData.setCurrentAppNoticeToken(appNoticeToken);
        AppNoticeData.appContext = activity.getApplicationContext();
    }

// Deprecated CID/PID API. Remove in a future version.
//    /**
//     * AppNotice constructor -- implied mode
//     * @param activity: Usually your start-up activity.
//     * @param companyId: The company ID assigned to you for the App Notice SDK.
//     * @param noticeId: The notice ID of the configuration created for this app.
//     * @param appNotice_callback: An AppNotice_Callback object that handles the various callbacks from the SDK to the host app.
//     */
//    private AppNotice(Activity activity, int companyId, int noticeId, AppNotice_Callback appNotice_callback) {
//        initAppNotice(activity, companyId, noticeId, appNotice_callback, true);  // Init the SDK in implied mode
//    }
//
//    /**
//     * AppNotice constructor
//     * @param activity: Usually your start-up activity.
//     * @param companyId: The company ID assigned to you for the App Notice SDK.
//     * @param noticeId: The notice ID of the configuration created for this app.
//     * @param appNotice_callback: An AppNotice_Callback object that handles the various callbacks from the SDK to the host app.
//     * @param isImpliedMode: Initialize the SDK in either implied or explicit mode: true = implied; false = explicit.
//     */
//    private AppNotice(Activity activity, int companyId, int noticeId, AppNotice_Callback appNotice_callback, boolean isImpliedMode) {
//        initAppNotice(activity, companyId, noticeId, appNotice_callback, isImpliedMode);
//    }
//
//    /**
//     * AppNotice initializer: Used for common functionality between the constructors
//     * @param activity: Usually your start-up activity.
//     * @param companyId: The company ID assigned to you for the App Notice SDK.
//     * @param noticeId: The notice ID of the configuration created for this app.
//     * @param appNotice_callback: An AppNotice_Callback object that handles the various callbacks from the SDK to the host app.
//     * @param isImpliedMode: Initialize the SDK in either implied or explicit mode: true = implied; false = explicit.
//     */
//    private void initAppNotice(Activity activity, int companyId, int noticeId, AppNotice_Callback appNotice_callback, boolean isImpliedMode) {
//        this.isImpliedMode = isImpliedMode;
//        AppNoticeData.usingToken = false;
//        extActivity = activity;
//        if ((companyId <= 0) || (noticeId <= 0)) {
//            throw(new IllegalArgumentException("Company ID and notice ID must both be valid identifiers."));
//        }
//
//        // Remember the provided callback
//        this.appNotice_callback = appNotice_callback;
//        AppNotice_Activity.appNotice_callback = appNotice_callback;
//
//        // Get either a new or initialized tracker config object
//        appNoticeData = AppNoticeData.getInstance(activity);
//
//        // Keep track of the company ID and the notice ID
//        appNoticeData.setCompanyId(companyId);
//        appNoticeData.setCurrentNoticeId(noticeId);
//        AppNoticeData.appContext = activity.getApplicationContext();
//    }

    /**
     * Starts the App Notice Implied Consent flow with an option to specify max displays in a 30-day period.
     * Should be called before your app begins any tracking activity.
     *   0 displays on first start and every notice ID change (recommended).
     *   1+ is the max number of times to display the consent screen on start up in a 30-day period.
     */
    public void startConsentFlow(int implied30dayDisplayMax) {
        this.implied30dayDisplayMax = implied30dayDisplayMax;
        AppNoticeData.implied30dayDisplayMax = implied30dayDisplayMax;
        startConsentFlow();
    }

    /**
     * Starts the App Notice Implied Consent flow. Must be called before your app begins any tracking activity.
     */
    public void startConsentFlow() {
        startConsentFlow(true);

        // Send notice for this event
        if (isImpliedMode) {
            AppNoticeData.sendNotice(AppNoticeData.pingEvent.IMPLIED_CONSENT_START);
        } else {
            AppNoticeData.sendNotice(AppNoticeData.pingEvent.EXPLICIT_CONSENT_START);
        }
    }

    /**
     * Resets the session and persistent values that AppNotice SDK uses to manage the dialog display frequency.
     */
    public void resetSDK() {
        AppNoticeData.resetSessionData();
        AppData.remove(AppData.APPDATA_IMPLIED_LAST_DISPLAY_TIME);
        AppData.remove(AppData.APPDATA_IMPLIED_DISPLAY_COUNT);
        AppData.remove(AppData.APPDATA_EXPLICIT_ACCEPTED);
        AppData.remove(AppData.APPDATA_TRACKERSTATES);
        AppData.remove(AppData.APPDATA_PREV_APP_NOTICE_TOKEN);
        AppData.remove(AppData.APPDATA_PREV_NOTICE_ID);
        AppData.remove(AppData.APPDATA_PREV_JSON);
    }

    /**
     * Shows the Manage Preferences screen. Can be called from your when the end user requests access to this screen (e.g., from a menu or button click).
     *   - fragmentActivity: Usually your current fragmentActivity
     */
    public void showManagePreferences() {
        startConsentFlow(false);
    }

    private void startConsentFlow(final boolean isConsentFlow) {
        if (!appNoticeData.isInitialized()) {
            appNoticeData.init();
        }

        // Start getting the tracker list before we display the consent dialog or the manage preferences screen
        if (appNoticeData.isTrackerListInitialized()) {
            // If initialized, use what we have
            if (isConsentFlow) {
                startConsentFlowFragment();
            } else {
                // Open the App Notice manage preferences fragment
                Util.showManagePreferences(extActivity);
            }
        } else {

            Log.d(TAG, "Starting initTrackerList from AppNotice startConsentFlow.");
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    appNoticeData.initTrackerList(new JSONGetterCallback() {

                        @Override
                        public void onTaskDone() {
                            // Do nothing
                            Log.d(TAG, "Done with initTrackerList from AppNotice startConsentFlow.");

                            // Now that it is initialized, use it
                            if (isConsentFlow) {
                                startConsentFlowFragment();
                            } else {
                                // Open the App Notice manage preferences fragment
                                Util.showManagePreferences(extActivity);
                            }
                        }
                    });
                }
            }, Util.THREAD_INITTRACKERLIST);
            thread.start();
        }

    }

    private void startConsentFlowFragment() {
        // appNoticeData should always be initialized at this point

        if (appNoticeData == null || !appNoticeData.isInitialized()) {
            // This handles a rare case where the app object has been killed, but the SDK activity continues to run.
            // This forces the app to restart in a way that the SDK gets properly initialized.
            // TODO: Should this be a callback to the host app?
            Log.d(TAG, "Force restart the host app to correctly startConsentFlow the SDK.");
            Util.forceAppRestart(extActivity);
        } else {
            // Determine if we need to show this Implied Notice dialog box
            Boolean showNotice = true;
            if (isImpliedMode) {
                showNotice = appNoticeData.getImpliedNoticeDisplayStatus();
            } else {
                showNotice = appNoticeData.getExplicitNoticeDisplayStatus();
            }

            if (showNotice) {
                FragmentManager fm = extActivity.getFragmentManager();
                FragmentTransaction fragmentTransaction = fm.beginTransaction();

                // Create and show the dialog.
                if (isImpliedMode) {
                    Intent intent = new Intent(extActivity, AppNotice_Activity.class);
                    intent.putExtra("FRAGMENT_TYPE", AppNotice_Activity.FRAGMENT_TAG_IMPLIED_CONSENT);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    extActivity.startActivity(intent);

                    // Count that this Implied Notice dialog box was displayed
                    AppNoticeData.incrementImpliedNoticeDisplayCount();

                } else {
                    Intent intent = new Intent(extActivity, AppNotice_Activity.class);
                    intent.putExtra("FRAGMENT_TYPE", AppNotice_Activity.FRAGMENT_TAG_EXPLICIT_CONSENT);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    extActivity.startActivity(intent);
                }

                // Remember that a notice has been shown for this notice ID
                if (usingToken) {
                    appNoticeData.setPreviousAppNoticeToken(appNoticeData.getCurrentAppNoticeToken());
                } else {
                    appNoticeData.setPreviousNoticeId(appNoticeData.getCurrentNoticeId());
                }

            } else {
                // If not showing a notice, let the host app know
                Boolean isAccepted = getAcceptedState();
                Log.d(TAG, "optionalTrackerArrayList size = " + appNoticeData.optionalTrackerArrayList.size());
                HashMap<Integer, Boolean> trackerHashMap = appNoticeData.getTrackerHashMap(true);
                Log.d(TAG, "trackerHashMap size = " + trackerHashMap.size());
                appNotice_callback.onNoticeSkipped(isAccepted, trackerHashMap);
            }
        }
    }

    public HashMap<Integer, Boolean> getTrackerPreferences() {
        return AppNoticeData.getTrackerPreferences();
    }

    public boolean getAcceptedState() {
        Boolean isAccepted = false;
        if (isImpliedMode) {
            int displayCount = AppData.getInteger(AppData.APPDATA_IMPLIED_DISPLAY_COUNT, 0);
            isAccepted = displayCount > 0? true : false;
        } else {
            isAccepted = AppData.getBoolean(AppData.APPDATA_EXPLICIT_ACCEPTED, false);
        }
        return isAccepted;
    }

}
