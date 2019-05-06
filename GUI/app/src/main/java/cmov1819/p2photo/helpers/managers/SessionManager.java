package cmov1819.p2photo.helpers.managers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.Set;

public class SessionManager {
    private static final String SESSION_ID_SHARED_PREF = "p2photo.SessionIDPreference";
    private static final String SESSION_ID_KEY = "sessionID";
    private static final String USER_NAME_KEY = "username";

    private static String sessionID;
    private static String username = null;

    // TODO - Remove these. //
    private static Set<String> catalogMembershipIDs;
    private static Set<String> catalogMembershipNames;

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

    /**********************************************************
     * MEMBERSHIP METHODS
     ***********************************************************/

    // TODO - Remove these. //

    public static Set<String> getCatalogMembershipsNames(Activity activity) {
        if (catalogMembershipNames != null) { return catalogMembershipNames; }
        SharedPreferences sharedPref = activity.getSharedPreferences(SESSION_ID_SHARED_PREF, Context.MODE_PRIVATE);
        return sharedPref.getStringSet("membershipNames", null);
    }

    public static void updateCatalogMembershipsNames(Activity activity, Set<String> catalogMemberships) {
        SharedPreferences sharedPref = activity.getSharedPreferences(SESSION_ID_SHARED_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet("membershipNames", catalogMemberships);
        editor.apply();
        SessionManager.catalogMembershipNames = catalogMemberships;
    }

    public static Set<String> getCatalogMembershipsIDs(Activity activity) {
        if (catalogMembershipIDs != null) { return catalogMembershipIDs; }
        SharedPreferences sharedPref = activity.getSharedPreferences(SESSION_ID_SHARED_PREF, Context.MODE_PRIVATE);
        Set<String> set = sharedPref.getStringSet("membershipIDs", null);
        return set;
    }

    public static void updateCatalogMembershipsIDs(Activity activity, Set<String> catalogMemberships) {
        SharedPreferences sharedPref = activity.getSharedPreferences(SESSION_ID_SHARED_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet("membershipIDs", catalogMemberships);
        editor.apply();
        SessionManager.catalogMembershipIDs = catalogMemberships;
    }

}
