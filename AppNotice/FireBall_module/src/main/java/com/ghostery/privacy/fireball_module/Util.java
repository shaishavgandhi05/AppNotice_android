package com.ghostery.privacy.fireball_module;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Steven.Overson on 3/27/2015.
 */
public class Util {
    public static final String SP_COMPANY_ID = "sp_companyId";
    public static final String SP_CONFIG_ID = "sp_configId";
    public static final String SP_USE_REMOTEVALUES = "sp_useRemoteValues";
    public static final String SP_IS_HYBRIDAPP = "sp_isHybridApp";

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    public static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    public static void setSharedPreference(Activity activity, String key, String value) {
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = s.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String getSharedPreference(Activity activity, String key, String defValue){
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(activity);
        return s.getString(key, defValue);
    }

    public static void setSharedPreference_boolean(Activity activity, String key, boolean value) {
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = s.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static boolean getSharedPreference_boolean(Activity activity, String key, boolean defValue){
        SharedPreferences s = PreferenceManager.getDefaultSharedPreferences(activity);
        return s.getBoolean(key, defValue);
    }

    public static void clearSharedPreferences(Activity activity, String prefName) {
        SharedPreferences s = activity.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = s.edit();
        editor.clear();
        editor.commit();
    }

}
