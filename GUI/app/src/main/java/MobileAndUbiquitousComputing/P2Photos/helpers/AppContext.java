package MobileAndUbiquitousComputing.P2Photos.helpers;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

public class AppContext extends Application {
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
