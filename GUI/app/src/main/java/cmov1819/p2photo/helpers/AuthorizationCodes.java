package cmov1819.p2photo.helpers;

@Deprecated
public class AuthorizationCodes {
    public static final String	CODE_CHALLENGE_METHOD_PLAIN	= "plain";
    public static final String	CODE_CHALLENGE_METHOD_S256 = "S256";
    public static final String	RESPONSE_MODE_FRAGMENT = "fragment";
    public static final String	RESPONSE_MODE_QUERY = "query";
    public static final String	RESPONSE_TYPE_CODE = "code";
    public static final String	RESPONSE_TYPE_TOKEN	= "token";
    public static final String	SCOPE_ADDRESS = "address";
    public static final String	SCOPE_EMAIL = "email";
    public static final String	SCOPE_OPENID = "openid";
    public static final String	SCOPE_PHONE = "phone";
    public static final String	SCOPE_PROFILE = "profile";
    public static final String	EXTRA_RESPONSE = "net.openid.appauth.AuthorizationResponse";
    public static final String	TOKEN_TYPE_BEARER = "bearer";
    public static final String	GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    public static final String	GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    public static final String	GRANT_TYPE_PASSWORD	= "password";
    public static final String	GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    public static final String	EXTRA_EXCEPTION	= "net.openid.appauth.AuthorizationException";
    public static final String	PARAM_ERROR	= "error";
    public static final String	PARAM_ERROR_DESCRIPTION = "error_description";
    public static final String	PARAM_ERROR_URI ="error_uri";
    public static final int	DEFAULT_CODE_VERIFIER_ENTROPY = 64;
    public static final int	MAX_CODE_VERIFIER_ENTROPY = 96;
    public static final int	MAX_CODE_VERIFIER_LENGTH = 128;
    public static final int	MIN_CODE_VERIFIER_ENTROPY = 32;
    public static final int	MIN_CODE_VERIFIER_LENGTH = 43;
    public static final int	TYPE_GENERAL_ERROR = 0;
    public static final int	TYPE_OAUTH_AUTHORIZATION_ERROR = 1;
    public static final int	TYPE_OAUTH_TOKEN_ERROR = 2;
    public static final int	TYPE_RESOURCE_SERVER_AUTHORIZATION_ERROR = 3;
}
