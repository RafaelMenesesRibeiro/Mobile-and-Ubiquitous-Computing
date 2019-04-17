package MobileAndUbiquitousComputing.P2Photos.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import net.openid.appauth.AuthState;

import org.json.JSONException;

public class SessionManager {
    private static final String SESSION_LOG_TAG = "P2Photo#SessionManager";
    private static final String AUTH_STATE_SHARED_PREF = "P2Photos.AuthStatePreference";
    private static final String AUTH_STATE_KEY = "authState";

    private static final String SESSION_ID_SHARED_PREF = "P2Photos.SessionIDPreference";
    private static final String SESSION_ID_KEY = "sessionID";
    private static final String USER_NAME_KEY = "userName";

    public static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    public static final String USER_INFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";
    public static final String TOKEN_ENDPOINT = "https://www.googleapis.com/oauth2/v4/token";
    private static String sessionID;
    private static String userName = null;

    public static String getSessionID(Activity activity) {
        if (sessionID != null) { return sessionID; }
        SharedPreferences sharedPref = activity.getSharedPreferences(SESSION_ID_SHARED_PREF, Context.MODE_PRIVATE);
        return sharedPref.getString(SESSION_ID_KEY, null);
    }

    public static String getUsername(Activity activity) {
        if (userName != null) { return userName; }
        SharedPreferences sharedPref = activity.getSharedPreferences(SESSION_ID_SHARED_PREF, Context.MODE_PRIVATE);
        return sharedPref.getString(USER_NAME_KEY, null);
    }

    public static void updateSessionID(Activity activity, String newSessionID) {
        SharedPreferences sharedPref = activity.getSharedPreferences(SESSION_ID_SHARED_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(SESSION_ID_KEY, newSessionID);
        editor.apply();
        sessionID = newSessionID;
    }

    public static void updateUserName(Activity activity, String newUserName) {
        SharedPreferences sharedPref = activity.getSharedPreferences(SESSION_ID_SHARED_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(USER_NAME_KEY, newUserName);
        editor.apply();
        userName = newUserName;
    }

    public static void deleteSessionID(Activity activity) {
        updateSessionID(activity, null);
    }

    public static void deleteUserName(Activity activity) {
        updateUserName(activity, null);
    }

    public static  void persistAuthState(@NonNull AuthState authState, @NonNull Activity activity) {
        activity.getSharedPreferences(AUTH_STATE_SHARED_PREF, Context.MODE_PRIVATE).edit()
                .putString(AUTH_STATE_KEY, authState.jsonSerialize().toString())
                .apply();
    }

    public static  void clearAuthState(@NonNull Activity activity) {
        activity.getSharedPreferences(AUTH_STATE_SHARED_PREF, Context.MODE_PRIVATE)
                .edit()
                .remove(AUTH_STATE_KEY)
                .apply();
    }

    @Nullable
    public static AuthState restoreAuthState(@NonNull Activity activity) {
        String jsonString =
                activity.getSharedPreferences(AUTH_STATE_SHARED_PREF, Context.MODE_PRIVATE)
                        .getString(AUTH_STATE_KEY, null);

        if (!TextUtils.isEmpty(jsonString)) {
            try {
                assert jsonString != null;
                return AuthState.jsonDeserialize(jsonString);
            } catch (JSONException jsonException) {
                Log.i(SESSION_LOG_TAG, "Except. when serializing AuthState from disk. This should not happen.");
            }
        }
        return null;
    }
}
