package flipnote.user.auth.application;

import flipnote.user.auth.domain.AuthErrorCode;
import flipnote.user.auth.domain.TokenPair;
import flipnote.user.auth.infrastructure.jwt.JwtProvider;
import flipnote.user.auth.infrastructure.oauth.OAuthApiClient;
import flipnote.user.auth.infrastructure.oauth.OAuth2UserInfo;
import flipnote.user.auth.infrastructure.oauth.PkceUtil;
import flipnote.user.auth.infrastructure.redis.SocialLinkTokenRepository;
import flipnote.user.global.config.OAuthProperties;
import flipnote.user.global.constants.HttpConstants;
import flipnote.user.global.exception.BizException;
import flipnote.user.user.domain.OAuthLink;
import flipnote.user.user.domain.OAuthLinkRepository;
import flipnote.user.user.domain.User;
import flipnote.user.user.domain.UserErrorCode;
import flipnote.user.user.domain.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthService {

    private final PkceUtil pkceUtil;
    private final OAuthApiClient oAuthApiClient;
    private final OAuthLinkRepository oAuthLinkRepository;
    private final UserRepository userRepository;
    private final SocialLinkTokenRepository socialLinkTokenRepository;
    private final JwtProvider jwtProvider;
    private final OAuthProperties oAuthProperties;

    public record AuthorizationRedirect(String authorizeUri, ResponseCookie verifierCookie) {}

    private static final int VERIFIER_COOKIE_MAX_AGE = 180;

    public AuthorizationRedirect getAuthorizationUri(String providerName, Long userId) {
        OAuthProperties.Provider provider = resolveProvider(providerName);

        String codeVerifier = pkceUtil.generateCodeVerifier();
        String codeChallenge = pkceUtil.generateCodeChallenge(codeVerifier);

        String state = null;
        if (userId != null) {
            state = UUID.randomUUID().toString();
            socialLinkTokenRepository.save(userId, state);
        }

        String authorizeUri = oAuthApiClient.buildAuthorizeUri(provider, codeChallenge, state);

        ResponseCookie verifierCookie = ResponseCookie.from(HttpConstants.OAUTH_VERIFIER_COOKIE, codeVerifier)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(VERIFIER_COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();

        return new AuthorizationRedirect(authorizeUri, verifierCookie);
    }

    public TokenPair socialLogin(String providerName, String code, String codeVerifier) {
        OAuth2UserInfo userInfo = getOAuth2UserInfo(providerName, code, codeVerifier);

        OAuthLink oAuthLink = oAuthLinkRepository
                .findByProviderAndProviderIdWithUser(userInfo.getProvider(), userInfo.getProviderId())
                .orElseThrow(() -> new BizException(AuthErrorCode.NOT_REGISTERED_SOCIAL_ACCOUNT));

        return jwtProvider.generateTokenPair(oAuthLink.getUser());
    }

    @Transactional
    public void linkSocialAccount(String providerName, String code, String state,
                                  String codeVerifier) {
        Long userId = socialLinkTokenRepository.findUserIdByState(state)
                .orElseThrow(() -> new BizException(AuthErrorCode.INVALID_SOCIAL_LINK_TOKEN));

        socialLinkTokenRepository.delete(state);

        OAuth2UserInfo userInfo = getOAuth2UserInfo(providerName, code, codeVerifier);

        if (oAuthLinkRepository.existsByUser_IdAndProviderAndProviderId(
                userId, userInfo.getProvider(), userInfo.getProviderId())) {
            throw new BizException(AuthErrorCode.ALREADY_LINKED_SOCIAL_ACCOUNT);
        }

        User user = userRepository.findByIdAndStatus(userId, User.Status.ACTIVE)
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));

        OAuthLink link = OAuthLink.builder()
                .provider(userInfo.getProvider())
                .providerId(userInfo.getProviderId())
                .user(user)
                .build();
        oAuthLinkRepository.save(link);
    }

    private OAuth2UserInfo getOAuth2UserInfo(String providerName, String code,
                                              String codeVerifier) {
        OAuthProperties.Provider provider = resolveProvider(providerName);
        String accessToken = oAuthApiClient.requestAccessToken(provider, code, codeVerifier);
        Map<String, Object> attributes = oAuthApiClient.requestUserInfo(provider, accessToken);
        return oAuthApiClient.createUserInfo(providerName, attributes);
    }

    private OAuthProperties.Provider resolveProvider(String providerName) {
        Map<String, OAuthProperties.Provider> providers = oAuthProperties.getProviders();
        if (providers == null) {
            throw new BizException(AuthErrorCode.INVALID_OAUTH_PROVIDER);
        }
        OAuthProperties.Provider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            log.warn("지원하지 않는 OAuth Provider: {}", providerName);
            throw new BizException(AuthErrorCode.INVALID_OAUTH_PROVIDER);
        }
        return provider;
    }
}
