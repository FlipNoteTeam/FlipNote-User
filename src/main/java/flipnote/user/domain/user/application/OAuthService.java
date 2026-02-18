package flipnote.user.domain.user.application;

import flipnote.user.domain.user.domain.*;
import flipnote.user.domain.user.infrastructure.*;
import flipnote.user.global.config.OAuthProperties;
import flipnote.user.global.constants.HttpConstants;
import jakarta.servlet.http.HttpServletRequest;
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

    public AuthorizationRedirect getAuthorizationUri(String providerName, HttpServletRequest request,
                                                     Long userId) {
        OAuthProperties.Provider provider = resolveProvider(providerName);

        String codeVerifier = pkceUtil.generateCodeVerifier();
        String codeChallenge = pkceUtil.generateCodeChallenge(codeVerifier);

        String state = null;
        if (userId != null) {
            state = UUID.randomUUID().toString();
            socialLinkTokenRepository.save(userId, state);
        }

        String authorizeUri = oAuthApiClient.buildAuthorizeUri(request, provider, codeChallenge, state);

        ResponseCookie verifierCookie = ResponseCookie.from(HttpConstants.OAUTH_VERIFIER_COOKIE, codeVerifier)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(VERIFIER_COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();

        return new AuthorizationRedirect(authorizeUri, verifierCookie);
    }

    public TokenPair socialLogin(String providerName, String code, String codeVerifier,
                                 HttpServletRequest request) {
        OAuth2UserInfo userInfo = getOAuth2UserInfo(providerName, code, codeVerifier, request);

        OAuthLink oAuthLink = oAuthLinkRepository
                .findByProviderAndProviderIdWithUser(userInfo.getProvider(), userInfo.getProviderId())
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_REGISTERED_SOCIAL_ACCOUNT));

        return jwtProvider.generateTokenPair(oAuthLink.getUser());
    }

    @Transactional
    public void linkSocialAccount(String providerName, String code, String state,
                                  String codeVerifier, HttpServletRequest request) {
        Long userId = socialLinkTokenRepository.findUserIdByState(state)
                .orElseThrow(() -> new UserException(UserErrorCode.INVALID_SOCIAL_LINK_TOKEN));

        socialLinkTokenRepository.delete(state);

        OAuth2UserInfo userInfo = getOAuth2UserInfo(providerName, code, codeVerifier, request);

        if (oAuthLinkRepository.existsByUser_IdAndProviderAndProviderId(
                userId, userInfo.getProvider(), userInfo.getProviderId())) {
            throw new UserException(UserErrorCode.ALREADY_LINKED_SOCIAL_ACCOUNT);
        }

        User user = userRepository.findByIdAndStatus(userId, User.Status.ACTIVE)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        OAuthLink link = OAuthLink.builder()
                .provider(userInfo.getProvider())
                .providerId(userInfo.getProviderId())
                .user(user)
                .build();
        oAuthLinkRepository.save(link);
    }

    private OAuth2UserInfo getOAuth2UserInfo(String providerName, String code,
                                              String codeVerifier, HttpServletRequest request) {
        OAuthProperties.Provider provider = resolveProvider(providerName);
        String accessToken = oAuthApiClient.requestAccessToken(provider, code, codeVerifier, request);
        Map<String, Object> attributes = oAuthApiClient.requestUserInfo(provider, accessToken);
        return oAuthApiClient.createUserInfo(providerName, attributes);
    }

    private OAuthProperties.Provider resolveProvider(String providerName) {
        Map<String, OAuthProperties.Provider> providers = oAuthProperties.getProviders();
        if (providers == null) {
            throw new UserException(UserErrorCode.INVALID_OAUTH_PROVIDER);
        }
        OAuthProperties.Provider provider = providers.get(providerName.toLowerCase());
        if (provider == null) {
            log.warn("지원하지 않는 OAuth Provider: {}", providerName);
            throw new UserException(UserErrorCode.INVALID_OAUTH_PROVIDER);
        }
        return provider;
    }
}
