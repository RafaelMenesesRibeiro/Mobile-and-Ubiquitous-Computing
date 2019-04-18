package cmov1819.p2photo.helpers;

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
    private static final String AUTH_STATE_SHARED_PREF = "p2photo.AuthStatePreference";
    private static final String AUTH_STATE_KEY = "authState";

    private static final String SESSION_ID_SHARED_PREF = "p2photo.SessionIDPreference";
    private static final String SESSION_ID_KEY = "sessionID";
    private static final String USER_NAME_KEY = "username";

    public static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    public static final String USER_INFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";
    public static final String TOKEN_ENDPOINT = "https://www.googleapis.com/oauth2/v4/token";

    private static String sessionID;
    private static String username = null;

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
        String jsonString = activity.getSharedPreferences(AUTH_STATE_SHARED_PREF, Context.MODE_PRIVATE)
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
