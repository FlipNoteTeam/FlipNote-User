package flipnote.user.application;

import flipnote.user.domain.AuthErrorCode;
import flipnote.user.domain.entity.OAuthLink;
import flipnote.user.domain.repository.OAuthLinkRepository;
import flipnote.user.domain.TokenClaims;
import flipnote.user.domain.TokenPair;
import flipnote.user.domain.entity.User;
import flipnote.user.domain.UserErrorCode;
import flipnote.user.domain.repository.UserRepository;
import flipnote.user.domain.common.BizException;
import flipnote.user.domain.event.EmailVerificationSendEvent;
import flipnote.user.domain.event.PasswordResetCreateEvent;
import flipnote.user.infrastructure.config.ClientProperties;
import flipnote.user.infrastructure.jwt.JwtProvider;
import flipnote.user.infrastructure.redis.EmailVerificationRepository;
import flipnote.user.infrastructure.redis.PasswordResetRepository;
import flipnote.user.infrastructure.redis.PasswordResetTokenGenerator;
import flipnote.user.infrastructure.redis.SessionInvalidationRepository;
import flipnote.user.infrastructure.redis.TokenBlacklistRepository;
import flipnote.user.infrastructure.redis.VerificationCodeGenerator;
import flipnote.user.interfaces.http.dto.request.ChangePasswordRequest;
import flipnote.user.interfaces.http.dto.request.LoginRequest;
import flipnote.user.interfaces.http.dto.request.SignupRequest;
import flipnote.user.interfaces.http.dto.response.SocialLinksResponse;
import flipnote.user.interfaces.http.dto.response.TokenValidateResponse;
import flipnote.user.interfaces.http.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final OAuthLinkRepository oAuthLinkRepository;
    private final SessionInvalidationRepository sessionInvalidationRepository;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final PasswordResetTokenGenerator passwordResetTokenGenerator;
    private final ClientProperties clientProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserResponse register(SignupRequest request) {
        if (!emailVerificationRepository.isVerified(request.getEmail())) {
            throw new BizException(AuthErrorCode.UNVERIFIED_EMAIL);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BizException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .nickname(request.getNickname())
                .phone(request.getPhone())
                .smsAgree(Boolean.TRUE.equals(request.getSmsAgree()))
                .build();

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    public TokenPair login(LoginRequest request) {
        User user = userRepository.findByEmailAndStatus(request.getEmail(), User.Status.ACTIVE)
                .orElseThrow(() -> new BizException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        return jwtProvider.generateTokenPair(user);
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && jwtProvider.isTokenValid(refreshToken)) {
            long remaining = jwtProvider.getRemainingExpiration(refreshToken);
            if (remaining > 0) {
                tokenBlacklistRepository.add(refreshToken, remaining);
            }
        }
    }

    public TokenPair refreshToken(String refreshToken) {
        if (refreshToken == null || !jwtProvider.isTokenValid(refreshToken)) {
            throw new BizException(AuthErrorCode.INVALID_TOKEN);
        }

        if (tokenBlacklistRepository.isBlacklisted(refreshToken)) {
            throw new BizException(AuthErrorCode.BLACKLISTED_TOKEN);
        }

        TokenClaims claims = jwtProvider.extractClaims(refreshToken);

        sessionInvalidationRepository.getInvalidatedAtMillis(claims.userId()).ifPresent(invalidatedAtMillis -> {
            if (jwtProvider.getIssuedAt(refreshToken).getTime() < invalidatedAtMillis) {
                throw new BizException(AuthErrorCode.INVALIDATED_SESSION);
            }
        });

        User user = findActiveUser(claims.userId());

        long remaining = jwtProvider.getRemainingExpiration(refreshToken);
        if (remaining > 0) {
            tokenBlacklistRepository.add(refreshToken, remaining);
        }

        return jwtProvider.generateTokenPair(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findActiveUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BizException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        sessionInvalidationRepository.invalidate(user.getId(), jwtProvider.getRefreshTokenExpiration());
    }

    public TokenValidateResponse validateToken(String token) {
        if (!jwtProvider.isTokenValid(token)) {
            throw new BizException(AuthErrorCode.INVALID_TOKEN);
        }

        if (tokenBlacklistRepository.isBlacklisted(token)) {
            throw new BizException(AuthErrorCode.BLACKLISTED_TOKEN);
        }

        TokenClaims claims = jwtProvider.extractClaims(token);

        sessionInvalidationRepository.getInvalidatedAtMillis(claims.userId()).ifPresent(invalidatedAtMillis -> {
            if (jwtProvider.getIssuedAt(token).getTime() < invalidatedAtMillis) {
                throw new BizException(AuthErrorCode.INVALIDATED_SESSION);
            }
        });

        findActiveUser(claims.userId());

        return new TokenValidateResponse(claims.userId(), claims.email(), claims.role());
    }

    public void sendEmailVerificationCode(String email) {
        if (emailVerificationRepository.hasCode(email)) {
            throw new BizException(AuthErrorCode.ALREADY_ISSUED_VERIFICATION_CODE);
        }

        String code = verificationCodeGenerator.generate();
        emailVerificationRepository.saveCode(email, code);
        eventPublisher.publishEvent(new EmailVerificationSendEvent(email, code));
    }

    public void verifyEmail(String email, String code) {
        if (!emailVerificationRepository.hasCode(email)) {
            throw new BizException(AuthErrorCode.NOT_ISSUED_VERIFICATION_CODE);
        }

        String savedCode = emailVerificationRepository.getCode(email);
        if (!code.equals(savedCode)) {
            throw new BizException(AuthErrorCode.INVALID_VERIFICATION_CODE);
        }

        emailVerificationRepository.deleteCode(email);
        emailVerificationRepository.markVerified(email);
    }

    public void requestPasswordReset(String email) {
        // 사용자가 없어도 정상 반환 (이메일 존재 여부 노출 방지)
        if (!userRepository.existsByEmail(email)) {
            return;
        }

        if (passwordResetRepository.hasToken(email)) {
            throw new BizException(AuthErrorCode.ALREADY_SENT_PASSWORD_RESET_LINK);
        }

        String token = passwordResetTokenGenerator.generate();
        passwordResetRepository.save(token, email);

        String link = clientProperties.getUrl() + clientProperties.getPaths().getPasswordReset()
                + "?token=" + token;
        eventPublisher.publishEvent(new PasswordResetCreateEvent(email, link));
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String email = passwordResetRepository.findEmailByToken(token);
        if (email == null) {
            throw new BizException(AuthErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }

        User user = userRepository.findByEmailAndStatus(email, User.Status.ACTIVE)
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));

        user.changePassword(passwordEncoder.encode(newPassword));
        sessionInvalidationRepository.invalidate(user.getId(), jwtProvider.getRefreshTokenExpiration());
        passwordResetRepository.delete(token, email);
    }

    public SocialLinksResponse getSocialLinks(Long userId) {
        List<OAuthLink> links = oAuthLinkRepository.findByUser_Id(userId);
        return SocialLinksResponse.from(links);
    }

    @Transactional
    public void deleteSocialLink(Long userId, Long socialLinkId) {
        if (!oAuthLinkRepository.existsByIdAndUser_Id(socialLinkId, userId)) {
            throw new BizException(AuthErrorCode.NOT_REGISTERED_SOCIAL_ACCOUNT);
        }
        oAuthLinkRepository.deleteById(socialLinkId);
    }

    private User findActiveUser(Long userId) {
        return userRepository.findByIdAndStatus(userId, User.Status.ACTIVE)
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));
    }
}
