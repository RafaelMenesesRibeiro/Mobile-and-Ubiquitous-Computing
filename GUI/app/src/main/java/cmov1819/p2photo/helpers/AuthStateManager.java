package cmov1819.p2photo.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

public class AuthStateManager {
    private static AuthStateManager instance;

    private final String AUTH_MGR_TAG = "AUTH MANAGER";

    private final String AUTH_STATE_SHARED_PREF = "p2photo.AuthStatePreference";
    private final String AUTH_STATE_KEY = "authState";

    private final String AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private final String TOKEN_ENDPOINT = "https://www.googleapis.com/oauth2/v4/token";

    private final String CLIENT_ID = "327056365677-stsv6tntebv1f2jj8agkcr84vrbs3llk.apps.googleusercontent.com";
    private final Uri REDIRECT_URI = Uri.parse("cmov1819.p2photo:/oauth2callback");
    private final int authorizationRequestCode;

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
                Uri.parse(AUTHORIZATION_ENDPOINT),
                Uri.parse(TOKEN_ENDPOINT),
                null
        );
        this.authorizationRequest = newAuthorizationRequest();
        this.authorizationRequestCode = this.authorizationRequest.hashCode();
        this.authState = restoreAuthState();
    }

    public static AuthStateManager getInstance(final Context context) {
        if (instance == null) { instance = new AuthStateManager(context); }
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
    public void handleAuthorizationResponse(final Context context,
                                            @NonNull AuthorizationResponse response,
                                            AuthorizationException error) {
        this.authState = new AuthState(response, error);
        // Exchange authorization code for the refresh and access tokens, and update the AuthState instance
        if (response != null) {
            Log.i(AUTH_MGR_TAG, "Handled authorization response: " + authState.jsonSerialize().toString());
            tryExchangeAuthCodeForAuthTokens(context, response);
        } else {
            Log.i(AUTH_MGR_TAG, "Authorization failed with error: " + error.getMessage());
            String msg = "You must authorize this app to manage some google drive files to use";
            Toast.makeText(context, msg , LENGTH_LONG).show();
        }
    }

    private void tryExchangeAuthCodeForAuthTokens(final Context context, AuthorizationResponse response) {
        Log.i(AUTH_MGR_TAG, "Initiating exchange protocol...");
        this.authorizationService.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
            @Override
            public void onTokenRequestCompleted(@Nullable TokenResponse token, @Nullable AuthorizationException exc) {
                if (exc != null) {
                    Log.w(AUTH_MGR_TAG, "Token exchange authorization failed", exc);
                    String msg = "Could not exchange auth code for an auth token, some features might be unavailable";
                    Toast.makeText(context, msg, LENGTH_LONG).show();
                } else {
                    if (token != null) {
                        Log.i(AUTH_MGR_TAG, "Token Response [ Access Token: " + token.accessToken + ", ID Token: " + token.idToken + " ]");
                        authState.update(token, exc);
                        persistAuthState();
                    } else {
                        Log.w(AUTH_MGR_TAG, "Received token is null");
                        String msg = "Could not exchange auth code for an auth token, some features might be unavailable";
                        Toast.makeText(context, msg, LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    /**********************************************************
     *  AUTHSTATE PERSISTENCE
     **********************************************************/

    @Nullable
    public synchronized AuthState restoreAuthState() {
        String jsonString = sharedPreferences.getString(AUTH_STATE_KEY, null);
        if (jsonString != null) {
            try {
                return AuthState.jsonDeserialize(jsonString);
            } catch (JSONException jsonException) {
                Log.i(AUTH_MGR_TAG, "Except. when serializing AuthState from disk. This should not happen.");
                return null;
            }
        }
        return null;
    }

    public synchronized void persistAuthState() {
        sharedPreferences.edit().putString(AUTH_STATE_KEY, authState.jsonSerialize().toString()).apply();
    }

    public synchronized void clearAuthState() {
        sharedPreferences.edit().remove(AUTH_STATE_KEY).apply();
    }

    public synchronized void refreshAuthState() {
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
     *  AUTHSTATE VALIDATORS, GETTERS AND SETTERS
     **********************************************************/

    public AuthorizationService getAuthorizationService() {
        return authorizationService;
    }

    public AuthorizationServiceConfiguration getAuthorizationServiceConfiguration() {
        return authorizationServiceConfiguration;
    }

    public int getAuthorizationRequestCode() {
        return authorizationRequestCode;
    }

    public AuthorizationRequest getAuthorizationRequest() {
        return authorizationRequest;
    }

    public AuthState getAuthState() {
        return authState;
    }

    public boolean hasValidAuthState() {
        if (authState == null) {
            return false;
        }
        return authState.isAuthorized();
    }

    /**********************************************************
     *  HELPERS
     **********************************************************/

    /* Creates a AuthorizationRequest instance containing Google Drive API Scopes required by P2Photo application */
    private AuthorizationRequest newAuthorizationRequest() {
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                this.authorizationServiceConfiguration,
                this.CLIENT_ID,
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
}