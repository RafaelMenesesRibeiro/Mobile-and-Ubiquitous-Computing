package MobileAndUbiquitousComputing.P2Photos.helpers;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

@SuppressLint("Registered")
public class AppContext extends Application {
    public static final String USED_INTENT = "USED_INTENT";
    public static final String APP_LOG_TAG = "P2PHOTO MESSAGE";

    @SuppressLint("StaticFieldLeak")
    private static volatile Context context;

    public void onCreate() {
        super.onCreate();
        AppContext.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return AppContext.context;
    }
}
