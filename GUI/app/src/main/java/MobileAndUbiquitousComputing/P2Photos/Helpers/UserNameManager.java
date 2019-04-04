package MobileAndUbiquitousComputing.P2Photos.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import MobileAndUbiquitousComputing.P2Photos.R;

public class UserNameManager {

    private static String userName = null;

    public static String getUserName(Activity activity) {
        if (userName != null) {
            return userName;
        }
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        return sharedPref.getString(activity.getString(R.string.user_name_key), null);
    }

    static void updateUserName(Activity activity, String newUserName) {
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(activity.getString(R.string.user_name_key), newUserName);
        editor.apply(); //TODO: Change to editor.commit() if this will be called async
        userName = newUserName;
    }

    static void deleteUserName(Activity activity) {
        updateUserName(activity,null);
    }
}
