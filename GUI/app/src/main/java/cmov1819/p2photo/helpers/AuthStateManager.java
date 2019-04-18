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
import android.widget.Toast;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;

import static android.widget.Toast.LENGTH_LONG;
import static cmov1819.p2photo.helpers.AppContext.getAppContext;

public class AuthStateManager {
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://www.googleapis.com/oauth2/v4/token";

    private static final String AUTH_MGR_TAG = "AUTH MANAGER";
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

    private AuthStateManager(Context context) {
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

    /**********************************************************
     *  AUTHSTATE REQUEST AND RESPONSE HANDLING
     **********************************************************/

    /*
    * AppAuth provides the AuthorizationResponse to this activity, via the provided RedirectUriReceiverActivity.
    * From it we can ultimately obtain a TokenResponse which we can use to make calls to the API;
    * The AuthState object that is created from the response can be used to store details from the auth session to
    * reuse it between application runs and it may be changed overtime as new OAuth results are received.
    */
    public void handleAuthorizationResponse(@NonNull AuthorizationResponse response, AuthorizationException error) {
        this.authState = new AuthState(response, error);
        // Exchange authorization code for the refresh and access tokens, and update the AuthState instance
        if (response != null) {
            Log.i(AUTH_MGR_TAG, "Handled authorization response: " + authState.jsonSerialize().toString());
            tryExchangeAuthCodeForAuthTokens(response);
        } else {
            Log.i(AUTH_MGR_TAG, "Authorization failed with error: " + error.getMessage());
            String msg = "You must authorize this app to manage some google drive files to use";
            Toast.makeText(getAppContext(), msg , LENGTH_LONG).show();
        }
    }

    private void tryExchangeAuthCodeForAuthTokens(AuthorizationResponse response) {
        Log.i(AUTH_MGR_TAG, "Initiating exchange protocol...");
        this.authorizationService.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
            @Override
            public void onTokenRequestCompleted(@Nullable TokenResponse token, @Nullable AuthorizationException exc) {
                if (exc != null) {
                    Log.w(AUTH_MGR_TAG, "Token exchange authorization failed", exc);
                    String msg = "Could not exchange auth code for an auth token, some features might be unavailable";
                    Toast.makeText(getAppContext(), msg, LENGTH_LONG).show();
                } else {
                    if (token != null) {
                        Log.i(AUTH_MGR_TAG, "Token Response [ Access Token: " + token.accessToken + ", ID Token: " + token.idToken + " ]");
                        authState.update(token, exc);
                        persistAuthState();
                    } else {
                        Log.w(AUTH_MGR_TAG, "Received token is null");
                        String msg = "Could not exchange auth code for an auth token, some features might be unavailable";
                        Toast.makeText(getAppContext(), msg, LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    /**********************************************************
     *  AUTHSTATE PERSISTENCE
     **********************************************************/

    @Nullable
    public AuthState restoreAuthState() {
        String jsonString = this.sharedPreferences.getString(AUTH_STATE_KEY, null);
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                return AuthState.jsonDeserialize(jsonString);
            } catch (JSONException jsonException) {
                Log.i(AUTH_MGR_TAG, "Except. when serializing AuthState from disk. This should not happen.");
                return null;
            }
        }
        return null;
    }

    public void persistAuthState() {
        this.sharedPreferences.edit().putString(AUTH_STATE_KEY, this.authState.jsonSerialize().toString()).apply();
    }

    public void clearAuthState() {
        this.sharedPreferences.edit().remove(AUTH_STATE_KEY).apply();
    }

    public void refreshAuthState() {
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

    /**********************************************************
     *  AUTHSTATE VALIDATORS AND GETTERS
     **********************************************************/

    public AuthState getAuthState() {
        return authState;
    }

    public boolean hasValidAuthState() {
        return this.authState == null && !this.authState.isAuthorized();
    }

    /**********************************************************
     *  HELPERS
     **********************************************************/

    /* Creates a AuthorizationRequest instance containing Google Drive API Scopes required by P2Photo application */
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

    /*
     * Context of calling activity and the action the pending intent will trigger once completed, see AndroidManifest
     * intent-filters for more a better understanding of action string.
     */
    public void newAuthState(Context context, String action) {
        Intent postAuthIntent = new Intent(action);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, this.requestCode, postAuthIntent, 0);
        this.authorizationService.performAuthorizationRequest(this.authorizationRequest, pendingIntent);
    }
}