package cmov1819.p2photo.helpers;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

@SuppressLint("Registered")
public class AppContext extends Application {
    public static final String APP_LOG_TAG = "P2PHOTO";

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
