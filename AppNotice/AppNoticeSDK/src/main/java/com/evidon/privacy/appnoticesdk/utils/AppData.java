package com.evidon.privacy.appnoticesdk.utils;

/**
 * Created by Steven.Overson on 3/24/2015.
 */

import android.content.Context;
import android.content.SharedPreferences;

import com.evidon.privacy.appnoticesdk.model.AppNoticeData;

import java.util.Map;

/**
 * Persistent storage for app data
 *
 */
public class AppData {
    protected static final int APPDATA_VERSION_VALUE = 101;
    public static final String APPDATA_FILENAME_END = "";
    private static SharedPreferences sharedPreferences = null;

    // Application data keys
    public static final String APPDATA_VERSION = "AppDataVersion";
    public static final String APPDATA_IMPLIED_LAST_DISPLAY_TIME = "implied_last_display_time";   // long int, timestamp in millis
    public static final String APPDATA_IMPLIED_DISPLAY_COUNT = "implied_display_count";           // int
    public static final String APPDATA_EXPLICIT_ACCEPTED = "explicit_accepted";                     // Boolean
    public static final String APPDATA_TRACKERSTATES = "tracker_states";                            // String
    public static final String APPDATA_PREV_APP_NOTICE_TOKEN = "previous_app_notice_token";         // String
    public static final String APPDATA_PREV_NOTICE_ID = "previous_notice_id";                       // int
    public static final String APPDATA_PREV_JSON = "previous_json";                                 // String

    public static SharedPreferences getSessionInfo(){
        if( sharedPreferences == null ) {
            Context appContext = AppNoticeData.appContext;
            String packageName = appContext.getPackageName();
            sharedPreferences = appContext.getSharedPreferences(packageName + APPDATA_FILENAME_END, appContext.MODE_PRIVATE);
            if (!sharedPreferences.contains(APPDATA_VERSION)) {
                setInteger(APPDATA_VERSION, APPDATA_VERSION_VALUE);		// Write a version number into the prefs file
                //upgradeFromOldFiles(fragmentActivity);
            }
        }
        return sharedPreferences;
    }

    protected static void upgradeFromOldFiles(Context context) {
        Boolean prefsCopied = false;
        // Get the old prefs file
//        SharedPreferences oldSharedPreferences = fragmentActivity.getSharedPreferences("(old designator: com.appname...)", appContext.MODE_PRIVATE);
//        if (some criteria) {
//            copySharedPreferences(oldSharedPreferences);
//            prefsCopied = true;
//
//            // Clear this prefs file
//            SharedPreferences.Editor editor = oldSharedPreferences.edit();
//            editor.clear();
//            editor.commit();
//        }
//        ...

    }

    protected static void copySharedPreferences(SharedPreferences oldSharedPreferences) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Map<String,?> keys = oldSharedPreferences.getAll();

        for(Map.Entry<String,?> entry : keys.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if(value instanceof Boolean)
                editor.putBoolean(key, ((Boolean)value).booleanValue());
            else if(value instanceof Float)
                editor.putFloat(key, ((Float)value).floatValue());
            else if(value instanceof Integer)
                editor.putInt(key, ((Integer)value).intValue());
            else if(value instanceof Long)
                editor.putLong(key, ((Long)value).longValue());
            else if(value instanceof String)
                editor.putString(key, ((String)value));
        }

        editor.commit();
    }

    public static void setBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = getSessionInfo().edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static boolean getBoolean(String key, Boolean defValue){
        Boolean _defValue = defValue.equals(null) ? true : defValue;	// Convert a null value to true
        Boolean val = getSessionInfo().getBoolean(key, _defValue);
        return val;
    }

    public static void setInteger(String key, Integer value) {
        SharedPreferences.Editor editor = getSessionInfo().edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static int getInteger(String key, Integer defValue){
        return getSessionInfo().getInt( key, defValue != null ? defValue : 0);
    }

    public static void setLong(String key, Long value) {
        SharedPreferences.Editor editor = getSessionInfo().edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public static long getLong(String key, Long defValue){
        return getSessionInfo().getLong( key, defValue != null ? defValue : 0);
    }

    public static void setString(String key, String value) {
        SharedPreferences s = getSessionInfo();
        SharedPreferences.Editor editor = s.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String getString(String key, String defValue){
        SharedPreferences s = getSessionInfo();
        return s.getString(key, defValue);
    }

    public static String getString(String key){
        return getString(key, "");
    }

    public static void remVal(String key){
        SharedPreferences s = getSessionInfo();
        SharedPreferences.Editor editor = s.edit();
        editor.remove(key);
        editor.commit();
    }

    public static void remove(String key) {
        SharedPreferences.Editor editor = getSessionInfo().edit();
        editor.remove(key);
        editor.commit();
    }

}
