package cmov1819.p2photo.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.RegistrationResponse;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class AuthStateManager {
    private static final AtomicReference<WeakReference<AuthStateManager>> INSTANCE_REF =
            new AtomicReference<>(new WeakReference<AuthStateManager>(null));

    private static final String AUTH_MGR_TAG = "AUTH";
    private static final String AUTH_STATE_SHARED_PREF = "p2photo.AuthStatePreference";
    private static final String AUTH_STATE_KEY = "authState";

    private final SharedPreferences sharedPreferences;
    private final ReentrantLock sharedPreferencesLock;
    private final AtomicReference<AuthState> currentAuthState;

    @AnyThread
    public static AuthStateManager getInstance(@NonNull Context context) {
        AuthStateManager manager = INSTANCE_REF.get().get();
        if (manager == null) {
            manager = new AuthStateManager(context.getApplicationContext());
            INSTANCE_REF.set(new WeakReference<>(manager));
        }
        return manager;
    }

    private AuthStateManager(Context context) {
        sharedPreferences = context.getSharedPreferences(AUTH_STATE_SHARED_PREF, Context.MODE_PRIVATE);
        sharedPreferencesLock = new ReentrantLock();
        currentAuthState = new AtomicReference<>();
    }

    @AnyThread
    @NonNull
    public AuthState getCurrent() {
        if (currentAuthState.get() != null) {
            return currentAuthState.get();
        }
        AuthState state = readState();
        if (currentAuthState.compareAndSet(null, state)) {
            return state;
        } else {
            return currentAuthState.get();
        }
    }

    @AnyThread
    @NonNull
    public AuthState replace(@NonNull AuthState state) {
        writeState(state);
        currentAuthState.set(state);
        return state;
    }

    @AnyThread
    @NonNull
    public AuthState updateAfterAuthorization(@Nullable AuthorizationResponse response, @Nullable AuthorizationException ex) {
        AuthState current = getCurrent();
        current.update(response, ex);
        return replace(current);
    }

    @AnyThread
    @NonNull
    public AuthState updateAfterTokenResponse(
            @Nullable TokenResponse response,
            @Nullable AuthorizationException ex) {
        AuthState current = getCurrent();
        current.update(response, ex);
        return replace(current);
    }

    @AnyThread
    @NonNull
    public AuthState updateAfterRegistration(
            RegistrationResponse response,
            AuthorizationException ex) {
        AuthState current = getCurrent();
        if (ex != null) {
            return current;
        }

        current.update(response);
        return replace(current);
    }

    @AnyThread
    @NonNull
    private AuthState readState() {
        sharedPreferencesLock.lock();
        try {
            String jsonString = sharedPreferences.getString(AUTH_STATE_KEY, null);
            if (jsonString == null) {
                return new AuthState();
            }
            try {
                return AuthState.jsonDeserialize(jsonString);
            } catch (JSONException ex) {
                Log.w(AUTH_MGR_TAG, "Failed to deserialize stored auth state - discarding");
                return new AuthState();
            }
        } finally {
            sharedPreferencesLock.unlock();
        }
    }

    @AnyThread
    private void writeState(@Nullable final AuthState authState) {
        sharedPreferencesLock.lock();
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (authState == null) {
                editor.remove(AUTH_STATE_KEY);
            } else {
                editor.putString(AUTH_STATE_KEY, authState.jsonSerializeString());
            }
            if (!editor.commit()) {
                throw new IllegalStateException("Failed to write state to shared prefs");
            }
        } finally {
            sharedPreferencesLock.unlock();
        }
    }
}