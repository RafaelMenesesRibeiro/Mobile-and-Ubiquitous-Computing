package cmov1819.p2photo.helpers.managers;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

public class KeyManager {
    private static KeyManager instance;

    private final SecretKey mSecretKey;
    private final PrivateKey mPrivateKey;
    private final PublicKey mPublicKey;

    private final Map<String, PublicKey> publicKeys;    // username, public key
    private final Map<String, SecretKey> sessionKeys;   // username, session key

    private PublicKey mCurrentServerKey;

    private KeyManager(SecretKey aes, PrivateKey privateKey, PublicKey publicKey) {
        this.publicKeys = new ConcurrentHashMap<>();
        this.sessionKeys = new ConcurrentHashMap<>();
        this.mSecretKey = aes;
        this.mPrivateKey = privateKey;
        this.mPublicKey = publicKey;
    }

    public static KeyManager init(SecretKey aes, PrivateKey privateKey, PublicKey publicKey) {
        if (instance == null) {
            instance = new KeyManager(aes, privateKey, publicKey);
        }
        return instance;
    }

    public static KeyManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("KeyManager was not initiated before using getInstance, call init on MainMenu!");
        }
        return instance;
    }

    /** Getters and Setters */

    public SecretKey getmSecretKey() {
        return mSecretKey;
    }

    public PrivateKey getmPrivateKey() {
        return mPrivateKey;
    }

    public PublicKey getmPublicKey() {
        return mPublicKey;
    }

    public Map<String, PublicKey> getPublicKeys() {
        return publicKeys;
    }

    public Map<String, SecretKey> getSessionKeys() {
        return sessionKeys;
    }

    public PublicKey getmCurrentServerKey() {
        return mCurrentServerKey;
    }

    public void setmCurrentServerKey(PublicKey mCurrentServerKey) {
        this.mCurrentServerKey = mCurrentServerKey;
    }
}
