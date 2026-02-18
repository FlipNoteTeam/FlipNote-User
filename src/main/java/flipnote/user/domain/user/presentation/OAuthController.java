package flipnote.user.domain.user.presentation;

import flipnote.user.domain.user.application.OAuthService;
import flipnote.user.domain.user.domain.UserErrorCode;
import flipnote.user.domain.user.domain.UserException;
import flipnote.user.domain.user.infrastructure.TokenPair;
import flipnote.user.global.config.ClientProperties;
import flipnote.user.global.constants.HttpConstants;
import flipnote.user.global.util.CookieUtil;
import flipnote.user.domain.user.infrastructure.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;
    private final JwtProvider jwtProvider;
    private final ClientProperties clientProperties;

    @GetMapping("/oauth2/authorization/{provider}")
    public ResponseEntity<Void> redirectToProvider(
            @PathVariable String provider,
            @RequestHeader(value = HttpConstants.USER_ID_HEADER, required = false) Long userId,
            HttpServletRequest request) {
        OAuthService.AuthorizationRedirect redirect = oAuthService.getAuthorizationUri(provider, request, userId);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, redirect.verifierCookie().toString())
                .location(URI.create(redirect.authorizeUri()))
                .build();
    }

    @GetMapping("/oauth2/callback/{provider}")
    public ResponseEntity<Void> handleCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @CookieValue(HttpConstants.OAUTH_VERIFIER_COOKIE) String codeVerifier,
            HttpServletRequest request,
            HttpServletResponse response) {

        CookieUtil.deleteCookie(response, HttpConstants.OAUTH_VERIFIER_COOKIE);

        boolean isSocialLinkRequest = StringUtils.hasText(state);
        if (isSocialLinkRequest) {
            return handleSocialLink(provider, code, state, codeVerifier, request);
        }
        return handleSocialLogin(provider, code, codeVerifier, request, response);
    }

    private ResponseEntity<Void> handleSocialLogin(String provider, String code, String codeVerifier,
                                                    HttpServletRequest request, HttpServletResponse response) {
        try {
            TokenPair tokenPair = oAuthService.socialLogin(provider, code, codeVerifier, request);
            CookieUtil.addCookie(response, HttpConstants.ACCESS_TOKEN_COOKIE, tokenPair.accessToken(),
                    jwtProvider.getAccessTokenExpiration() / 1000);
            CookieUtil.addCookie(response, HttpConstants.REFRESH_TOKEN_COOKIE, tokenPair.refreshToken(),
                    jwtProvider.getRefreshTokenExpiration() / 1000);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(clientProperties.getUrl() + clientProperties.getPaths().getSocialLoginSuccess()))
                    .build();
        } catch (Exception e) {
            log.warn("소셜 로그인 처리 실패. provider: {}", provider, e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(clientProperties.getUrl() + clientProperties.getPaths().getSocialLoginFailure()))
                    .build();
        }
    }

    private ResponseEntity<Void> handleSocialLink(String provider, String code, String state,
                                                   String codeVerifier, HttpServletRequest request) {
        try {
            oAuthService.linkSocialAccount(provider, code, state, codeVerifier, request);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(clientProperties.getUrl() + clientProperties.getPaths().getSocialLinkSuccess()))
                    .build();
        } catch (UserException e) {
            log.warn("소셜 계정 연동 처리 실패. provider: {}", provider, e);
            if (e.getErrorCode() == UserErrorCode.ALREADY_LINKED_SOCIAL_ACCOUNT) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(clientProperties.getUrl() + clientProperties.getPaths().getSocialLinkConflict()))
                        .build();
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(clientProperties.getUrl() + clientProperties.getPaths().getSocialLinkFailure()))
                    .build();
        }
    }
}
