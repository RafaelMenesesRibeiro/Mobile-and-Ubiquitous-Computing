package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import MobileAndUbiquitousComputing.P2Photos.R;

public class SessionID {

    public static String getSessionID(Activity activity) {
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        return sharedPref.getString(activity.getString(R.string.session_id_key), null);
    }

    public static void updateSessionID(Activity activity, String newSessionID) {
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(activity.getString(R.string.session_id_key), newSessionID);
        editor.apply(); //TODO: Change to editor.commit() if this will be called async
    }

    public static void deleteSessionID(Activity activity) {
        updateSessionID(activity,null);
    }
}
