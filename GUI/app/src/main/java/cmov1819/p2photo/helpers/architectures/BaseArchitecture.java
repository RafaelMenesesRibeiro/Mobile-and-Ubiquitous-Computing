package cmov1819.p2photo.helpers.architectures;

import android.app.Activity;
import android.view.View;

import cmov1819.p2photo.LoginActivity;

public abstract class BaseArchitecture {

    public abstract void handlePendingMemberships(Activity activity);

    public abstract void setup(View view, LoginActivity loginActivity);
}
