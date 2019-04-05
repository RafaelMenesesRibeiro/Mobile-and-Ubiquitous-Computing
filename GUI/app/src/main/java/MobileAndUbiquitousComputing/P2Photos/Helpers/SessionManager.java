package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import MobileAndUbiquitousComputing.P2Photos.R;

public class SessionManager {

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
        updateSessionID(activity,null);
    }

    public static void deleteUserName(Activity activity) {
        updateUserName(activity,null);
    }
}
