package cmov1819.p2photo.helpers.managers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.Set;

import static cmov1819.p2photo.LimitStorageFragment.DEFAULT_CACHE_VALUE;

public class SessionManager {
    private static final String SESSION_ID_SHARED_PREF = "p2photo.SessionIDPreference";
    private static final String SESSION_ID_KEY = "sessionID";
    private static final String USER_NAME_KEY = "username";
    private static final String CACHE_SIZE_TAG = "cacheSize";

    private static String sessionID;
    private static String username = null;

    /**********************************************************
     * PREFERENCES METHODS
     ***********************************************************/

    public static void setMaxCacheImageSize(Activity activity, int size) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(SESSION_ID_SHARED_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(CACHE_SIZE_TAG, size);
    }

    public static int getMaxCacheImageSize(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(SESSION_ID_SHARED_PREF, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(CACHE_SIZE_TAG, DEFAULT_CACHE_VALUE);
    }

    /**********************************************************
     * COOKIE METHODS
     ***********************************************************/

    public static String getSessionID(@NonNull Activity activity) {
        if (sessionID != null) { return sessionID; }
        SharedPreferences sharedPref = activity.getSharedPreferences(SESSION_ID_SHARED_PREF, Context.MODE_PRIVATE);
        return sharedPref.getString(SESSION_ID_KEY, null);
    }

    public static String getUsername(@NonNull Activity activity) {
        if (username != null) { return username; }
        SharedPreferences sharedPref = activity.getSharedPreferences(SESSION_ID_SHARED_PREF, Context.MODE_PRIVATE);
        return sharedPref.getString(USER_NAME_KEY, null);
    }

    public static void updateSessionID(@NonNull Activity activity, @NonNull String newSessionID) {
        SharedPreferences sharedPref = activity.getSharedPreferences(SESSION_ID_SHARED_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SESSION_ID_KEY, newSessionID);
        editor.apply();
        sessionID = newSessionID;
    }

    public static void updateUsername(@NonNull Activity activity, @NonNull String newUserName) {
        SharedPreferences sharedPref = activity.getSharedPreferences(SESSION_ID_SHARED_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(USER_NAME_KEY, newUserName);
        editor.apply();
        username = newUserName;
    }

    public static void deleteSessionID(@NonNull  Activity activity) {
        updateSessionID(activity, null);
    }

    public static void deleteUsername(@NonNull  Activity activity) {
        updateUsername(activity, null);
    }
}
