package cmov1819.p2photo.helpers.architectures;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import java.io.File;

import cmov1819.p2photo.LoginActivity;

public abstract class BaseArchitecture {

    public abstract void handlePendingMemberships(Activity activity);

    public abstract void setup(View view, LoginActivity loginActivity);

    public abstract void addPhoto(FragmentActivity activity, String catalogId, File androidFilePath);

    public abstract void newCatalogSlice(Activity activity, String catalogID, String catalogTitle);
}
