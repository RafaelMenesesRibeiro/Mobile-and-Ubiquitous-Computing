package cmov1819.p2photo.helpers.managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.common.collect.ImmutableSet;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;

import cmov1819.p2photo.LoginActivity;

import static android.widget.Toast.LENGTH_SHORT;

public class AuthStateManager {
    private static final String AUTH_FAILURE = "Could not obtain valid authorization. Some features will be disabled";
    private static final String REFRESH_FAILURE = "Could not refresh autorization automatically. Please reauthenticate";

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
    private AuthorizationRequest authorizationRequest;
    private AuthState authState;

    /**********************************************************
     * SINGLETON METHODS
     *********************************************************/

    private AuthStateManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(AUTH_STATE_SHARED_PREF, Context.MODE_PRIVATE);
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

    public void handleAuthorizationResponse(final Context context, Intent appAuthIntent) {
        String msg = "Initiating exchange protocol...";
        LogManager.logInfo(AUTH_MGR_TAG, msg);
        AuthorizationResponse response = AuthorizationResponse.fromIntent(appAuthIntent);
        AuthorizationException error = AuthorizationException.fromIntent(appAuthIntent);
        this.authState = new AuthState(response, error);
        if (response != null) {
            msg = "Handled authorization response " + authState.jsonSerializeString();
            LogManager.logInfo(AUTH_MGR_TAG, msg);
            AuthorizationService service = new AuthorizationService(context);
            exchangeAuthorizationForAcessTokens(context, service, response, error);
            service.dispose();
        }
    }

    @Deprecated
    public void refreshTokens(final Context context) {
        TokenRequest request = authState.createTokenRefreshRequest();
        AuthorizationService authorizationService = new AuthorizationService(context);
        authorizationService.performTokenRequest(request, new AuthorizationService.TokenResponseCallback() {
            @Override
            public void onTokenRequestCompleted(@Nullable TokenResponse response, @Nullable AuthorizationException error) {
            if (error != null) {
                String msg = "<AuthorizationException> Unable to refresh authorization token.";
                LogManager.logError(AUTH_MGR_TAG, msg);
                getAuthorization(context, REFRESH_FAILURE,true);
            } else {
                updateAuthState(response, error);
            }
            }
        });
        authorizationService.dispose();
    }

    @Deprecated
    public void getAuthorization(final Context context, String reason, boolean forceFinish) {
        LogManager.toast((Activity) context, reason);
        clearAuthState();
        context.startActivity(new Intent(context, LoginActivity.class));
        if (forceFinish) { ((Activity)context).finish(); }
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
                String msg = "Except. when serializing AuthState from disk. This should not happen.";
                LogManager.logInfo(AUTH_MGR_TAG, msg);
                return null;
            }
        }
        return null;
    }

    public synchronized void persistAuthState() {
        sharedPreferences.edit().putString(AUTH_STATE_KEY, authState.jsonSerialize().toString()).apply();
    }

    public synchronized void clearAuthState() {
        authState = null;
        sharedPreferences.edit().remove(AUTH_STATE_KEY).apply();
    }

    public synchronized void updateAuthState(TokenResponse response, AuthorizationException error) {
        String msg = "Updated and persisted <TokenResponse>: " + response.accessToken + ", "+ response.idToken;
        LogManager.logInfo(AUTH_MGR_TAG, msg);
        authState.update(response, error);
        persistAuthState();
    }

    /**********************************************************
     *  AUTHSTATE VALIDATORS, OPERATORS, GETTERS AND SETTERS
     **********************************************************/

    public synchronized AuthState getAuthState() {
        return authState;
    }

    public AuthorizationServiceConfiguration getAuthorizationServiceConfiguration() {
        return authorizationServiceConfiguration;
    }

    public AuthorizationRequest getAuthorizationRequest() {
        return authorizationRequest;
    }

    public synchronized boolean hasValidAuthState() {
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
        builder.setScopes(ImmutableSet.of(
                    "email", "profile", "openid",
                    "https://www.googleapis.com/auth/drive.file",
                    "https://www.googleapis.com/auth/drive.appdata"
                )
        );
        return builder.build();
    }

    private void exchangeAuthorizationForAcessTokens(final Context context, AuthorizationService service,
                                                     AuthorizationResponse response, AuthorizationException error) {
        service.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
            @Override
            public void onTokenRequestCompleted(@Nullable TokenResponse response, @Nullable AuthorizationException error) {
                if (error != null) {
                    String msg = "Token exchange <AuthorizationResponse> had <AuthorizationException>";
                    LogManager.logError(AUTH_MGR_TAG, msg);
                    LogManager.toast((Activity) context, AUTH_FAILURE);
                }
                else {
                    if (response != null) {
                        updateAuthState(response, error);
                    }
                    else {
                        String msg = "Could not obtain <TokenResponse> from <AuthorizationResponse>";
                        LogManager.logError(AUTH_MGR_TAG, msg);
                        LogManager.toast((Activity) context, AUTH_FAILURE);
                    }
                }
            }
        });
    }

}