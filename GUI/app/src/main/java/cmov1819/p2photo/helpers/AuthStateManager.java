package cmov1819.p2photo.helpers;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;

public class AuthStateManager {
    private static final String AUTH_TAG = "AUTH";
    private static final String AUTH_STATE_SHARED_PREF = "p2photo.AuthStatePreference";
    private static final String AUTH_STATE_KEY = "authState";

    public static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    public static final String USER_INFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";
    public static final String TOKEN_ENDPOINT = "https://www.googleapis.com/oauth2/v4/token";

    /**********************************************************
     * AUTHSTATE PERSISTENCE
     ***********************************************************/

    @Nullable
    public static AuthState restoreAuthState(@NonNull Activity activity) {
        String jsonString = activity.getSharedPreferences(AUTH_STATE_SHARED_PREF, Context.MODE_PRIVATE)
                .getString(AUTH_STATE_KEY, null);
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                return AuthState.jsonDeserialize(jsonString);
            } catch (JSONException jsonException) {
                Log.i(AUTH_TAG, "Except. when serializing AuthState from disk. This should not happen.");
                return null;
            }
        }
        return null;
    }

    public static void persistAuthState(@NonNull AuthState authState, @NonNull Activity activity) {
        activity.getSharedPreferences(AUTH_STATE_SHARED_PREF, Context.MODE_PRIVATE).edit()
                .putString(AUTH_STATE_KEY, authState.jsonSerialize().toString())
                .apply();
    }

    public static void refreshAuthState(@NonNull AuthState authState, @NonNull Activity activity) {
        ClientSecretPost clientSecretPost = new ClientSecretPost(authManager.getAuth().getClientSecret());
        final TokenRequest request = authState.createTokenRefreshRequest();
        final AuthorizationService authService = authManager.getAuthService();

        authService.performTokenRequest(request, clientSecretPost, new AuthorizationService.TokenResponseCallback() {
            @Override
            public void onTokenRequestCompleted(@Nullable TokenResponse response, @Nullable AuthorizationException ex) {
                if (ex != null){
                    ex.printStackTrace();
                    return;
                }
                authManager.updateAuthState(response,ex);
                MyApp.Token = authState.getIdToken();
            }
        });
    }

    public static  void clearAuthState(@NonNull Activity activity) {
        activity.getSharedPreferences(AUTH_STATE_SHARED_PREF, Context.MODE_PRIVATE)
                .edit()
                .remove(AUTH_STATE_KEY)
                .apply();
    }
}