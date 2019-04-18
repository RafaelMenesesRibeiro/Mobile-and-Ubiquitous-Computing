package cmov1819.p2photo.helpers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;

public class AuthStateManager {
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://www.googleapis.com/oauth2/v4/token";

    private static final String AUTH_TAG = "AUTH";
    private static final String AUTH_STATE_SHARED_PREF = "p2photo.AuthStatePreference";
    private static final String AUTH_STATE_KEY = "authState";

    private static AuthStateManager instance;

    private final String APP_ID = "327056365677-stsv6tntebv1f2jj8agkcr84vrbs3llk.apps.googleusercontent.com";
    private final Uri REDIRECT_URI = Uri.parse("https://127.0.0.1");
    private final int requestCode;

    private SharedPreferences sharedPreferences;
    private AuthorizationServiceConfiguration authorizationServiceConfiguration;
    private AuthorizationService authorizationService;
    private AuthorizationRequest authorizationRequest;
    private AuthState authState;

    /**********************************************************
    * SINGLETON METHODS
    *********************************************************/

    private AuthStateManager(Context context){
        this.sharedPreferences = context.getSharedPreferences(AUTH_STATE_SHARED_PREF, Context.MODE_PRIVATE);
        this.authorizationService = new AuthorizationService(context);
        this.authorizationServiceConfiguration = new AuthorizationServiceConfiguration(
                // authorizationServiceConfiguration must be instanciated before authorizationRequest
                Uri.parse(AUTH_ENDPOINT),
                Uri.parse(TOKEN_ENDPOINT),
                null
        );
        this.authorizationRequest = newAuthorizationRequest();
        this.requestCode = this.authorizationRequest.hashCode();
    }

    public static AuthStateManager getInstance(Context context) {
        if (instance == null) { instance = new AuthStateManager(context); }
        return instance;
    }

    public static AuthStateManager getInstance() {
        return instance;
    }

    private AuthorizationRequest newAuthorizationRequest() {
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                this.authorizationServiceConfiguration,
                this.APP_ID,
                ResponseTypeValues.CODE,
                this.REDIRECT_URI
        );
        builder.setScopes(new ArrayList<>(Arrays.asList(
                "email", "profile", "openid",
                "https://www.googleapis.com/auth/drive.photos.readonly",
                "https://www.googleapis.com/auth/drive.metadata.readonly",
                "https://www.googleapis.com/auth/drive.readonly",
                "https://www.googleapis.com/auth/drive.file",
                "https://www.googleapis.com/auth/drive.appdata"
        )));
        return builder.build();
    }

    /**********************************************************
     *  AUTHSTATE CONSTRUCTORS
     **********************************************************/

    /*
     * This helper method creates a new authState by persisting it to SharedPreferences instead of actually returning
     * an object; Therefore, accesses to the authState are than passed by deserializing the object from disk.
     */
    public void newAuthState(Context context, String action) {
        Intent postAuthIntent = new Intent(action);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, this.requestCode, postAuthIntent, 0);
        this.authorizationService.performAuthorizationRequest(this.authorizationRequest, pendingIntent);
    }

    /**********************************************************
    *  AUTHSTATE PERSISTENCE
    **********************************************************/

    public void persistAuthState(@NonNull AuthState authState) {
        this.sharedPreferences.edit().putString(AUTH_STATE_KEY, authState.jsonSerialize().toString()).apply();
    }

    public void clearAuthState() {
        this.sharedPreferences.edit().remove(AUTH_STATE_KEY).apply();
    }

    @Nullable
    public AuthState restoreAuthState() {
        String jsonString = this.sharedPreferences.getString(AUTH_STATE_KEY, null);
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


    public void refreshAuthState(@NonNull AuthState authState) {
        /*
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
        */
    }


}