package flipnote.user.domain.user.presentation;

import flipnote.user.domain.user.application.AuthService;
import flipnote.user.domain.user.infrastructure.JwtProvider;
import flipnote.user.domain.user.infrastructure.TokenPair;
import flipnote.user.domain.user.presentation.dto.request.*;
import flipnote.user.domain.user.presentation.dto.response.SocialLinksResponse;
import flipnote.user.domain.user.presentation.dto.response.TokenValidateResponse;
import flipnote.user.domain.user.presentation.dto.response.UserResponse;
import flipnote.user.global.constants.HttpConstants;
import flipnote.user.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody SignupRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request,
                                      HttpServletResponse response) {
        TokenPair tokenPair = authService.login(request);
        setTokenCookies(response, tokenPair);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = HttpConstants.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        CookieUtil.deleteCookie(response, HttpConstants.ACCESS_TOKEN_COOKIE);
        CookieUtil.deleteCookie(response, HttpConstants.REFRESH_TOKEN_COOKIE);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<Void> refreshToken(
            @CookieValue(name = HttpConstants.REFRESH_TOKEN_COOKIE) String refreshToken,
            HttpServletResponse response) {
        TokenPair tokenPair = authService.refreshToken(refreshToken);
        setTokenCookies(response, tokenPair);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/token/validate")
    public ResponseEntity<TokenValidateResponse> validateToken(
            @Valid @RequestBody TokenValidateRequest request) {
        TokenValidateResponse result = authService.validateToken(request.getToken());
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader(HttpConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse response) {
        authService.changePassword(userId, request);
        CookieUtil.deleteCookie(response, HttpConstants.ACCESS_TOKEN_COOKIE);
        CookieUtil.deleteCookie(response, HttpConstants.REFRESH_TOKEN_COOKIE);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-verification/request")
    public ResponseEntity<Void> sendEmailVerification(
            @Valid @RequestBody EmailVerificationRequest request) {
        authService.sendEmailVerificationCode(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email-verification")
    public ResponseEntity<Void> verifyEmail(
            @Valid @RequestBody EmailVerifyRequest request) {
        authService.verifyEmail(request.getEmail(), request.getCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(
            @Valid @RequestBody PasswordResetCreateRequest request) {
        authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request.getToken(), request.getPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/social-links")
    public ResponseEntity<SocialLinksResponse> getSocialLinks(
            @RequestHeader(HttpConstants.USER_ID_HEADER) Long userId) {
        SocialLinksResponse response = authService.getSocialLinks(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/social-links/{socialLinkId}")
    public ResponseEntity<Void> deleteSocialLink(
            @RequestHeader(HttpConstants.USER_ID_HEADER) Long userId,
            @PathVariable Long socialLinkId) {
        authService.deleteSocialLink(userId, socialLinkId);
        return ResponseEntity.noContent().build();
    }

    private void setTokenCookies(HttpServletResponse response, TokenPair tokenPair) {
        CookieUtil.addCookie(response, HttpConstants.ACCESS_TOKEN_COOKIE, tokenPair.accessToken(),
                jwtProvider.getAccessTokenExpiration() / 1000);
        CookieUtil.addCookie(response, HttpConstants.REFRESH_TOKEN_COOKIE, tokenPair.refreshToken(),
                jwtProvider.getRefreshTokenExpiration() / 1000);
    }
}
