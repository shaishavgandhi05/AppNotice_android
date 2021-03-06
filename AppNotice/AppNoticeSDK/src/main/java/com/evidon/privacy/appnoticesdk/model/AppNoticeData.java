package com.evidon.privacy.appnoticesdk.model;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.evidon.privacy.appnoticesdk.AppNotice;
import com.evidon.privacy.appnoticesdk.R;
import com.evidon.privacy.appnoticesdk.callbacks.JSONGetterCallback;
import com.evidon.privacy.appnoticesdk.utils.AppData;
import com.evidon.privacy.appnoticesdk.utils.ServiceHandler;
import com.evidon.privacy.appnoticesdk.utils.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;


/**
 * Created by Steven.Overson on 2/26/2015.
 */

// Never instantiate directly. Use getInstance() instead.
public class AppNoticeData {
    private static final String TAG = "AppNoticeData";

    private static AppNoticeData instance;
    private static Activity activity;
    private ProgressDialog progressDialog;
    private boolean isTrackerListInitialized = false;
    private boolean isInitialized = false;
    private static int companyId;
    private static String currentAppNoticeToken;
    private static String previousAppNoticeToken;
    private static int currentNoticeId;
    private static int previousNoticeId;
    private int implied_flow_session_display_max_default = 1;
    private int consent_flow_dialog_opacity_default = 100;
    private final Object waitObj = new Object();
    private boolean gettingTrackerList = false;
    private ArrayList<Tracker> trackerArrayList = new ArrayList<>();
    public ArrayList<Tracker> optionalTrackerArrayList = new ArrayList<>();
    public ArrayList<Tracker> essentialTrackerArrayList = new ArrayList<>();
    public static Context appContext;
    public static boolean usingToken = true;
    public static int implied30dayDisplayMax = 0;  // Default to mode-0. 0 displays on first start and every notice ID change. 1+ is the max number of times to display the consent screen on start up in a 30-day period.
    public static int impliedFlow_SessionCount;
    private static boolean sendConsentStartNoticeLater = true;

    private final static long ELAPSED_30_DAYS_MILLIS = 2592000000L;     // Number of milliseconds in 30 days

    // 0 = company ID; 1 = pub-notice ID
    private final static String URL_JSON_REQUEST = "https://c.betrad.com/pub/c/{0}/{1}.js";
    private final static String URL_JSON_REQUEST_VIA_TOKEN = "http://privacyapi.evidon.com/api/v1/appnotice/configuration/";

    // Opt-in/out ping-back
    // 0 = Publisher ID; 1 = Owner Company ID, 2 = trackerId; 3 = optOut; 4 = uniqueVisit; 5 = firstOptOut; 6 = selectAll
    private final static String URL_SDK_OPT_IN_OUT = "https://l.betrad.com/oo/p.gif?ocid={0}&pid={1}&c={2}&et={3}&mb=4";

    // Event ping-back URLs for implied
    private static String URL_SDK_IMPLIED_CONSENT_START;
    private static String URL_SDK_IMPLIED_CONTINUE;
    private static String URL_SDK_IMPLIED_PREF_CONSENT;
    private static String URL_SDK_IMPLIED_PREF_DIRECT;

    // Event ping-back URLs for explicit
    private static String URL_SDK_EXPLICIT_CONSENT_START;
    private static String URL_SDK_EXPLICIT_ACCEPT;
    private static String URL_SDK_EXPLICIT_DECLINE;
    private static String URL_SDK_EXPLICIT_PREF_CONSENT;
    private static String URL_SDK_EXPLICIT_PREF_DIRECT;

    public enum pingEvent {
        IMPLIED_CONSENT_START,
        IMPLIED_CONTINUE,
        IMPLIED_PREF_CONSENT,
        IMPLIED_PREF_DIRECT,
        EXPLICIT_CONSENT_START,
        EXPLICIT_ACCEPT,
        EXPLICIT_DECLINE,
        EXPLICIT_PREF_CONSENT,
        EXPLICIT_PREF_DIRECT
    }

    // Non-JSON outer characters that surround the JSON structure
    private static final String NON_JSON_PREFIX = "__ev_hover.s(";
    private static final String NON_JSON_POSTFIX = ");";
    private static final String FILE_NOT_FOUND = "File not found";

    // Field tags
    private static final String TAG_COMPANY_ID = "companyId";                                           // Company ID (via token)
    private static final String TAG_NOTICE_ID = "id";                                                   // Notice ID (via token)
    private static final String TAG_TRACKERS_VIA_TOKEN = "vendors";                                     // Tracker list
    private static final String TAG_TRACKERS = "trackers";                                              // Tracker list

    // Field values
    private int implied_flow_session_display_max; // Maximum number of times the Implied Consent dialog should be displayed in a session.


    // Public getters and setters
    public Boolean isTrackerListInitialized() { return isTrackerListInitialized; }
    public Boolean isInitialized() { return isInitialized; }
    public int getCompanyId() { return companyId; }
    public void setCompanyId(int companyId) { this.companyId = companyId; }
    public String getCurrentAppNoticeToken() { return currentAppNoticeToken; }
    public void setCurrentAppNoticeToken(String currentAppNoticeToken) { AppNoticeData.currentAppNoticeToken = currentAppNoticeToken; }
    public void setPreviousAppNoticeToken(String previousAppNoticeToken) {
        this.previousAppNoticeToken = previousAppNoticeToken;
        AppData.setString(AppData.APPDATA_PREV_APP_NOTICE_TOKEN, previousAppNoticeToken);
    }
    public int getCurrentNoticeId() { return currentNoticeId; }
    public void setCurrentNoticeId(int currentNoticeId) { this.currentNoticeId = currentNoticeId; }
    public void setPreviousNoticeId(int previousNoticeId) {
        this.previousNoticeId = previousNoticeId;
        AppData.setInteger(AppData.APPDATA_PREV_NOTICE_ID, previousNoticeId);
    }
    public int getImpliedFlowSessionDisplayMax() { return implied_flow_session_display_max; }


    // Single instance
    public static synchronized AppNoticeData getInstance(Activity _activity)
    {
        activity = _activity;

        // Ensure the app only uses one instance of this class.
        if (instance == null) {
            instance = new AppNoticeData();
        }

        return instance;
    }

    // Constructor
    private AppNoticeData() {
        instance = this;

        // Build the event ping-back URL strings
        String sdkVersionCode = String.valueOf(AppNotice.sdkVersionCode);
        String appVersion = getAppVersion();
        String osVersion = Build.VERSION.RELEASE;
        String language = Locale.getDefault().toString();
        String versionInfo = "&v=" + sdkVersionCode + "&av=" + appVersion + "&os=" + osVersion + "&l=" + language;

        URL_SDK_IMPLIED_CONSENT_START =  "http://l.betrad.com/pub/p.gif?ocid={0}&pid={1}&nt=11&mb=4" + versionInfo;    //pid, ocid, nt = 11, v, av, mb, os, l
        URL_SDK_IMPLIED_CONTINUE =       "http://l.betrad.com/pub/p.gif?ocid={0}&pid={1}&nt=11&mb=4&aa=1";    //pid, ocid, nt = 11, aa = 1, mb
        URL_SDK_IMPLIED_PREF_CONSENT =   "http://l.betrad.com/pub/p.gif?ocid={0}&pid={1}&nt=11&mb=4&u=1";    //pid, ocid, nt = 11, u = 1, mb
        URL_SDK_IMPLIED_PREF_DIRECT =    "http://l.betrad.com/pub/p.gif?ocid={0}&pid={1}&nt=11&mb=4&u=0" + versionInfo;    //pid, ocid, nt = 11, u = 0,  v, av, mb, os, l

        URL_SDK_EXPLICIT_CONSENT_START = "http://l.betrad.com/pub/p.gif?ocid={0}&pid={1}&nt=12&mb=4" + versionInfo;    //pid, ocid, nt = 12, v, av, mb, os, l
        URL_SDK_EXPLICIT_ACCEPT =        "http://l.betrad.com/pub/p.gif?ocid={0}&pid={1}&nt=12&mb=4&aa=1";    //pid, ocid, nt = 12, aa = 1, mb
        URL_SDK_EXPLICIT_DECLINE =       "http://l.betrad.com/pub/p.gif?ocid={0}&pid={1}&nt=12&mb=4&aa=0";    //pid, ocid, nt = 12, aa = 0, mb
        URL_SDK_EXPLICIT_PREF_CONSENT =  "http://l.betrad.com/pub/p.gif?ocid={0}&pid={1}&nt=12&mb=4&u=1";    //pid, ocid, nt = 12, u = 1, mb
        URL_SDK_EXPLICIT_PREF_DIRECT =   "http://l.betrad.com/pub/p.gif?ocid={0}&pid={1}&nt=12&mb=4&u=0" + versionInfo;    //pid, ocid, nt = 12, u = 0, v, av, mb, os, l

        // Pre-populate the max values with defaults just in case the JSON object can't be retrieved
        implied_flow_session_display_max = implied_flow_session_display_max_default = activity.getResources().getInteger(R.integer.evidon_implied_flow_session_display_max);
    }

    public static String getAppVersion() {
        String appVersionName = "";
        final PackageManager packageManager = activity.getPackageManager();
        if (packageManager != null) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(activity.getPackageName(), 0);
                int appVersionCode = packageInfo.versionCode;
                appVersionName = packageInfo.versionName + "." + String.valueOf(appVersionCode);
            } catch (PackageManager.NameNotFoundException e) {
                appVersionName = "";
            }
        }
        return appVersionName;
    }

    public HashMap<Integer, Boolean> getTrackerHashMap(boolean useTrackerIdAsInt) {
        HashMap trackerHashMap = new HashMap();

        // Loop through the tracker list and add non-essential tracker IDs and their on/off state
        for (Tracker tracker : trackerArrayList) {
            if (!tracker.isEssential() && !isTrackerDuplicateOfEssentialTracker(tracker.getTrackerId())) {
                if (useTrackerIdAsInt)
                    trackerHashMap.put(tracker.getTrackerId(), tracker.isOn());
                else
                    trackerHashMap.put(Integer.toString(tracker.getTrackerId()), tracker.isOn());
            }
        }

        return trackerHashMap;
    }

    public ArrayList<Tracker> getOptionalTrackerListClone() {
        ArrayList<Tracker> trackerArrayListClone = new ArrayList<>();

        // Loop through the optional tracker list and add all tracker IDs and their on/off state
        for (Tracker tracker : optionalTrackerArrayList) {
            trackerArrayListClone.add(new Tracker(tracker));
        }
        return trackerArrayListClone;
    }

    // Returns requested tracker. If not found, returns null.
    public Tracker getTrackerById(int uId) {
        // Loop through the tracker list to look for the requested tracker
        for (Tracker tracker : trackerArrayList) {
            if (tracker.uId == uId) {
                return tracker;
            }
        }
        return null;
    }

    // Returns true if there are essential trackers in the list, else false
    public boolean hasEssentialTrackers() {
        boolean hasEssential = essentialTrackerArrayList.size() > 0;
        return hasEssential;
    }

    // Returns true if there are optional trackers in the list that aren't duplicates of essential trackers, else false
    public boolean hasOptionalTrackers() {
        boolean hasOptional = false;
        for (Tracker tracker : optionalTrackerArrayList) {
            if (!isTrackerDuplicateOfEssentialTracker(tracker.getTrackerId())) {
                hasOptional = true;
                break;  // Stop loop after first detection
            }
        }
        return hasOptional;
    }

    // Sets all the specified non-essential tracker (and all duplicate non-essential trackers) on/off state to the specified value.
    public void setTrackerOnOffState(int uId, boolean isOn) {
        Tracker selectedTracker = getTrackerById(uId);  // Get the tracker object for the specified tracker
        if (!selectedTracker.isEssential() && !isTrackerDuplicateOfEssentialTracker(selectedTracker.getTrackerId())) {
            int selectedTrackerId = selectedTracker.getTrackerId();
            for (Tracker tracker : optionalTrackerArrayList) {
                if (tracker.getTrackerId() == selectedTrackerId) {
                    tracker.setOnOffState(isOn);
                }
            }
        }
    }

//    // Sets all non-essential tracker on/off states to the specified value.
//    public void setTrackerOnOffState(boolean isOn) {
//        for (Tracker tracker : optionalTrackerArrayList) {
//            if (!tracker.isEssential() && !isTrackerDuplicateOfEssentialTracker(tracker.getTrackerId())) {
//                tracker.setOnOffState(isOn);
//            }
//        }
//    }
//
//    // Returns 1 if all on; 0 if some on and some off; -1 if all off
//    public int getTrackerOnOffStates() {
//        int trackerCount = 0;
//        int trackerOnCount = 0;
//        for (Tracker tracker : optionalTrackerArrayList) {
//            if (!tracker.isEssential() && !isTrackerDuplicateOfEssentialTracker(tracker.getTrackerId())) {
//                trackerCount++;
//                if (tracker.isOn()) {
//                    trackerOnCount++;
//                }
//            }
//        }
//
//        int trackerOnOffStates = trackerOnCount == trackerCount ? 1 : trackerOnCount == 0 ? -1 : 0;
//        return trackerOnOffStates;
//    }
//
//    // Returns the number of non-essential trackers
//    public int getNonEssentialTrackerCount() {
//        int nonEssentialTrackerCount = 0;    // Assume no changes
//
//        // Count non-essential trackers
//        for (int i = 0; i < optionalTrackerArrayList.size(); i++) {
//            Tracker tracker = optionalTrackerArrayList.get(i);
//
//            // If the tracker is non-essential...
//            if (!tracker.isEssential() && !isTrackerDuplicateOfEssentialTracker(tracker.getTrackerId())) {
//                nonEssentialTrackerCount++;
//            }
//        }
//
//        return nonEssentialTrackerCount;
//    }

    public boolean isTrackerDuplicateOfEssentialTracker(int trackerId) {
        Boolean isTrackerDuplicateOfEssentialTracker = false;    // Assume not a duplicate

        // Look for duplicate essential trackers
        for (int i = 0; i < trackerArrayList.size(); i++) {
            Tracker tracker = trackerArrayList.get(i);

            // If the tracker is essential...
            if (tracker.isEssential()) {
                // If the tracker is a duplicate...
                if (tracker.getTrackerId() == trackerId) {
                    isTrackerDuplicateOfEssentialTracker = true;
                    break;
                }
            } else {
                // All essential trackers should be listed first, so when we get to a non-essential tracker, we can stop looking
                break;
            }
        }

        return isTrackerDuplicateOfEssentialTracker;
    }

    // Returns the number of non-essential trackers that have changed on/off state since the original tracker was captured
    public int getTrackerStateChangeCount(ArrayList<Tracker> originalTrackerArrayList) {
        int changeCount = 0;    // Assume no changes

        // Send opt-in/out ping-back for each changed non-essential tracker
        if (optionalTrackerArrayList != null && originalTrackerArrayList != null &&
                optionalTrackerArrayList.size() == originalTrackerArrayList.size()) {

            for (int i = 0; i < optionalTrackerArrayList.size(); i++) {
                Tracker tracker = optionalTrackerArrayList.get(i);
                Tracker originalTracker = originalTrackerArrayList.get(i);

                // If the tracker is non-essential and is changed...
                if (!tracker.isEssential() && (tracker.isOn() != originalTracker.isOn())) {
                    changeCount++;
                }
            }
        }

        return changeCount;
    }

    // Sends an opt-in/out ping back through the opt-in-out chanel
    public static void sendOptInOutNotice(final int trackerId, final boolean optOut) {
        // Use a non-UI thread
        new Thread(){
            public void run(){
                Object[] urlParams = new Object[7];
                urlParams[0] = String.valueOf(companyId);		// 0
                urlParams[1] = String.valueOf(currentNoticeId);	// 1
                urlParams[2] = String.valueOf(trackerId);		// 2
                urlParams[3] = optOut ? "1" : "0";  		    // 3

                String uRL = MessageFormat.format(URL_SDK_OPT_IN_OUT, urlParams);

                if (uRL != null && uRL.length() > 0) {
                    Log.d(TAG, "Sending notice beacon: (type=OptInOut) " + uRL);
                    try{
                        ServiceHandler serviceHandler = new ServiceHandler();
                        String resp = serviceHandler.getRequest(uRL);
                        Log.d(TAG, "Notice beacon resp: " + resp);
                    }catch(Exception e){
                        Log.e(TAG, "Error sending notice beacon: (type=OptInOut) " + uRL, e);
                    }
                } else {
                    Log.e(TAG, "No URL found for Opt-In/Out.");
                }
            }
        }.start();
    }

    // Sends a report back through the Site Notice Channel
    public static void sendNotice(final pingEvent type) {
        if (currentNoticeId > 0) {
            // If we already have the company ID and notice ID, send now
            // Use a non-UI thread
            new Thread(){
                public void run(){
                    String uRL = "";

                    switch (type) {
                        case IMPLIED_CONSENT_START:
                            uRL = URL_SDK_IMPLIED_CONSENT_START;
                            break;
                        case IMPLIED_CONTINUE:
                            uRL = URL_SDK_IMPLIED_CONTINUE;
                            break;
                        case IMPLIED_PREF_CONSENT:
                            uRL = URL_SDK_IMPLIED_PREF_CONSENT;
                            break;
                        case IMPLIED_PREF_DIRECT:
                            uRL = URL_SDK_IMPLIED_PREF_DIRECT;
                            break;
                        case EXPLICIT_CONSENT_START:
                            uRL = URL_SDK_EXPLICIT_CONSENT_START;
                            break;
                        case EXPLICIT_ACCEPT:
                            uRL = URL_SDK_EXPLICIT_ACCEPT;
                            break;
                        case EXPLICIT_DECLINE:
                            uRL = URL_SDK_EXPLICIT_DECLINE;
                            break;
                        case EXPLICIT_PREF_CONSENT:
                            uRL = URL_SDK_EXPLICIT_PREF_CONSENT;
                            break;
                        case EXPLICIT_PREF_DIRECT:
                            uRL = URL_SDK_EXPLICIT_PREF_DIRECT;
                            break;
                    }

                    if (uRL != null && uRL.length() > 0) {
                        Object[] urlParams = new Object[7];
                        urlParams[0] = String.valueOf(companyId);		// 0
                        urlParams[1] = String.valueOf(currentNoticeId);	// 1
                        uRL = MessageFormat.format(uRL, urlParams);

                        Log.d(TAG, "Sending notice beacon: (type=" + type + ") " + uRL);
                        try{
                            // if (Network.isNetworkAvailable(fragmentActivity)) {
                            //     // Split the supplied URL into usable parts for the service call
                            //     String[] uRlParts = uRL.split("\\?");
                            //     String[] params = uRlParts[1].split("\\&");
                            //     List paramList = new ArrayList();
                            //     for (String param : params) {
                            //         String[] paramParts = param.split("\\=");
                            //         BasicNameValuePair nameValuePair = new BasicNameValuePair(paramParts[0], paramParts[1]);
                            //         paramList.add(nameValuePair);
                            //     }
                            //
                            //     ServiceHandler sh = new ServiceHandler();
                            //     String temp = sh.makeServiceCall(uRlParts[0], ServiceHandler.POST, paramList);
                            // } else {
                            //     Log.e(TAG, "No network connection for sending notice beacon: (type=" + type + ")" + uRL);
                            // }
                            ServiceHandler serviceHandler = new ServiceHandler();
                            String temp = serviceHandler.getRequest(uRL);
                        }catch(Exception e){
                            Log.e(TAG, "Error sending notice beacon: (type=" + type + ")" + uRL, e);
                        }
                    } else {
                        Log.e(TAG, "No URL found due to incorrect notification type.");
                    }
                }
            }.start();
            sendConsentStartNoticeLater = false;
        } else {
            // If we don't have the company ID and notice ID, send later
            sendConsentStartNoticeLater = true;
        }
    }

    // Init
    public void init() {
        implied_flow_session_display_max = implied_flow_session_display_max_default;

        previousNoticeId = AppData.getInteger(AppData.APPDATA_PREV_NOTICE_ID, 0);
        previousAppNoticeToken = AppData.getString(AppData.APPDATA_PREV_APP_NOTICE_TOKEN, "");

        isInitialized = true;
    }

    // Init
    public void initTrackerList(final JSONGetterCallback mJSONGetterCallback) {
        assert(Thread.currentThread().getName().equals(Util.THREAD_INITTRACKERLIST));
        synchronized(waitObj) {
            while (gettingTrackerList) {
                try {
                    Log.d(TAG, "initTrackerList: Waiting for tracker list to be filled.");
                    waitObj.wait();
                } catch (InterruptedException e) {
                    // Do nothing
                    Log.d(TAG, "initTrackerList: Wait interrupted while filling tracker list.");
                }
            }
            Log.d(TAG, "initTrackerList: gettingTrackerList = " + Boolean.toString(gettingTrackerList));

            // Check to see if we have a current cached tracker list
            String previousJson = AppData.getString(AppData.APPDATA_PREV_JSON, "");
            if (currentNoticeId == previousNoticeId && !previousJson.isEmpty()) {
                fillTrackerList(previousJson);

                // Restore the selection states of the trackers
                restoreTrackerStates();

                // Let the specified callback know it finished...
                if (mJSONGetterCallback != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mJSONGetterCallback.onTaskDone();
                        }
                    });
                }

                // Save the tracker states
//                saveTrackerStates();
            } else {
                // Start the call to get the AppNoticeData data from the service...from the UI thread
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gettingTrackerList = true;
                        JSONGetter mJSONGetter = new JSONGetter(mJSONGetterCallback);
                        mJSONGetter.execute();
                    }
                });
            }
        }
    }

    // Determine if the Implied notice should be shown. True = show notice; False = don't show notice.
    public boolean getImpliedNoticeDisplayStatus() {
        Boolean showNotice = true;     // Assume we need to show the notice

        if (implied30dayDisplayMax <= 0) {
            // If the notice ID has changed, we need to show the notice again
            if (usingToken) {
                if (currentAppNoticeToken.equals(previousAppNoticeToken)) {
                    showNotice = false;
                } else {
                    showNotice = true;
                }
            } else {
                if (currentNoticeId == previousNoticeId) {
                    showNotice = false;
                } else {
                    showNotice = true;
                }
            }
        } else {
            long currentTime = System.currentTimeMillis();
            int implied_display_count = (int) AppData.getInteger(AppData.APPDATA_IMPLIED_DISPLAY_COUNT, 0);
            long implied_last_display_time = (long) AppData.getLong(AppData.APPDATA_IMPLIED_LAST_DISPLAY_TIME, 0L);

            if (implied_last_display_time == 0L) {     // If this is the first pass...
                implied_last_display_time = currentTime;
                AppData.setLong(AppData.APPDATA_IMPLIED_LAST_DISPLAY_TIME, implied_last_display_time);
            }

            if (impliedFlow_SessionCount >= implied_flow_session_display_max) { // If displayed enough in this session...
                showNotice = false; // don't display it now
            } else {
                if (currentTime <= implied_last_display_time + ELAPSED_30_DAYS_MILLIS) { // If displayed less than 30 days ago...
                    if (implied_display_count >= AppNoticeData.implied30dayDisplayMax) { // If displayed enough in last 30 days...
                        showNotice = false; // don't display it now
                    }
                } else {
                    // If it's been more than 30 days...
                    AppData.setInteger(AppData.APPDATA_IMPLIED_DISPLAY_COUNT, 0); // Reset the display count to 0
                    AppData.setLong(AppData.APPDATA_IMPLIED_LAST_DISPLAY_TIME, currentTime); // And reset the last display time
                }
            }
        }

        return showNotice;
    }

    // Determine if the Explicit notice should be shown. True = show notice; False = don't show notice.
    public boolean getExplicitNoticeDisplayStatus() {
        Boolean displayStatus = true;     // Assume we need to show the notice
        if (usingToken) {
            if (currentAppNoticeToken.equals(previousAppNoticeToken)) {
                // If the token is the same, see if this has been accepted
                Boolean isExplicitAccepted = (boolean) AppData.getBoolean(AppData.APPDATA_EXPLICIT_ACCEPTED, false);
                displayStatus = !isExplicitAccepted;     // If not accepted, display notice; and vice-versa
            } else {
                // If the token has changed, we need to show the notice again
                displayStatus = true;
            }
        } else {
            if (currentNoticeId != previousNoticeId) {
                // If the notice ID has changed, we need to show the notice again
                displayStatus = true;
            } else {
                // If the notice ID is the same, see if this has been accepted
                Boolean isExplicitAccepted = (boolean) AppData.getBoolean(AppData.APPDATA_EXPLICIT_ACCEPTED, false);
                displayStatus = !isExplicitAccepted;     // If not accepted, display notice; and vice-versa
            }

            }
        return displayStatus;
    }

    public static void incrementImpliedNoticeDisplayCount() {
        long implied_last_display_time = (long) AppData.getLong(AppData.APPDATA_IMPLIED_LAST_DISPLAY_TIME, 0L);

        // Increment the implied display count
        int currentDisplayCount = AppData.getInteger(AppData.APPDATA_IMPLIED_DISPLAY_COUNT, 0);
        AppData.setInteger(AppData.APPDATA_IMPLIED_DISPLAY_COUNT, currentDisplayCount + 1);

        // Increment the implied session display count
        impliedFlow_SessionCount++;

        long currentTime = System.currentTimeMillis();
        if (currentDisplayCount == 0) {             // If this is the first time being displayed in this 30-day period...
            AppData.setLong(AppData.APPDATA_IMPLIED_LAST_DISPLAY_TIME, currentTime);   //   reset the last display time to now
        }
    }

    public void saveTrackerStates() {
        HashMap trackerHashMap = getTrackerHashMap(false);     // Use tracker ID as a string
        if (!trackerHashMap.isEmpty()) {    // Don't override last saved states if we don't have any trackers in the map
            JSONObject trackerStateJSONObject = new JSONObject(trackerHashMap);
            String trackerStatesString = trackerStateJSONObject.toString();
            AppData.setString(AppData.APPDATA_TRACKERSTATES, trackerStatesString);
        }
    }

    public void restoreTrackerStates() {
        HashMap trackerHashMap = getTrackerPreferences();

        // Look for a non-essential tracker with the same ID. If found, set its state.
        for (Tracker tracker : optionalTrackerArrayList) {
            if (!tracker.isEssential()) {
                Boolean trackerState = (Boolean)trackerHashMap.get(tracker.getTrackerId());

                if (trackerState != null)
                    tracker.setOnOffState(trackerState);
            }
        }
    }

    // Gets the tracker preferences directly from AppData just in case the AppNoticeData object hasn't been inite'ed yet.
    public static HashMap getTrackerPreferences() {
        HashMap trackerHashMap = new HashMap();

        String trackerStatesString = AppData.getString(AppData.APPDATA_TRACKERSTATES);

        if (trackerStatesString.length() > 2) {
            // Strip off the brackets
            trackerStatesString = trackerStatesString.substring(1, trackerStatesString.length() - 1);
            String[] trackerStatesArray = trackerStatesString.split(",");

            for (String trackerStatePairString : trackerStatesArray) {
                String[] trackerStateArray = trackerStatePairString.split(":");
                String trackerIdString = trackerStateArray[0];
                String trackerStateString = trackerStateArray[1];

                // Strip off the quote marks
                trackerIdString = trackerIdString.substring(1, trackerIdString.length() - 1);

                // Get the actual key/value pair
                int trackerId = Integer.parseInt(trackerIdString);
                Boolean trackerState = Boolean.parseBoolean(trackerStateString);

                // Add this tracker state to the hashmap
                trackerHashMap.put(trackerId, trackerState);

                Log.d(TAG, trackerIdString + " : " + trackerStatePairString);
            }
        }

        return trackerHashMap;
    }

    public void fillTrackerList(String jsonStr) {
        try {
            JSONObject jsonObj = null;

            if (jsonStr != null && jsonStr.length() > 20 && !jsonStr.startsWith(FILE_NOT_FOUND)){
                // Strip off the not-JSON outer characters
                if (jsonStr.startsWith(NON_JSON_PREFIX))
                    jsonStr = jsonStr.substring(NON_JSON_PREFIX.length());
                if (jsonStr.endsWith(NON_JSON_POSTFIX))
                    jsonStr = jsonStr.substring(0, jsonStr.length() - NON_JSON_POSTFIX.length());

                jsonObj = new JSONObject(jsonStr);
            }

            if (jsonObj != null) {
                try {
                    String trackerJSONString;
                    if (usingToken) {
                        companyId = jsonObj.getInt(TAG_COMPANY_ID);
                        currentNoticeId = jsonObj.getInt(TAG_NOTICE_ID);

                        if (sendConsentStartNoticeLater) {
                            // Send notice for this event
                            if (AppNotice.isImpliedMode) {
                                AppNoticeData.sendNotice(AppNoticeData.pingEvent.IMPLIED_CONSENT_START);
                            } else {
                                AppNoticeData.sendNotice(AppNoticeData.pingEvent.EXPLICIT_CONSENT_START);
                            }
                        }

                        trackerJSONString = jsonObj.isNull(TAG_TRACKERS_VIA_TOKEN)? null : jsonObj.getString(TAG_TRACKERS_VIA_TOKEN);
                    } else {
                        trackerJSONString = jsonObj.isNull(TAG_TRACKERS)? null : jsonObj.getString(TAG_TRACKERS);
                    }

                    if (trackerJSONString != null) {
                        JSONArray trackerJSONArray = new JSONArray(trackerJSONString);
                        trackerArrayList.clear();
                        optionalTrackerArrayList.clear();
                        essentialTrackerArrayList.clear();

                        int id;
                        for (int i = 0; i < trackerJSONArray.length(); i++) {
                            JSONObject trackerJSONObject = trackerJSONArray.getJSONObject(i);
                            Tracker tracker = new Tracker(trackerJSONObject);
                            trackerArrayList.add(tracker);
                            Log.d(TAG, "Add tracker: " + tracker.getTrackerId() + " (" + tracker.getName() + ")");
                        }

                        // Sort by category and then by name within category
                        Collections.sort(trackerArrayList, new Comparator<Tracker>() {
                            @Override
                            public int compare(Tracker tracker1, Tracker tracker2) {
                                int result = 0;

                                // Sort first by category...keeping "Essential" at the top
                                if (tracker1.isEssential() && tracker2.isEssential()) {
                                    result = 0;
                                } else if (tracker1.isEssential()) {
                                    result = -1;
                                } else if (tracker2.isEssential()) {
                                    result = 1;
                                } else {
                                    // Sort by non-essential category
                                    String tracker1_category = tracker1.getCategory().toUpperCase();
                                    String tracker2_category = tracker2.getCategory().toUpperCase();

                                    //ascending order
                                    result = tracker1_category.compareTo(tracker2_category);
//                                    result = tracker1.getCategory().compareToIgnoreCase(tracker2.getCategory());
                                }

                                // If it's in the same category, then sort by tracker name
                                if (result == 0) {
                                    String tracker1_name = tracker1.getName().toUpperCase();
                                    String tracker2_name = tracker2.getName().toUpperCase();

                                    //ascending order
                                    result = tracker1_name.compareTo(tracker2_name);
//                                    result = tracker1.getName().compareToIgnoreCase(tracker2.getName());
                                }
                                return result;
                            }
                        });

                        // Fill essential and optional tracker array lists
                        for (int i = 0; i < trackerArrayList.size(); i++) {
                            Tracker tracker = trackerArrayList.get(i);
                            tracker.uId = i;        // Set the tracker's unique ID

                            // If the tracker is non-essential and is changed...
                            if (tracker.isEssential()) {
                                essentialTrackerArrayList.add(tracker);
                            } else {
                                optionalTrackerArrayList.add(tracker);
                            }
                        }
                    }

                    isTrackerListInitialized = true;
                } catch (JSONException e) {
                    Log.e(TAG, "JSONException while parsing the JSON object.", e);
                }
            }

        } catch (NumberFormatException e) {
            Log.e(TAG, "Exception while parsing elements of the JSON object", e);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "NotFoundException while getting the JSON object", e);
        } catch (Exception e) {
            Log.e(TAG, "Exception while getting or parsing the JSON object.", e);
        }
    }


    // =================================================================================
    // =================================================================================

    // Async task to get AppNoticeData data from a URL
    private class JSONGetter extends AsyncTask<Void, Void, Void> {

        private JSONGetterCallback mJSONGetterCallback;

        public JSONGetter(JSONGetterCallback mJSONGetterCallback) {
            this.mJSONGetterCallback = mJSONGetterCallback;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(!activity.isFinishing())
            {
                // Make sure we don't have two dialogs created
                if((progressDialog != null) && progressDialog.isShowing() ) {
                    Log.d(TAG, "Wait dialog already showing.");
                } else {
                    // Showing progress dialog
                    progressDialog = new ProgressDialog(activity);
                    progressDialog.setMessage(activity.getResources().getString(R.string.evidon_pleaseWait));
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }

            }
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            // Creating service handler class instance
            ServiceHandler serviceHandler = new ServiceHandler();

            // Make a request to url for the AppNoticeData info
            String url = getFormattedJSONUrl();
            String jsonStr = "";

            if (usingToken) {
                jsonStr = serviceHandler.getRequest(url, currentAppNoticeToken);
            } else {
                jsonStr = serviceHandler.getRequest(url);
            }
            fillTrackerList(jsonStr);

            // If this JSON string was parsed successfully, cache it for later use
            if (isTrackerListInitialized) {
                AppData.setString(AppData.APPDATA_PREV_JSON, jsonStr);
            } else {
                Log.d(TAG, "Failed to fill tracker list.");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            // Restore the selection states of the trackers
            restoreTrackerStates();

            // Save the tracker states
            saveTrackerStates();

            // Dismiss the progress dialog
            if((progressDialog != null) && progressDialog.isShowing() ){
                try {
                    progressDialog.dismiss();
                } catch (Exception e) {}
            }

            // End the wait-object
            gettingTrackerList = false;
            synchronized(waitObj) {
                Log.d(TAG, "Finished filling tracker list.");
                waitObj.notifyAll();
            }

            // Let the specified callback know it finished...
            if (mJSONGetterCallback != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mJSONGetterCallback.onTaskDone();
                    }
                });
            }
        }

        protected String getFormattedJSONUrl() {
            String formattedJSONUrl = "";
            if (usingToken) {
                formattedJSONUrl = URL_JSON_REQUEST_VIA_TOKEN;
            } else {
                Object[] urlParams = new Object[2];
                urlParams[0] = String.valueOf(companyId);			// 0
                urlParams[1] = String.valueOf(currentNoticeId);		// 1
                formattedJSONUrl = MessageFormat.format(URL_JSON_REQUEST, urlParams);
            }
            return formattedJSONUrl;
        }

    }

    public static void resetSessionData() {
        impliedFlow_SessionCount = 0;
    }
}
