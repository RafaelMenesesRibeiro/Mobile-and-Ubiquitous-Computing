package MobileAndUbiquitousComputing.P2Photos.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.openid.appauth.AuthState;

import org.json.JSONException;

import MobileAndUbiquitousComputing.P2Photos.R;

import static MobileAndUbiquitousComputing.P2Photos.helpers.AppContext.AUTH_STATE;

public class SessionManager {
    private static final String AUTH_STATE_SHARED_PREF = "AuthStatePreference";
    private static String sessionID;
    private static String userName = null;

    public static String getSessionID(Activity activity) {
        if (sessionID != null) {
            return sessionID;
        }
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        return sharedPref.getString(activity.getString(R.string.session_id_key), null);
    }

    public static String getUsername(Activity activity) {
        if (userName != null) {
            return userName;
        }
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        return sharedPref.getString(activity.getString(R.string.user_name_key), null);
    }

    public static void updateSessionID(Activity activity, String newSessionID) {
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(activity.getString(R.string.session_id_key), newSessionID);
        editor.apply(); //TODO: Change to editor.commit() if this will be called async
        sessionID = newSessionID;
    }

    public static void updateUserName(Activity activity, String newUserName) {
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(activity.getString(R.string.user_name_key), newUserName);
        editor.apply(); //TODO: Change to editor.commit() if this will be called async
        userName = newUserName;
    }

    public static void deleteSessionID(Activity activity) {
        updateSessionID(activity, null);
    }

    public static void deleteUserName(Activity activity) {
        updateUserName(activity, null);
    }

    @SuppressLint("ApplySharedPref")
    public static  void persistAuthState(@NonNull AuthState authState, @NonNull Activity activity) {
        activity.getSharedPreferences(AUTH_STATE_SHARED_PREF, Context.MODE_PRIVATE).edit()
                .putString(AUTH_STATE, authState.jsonSerialize().toString())
                .apply();
    }

    public static  void clearAuthState(@NonNull Activity activity) {
        activity.getSharedPreferences(AUTH_STATE_SHARED_PREF, Context.MODE_PRIVATE)
                .edit()
                .remove(AUTH_STATE)
                .apply();
    }

    @Nullable
    public static AuthState restoreAuthState(@NonNull Activity activity) {
        String jsonString =  activity.getSharedPreferences(AUTH_STATE_SHARED_PREF, Context.MODE_PRIVATE).getString(AUTH_STATE, null);
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                assert jsonString != null;
                return AuthState.jsonDeserialize(jsonString);
            } catch (JSONException jsonException) {
                // should never happen
            }
        }
        return null;
    }


}
