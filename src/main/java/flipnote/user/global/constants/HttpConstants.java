package flipnote.user.global.constants;

public final class HttpConstants {

    private HttpConstants() {
    }

    public static final String USER_ID_HEADER = "X-USER-ID";
    public static final String ACCESS_TOKEN_COOKIE = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    public static final String OAUTH_VERIFIER_COOKIE = "oauth2_auth_request";
}
