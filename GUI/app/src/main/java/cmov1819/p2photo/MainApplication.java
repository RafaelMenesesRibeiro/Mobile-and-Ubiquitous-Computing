package cmov1819.p2photo;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

public class MainApplication extends Application {
    public static final String APP_LOG_TAG = "P2PHOTO";

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public void onCreate() {
        super.onCreate();
        MainApplication.context = getApplicationContext();
    }

    public static String getApplicationName() {
        ApplicationInfo appInfo = null;
        PackageManager packageManager = MainApplication.context.getPackageManager();
        try {
            appInfo = packageManager.getApplicationInfo(context.getApplicationInfo().packageName, 0);
        } catch (PackageManager.NameNotFoundException exc) {
            // pass
        }
        return (String) (appInfo != null ? packageManager.getApplicationLabel(appInfo) : "cmov1819.p2photo");
    }
}
