package cmov1819.p2photo.helpers.architectures;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import java.io.File;

import cmov1819.p2photo.LoginActivity;
import cmov1819.p2photo.MainMenuActivity;

public abstract class BaseArchitecture {

    public abstract void handlePendingMemberships(final Activity activity);

    public abstract void onSignUp(final LoginActivity loginActivity);

    public abstract void setup(final View view, final LoginActivity loginActivity);

    public abstract void setupHome(final MainMenuActivity mainMenuActivity);

    public abstract void addPhoto(final FragmentActivity activity, String catalogId, File androidFilePath);

    public abstract void newCatalogSlice(final Activity activity, String catalogID, String catalogTitle);

    public abstract void viewCatalog(final Activity activity, final View view, String catalogID, String catalogTitle);
}
