package cmov1819.p2photo.helpers.managers;

import android.util.Log;

public class LogManager {
    private static final String ADD_PHOTO_TAG = "Add Photo";
    private static final String LIST_USERS_TAG = "List Users";
    private static final String NEW_CATALOG_TAG = "New Catalog";
    private static final String NEW_CATALOG_MEMBER_TAG = "New Catalog Member";
    private static final String SEARCH_USER_TAG = "Search User";
    private static final String VIEW_CATALOG_TAG = "View Catalog";
    private static final String VIEW_USER_CATALOGS = "View User Catalogs";

    public static void LogAddPhoto() {
        Log.i(ADD_PHOTO_TAG, "Add Photo Operation completed.");
    }

    public static void LogListUsers() {
        Log.i(LIST_USERS_TAG, "List Users Operation completed.");
    }

    public static void LogNewCatalog(String catalogID, String catalogTitle) {
        Log.i(NEW_CATALOG_TAG, "New Catalog Operation completed. Created catalog <id, title>: " + catalogID + ", " + catalogTitle);
    }

    public static void LogNewCatalogMember(String catalogID, String catalogTitle, String username) {
        Log.i(NEW_CATALOG_MEMBER_TAG, "New Catalog Member Operation completed. Added user: "
                + username + " to catalog <id, title>: " + catalogID + ", " + catalogTitle);
    }

    public static void LogSearchUser(String searchedPattern) {
        Log.i(SEARCH_USER_TAG, "Search User Operation completed. Searched for pattern: " + searchedPattern);
    }

    public static void LogViewCatalog(String catalogID, String catalogTitle) {
        Log.i(VIEW_CATALOG_TAG, "View Catalog Operation completed. Viewed catalog <id, title>:" + catalogID + ", " + catalogTitle);
    }

    public static void LogViewUserCatalogs(String username) {
        Log.i(VIEW_USER_CATALOGS, "View User Catalogs Operation completed. Viewed catalogs for user: " + username);
    }
}
