package flipnote.user.domain.user.application;

import flipnote.user.domain.user.domain.*;
import flipnote.user.domain.user.domain.event.EmailVerificationSendEvent;
import flipnote.user.domain.user.domain.event.PasswordResetCreateEvent;
import flipnote.user.domain.user.infrastructure.*;
import flipnote.user.domain.user.presentation.dto.request.*;
import flipnote.user.domain.user.presentation.dto.response.SocialLinksResponse;
import flipnote.user.domain.user.presentation.dto.response.TokenValidateResponse;
import flipnote.user.domain.user.presentation.dto.response.UserResponse;
import flipnote.user.global.config.ClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Date;
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
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final PasswordResetTokenGenerator passwordResetTokenGenerator;
    private final ClientProperties clientProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserResponse register(SignupRequest request) {
        if (!emailVerificationRepository.isVerified(request.getEmail())) {
            throw new UserException(UserErrorCode.UNVERIFIED_EMAIL);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserException(UserErrorCode.EMAIL_ALREADY_EXISTS);
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
                .orElseThrow(() -> new UserException(UserErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserException(UserErrorCode.INVALID_CREDENTIALS);
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
            throw new UserException(UserErrorCode.INVALID_TOKEN);
        }

        if (tokenBlacklistRepository.isBlacklisted(refreshToken)) {
            throw new UserException(UserErrorCode.BLACKLISTED_TOKEN);
        }

        TokenClaims claims = jwtProvider.extractClaims(refreshToken);
        User user = findActiveUser(claims.userId());

        if (user.getInvalidatedAt() != null) {
            Date issuedAt = jwtProvider.getIssuedAt(refreshToken);
            if (issuedAt.before(Date.from(user.getInvalidatedAt()
                    .atZone(ZoneId.systemDefault()).toInstant()))) {
                throw new UserException(UserErrorCode.INVALIDATED_SESSION);
            }
        }

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
            throw new UserException(UserErrorCode.PASSWORD_MISMATCH);
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    public TokenValidateResponse validateToken(String token) {
        if (!jwtProvider.isTokenValid(token)) {
            throw new UserException(UserErrorCode.INVALID_TOKEN);
        }

        if (tokenBlacklistRepository.isBlacklisted(token)) {
            throw new UserException(UserErrorCode.BLACKLISTED_TOKEN);
        }

        TokenClaims claims = jwtProvider.extractClaims(token);
        User user = findActiveUser(claims.userId());

        if (user.getInvalidatedAt() != null) {
            Date issuedAt = jwtProvider.getIssuedAt(token);
            if (issuedAt.before(Date.from(user.getInvalidatedAt()
                    .atZone(ZoneId.systemDefault()).toInstant()))) {
                throw new UserException(UserErrorCode.INVALIDATED_SESSION);
            }
        }

        return new TokenValidateResponse(claims.userId(), claims.email(), claims.role());
    }

    public void sendEmailVerificationCode(String email) {
        if (emailVerificationRepository.hasCode(email)) {
            throw new UserException(UserErrorCode.ALREADY_ISSUED_VERIFICATION_CODE);
        }

        String code = verificationCodeGenerator.generate();
        emailVerificationRepository.saveCode(email, code);
        eventPublisher.publishEvent(new EmailVerificationSendEvent(email, code));
    }

    public void verifyEmail(String email, String code) {
        if (!emailVerificationRepository.hasCode(email)) {
            throw new UserException(UserErrorCode.NOT_ISSUED_VERIFICATION_CODE);
        }

        String savedCode = emailVerificationRepository.getCode(email);
        if (!code.equals(savedCode)) {
            throw new UserException(UserErrorCode.INVALID_VERIFICATION_CODE);
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
            throw new UserException(UserErrorCode.ALREADY_SENT_PASSWORD_RESET_LINK);
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
            throw new UserException(UserErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }

        User user = userRepository.findByEmailAndStatus(email, User.Status.ACTIVE)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        user.changePassword(passwordEncoder.encode(newPassword));
        passwordResetRepository.delete(token, email);
    }

    public SocialLinksResponse getSocialLinks(Long userId) {
        List<OAuthLink> links = oAuthLinkRepository.findByUser_Id(userId);
        return SocialLinksResponse.from(links);
    }

    @Transactional
    public void deleteSocialLink(Long userId, Long socialLinkId) {
        if (!oAuthLinkRepository.existsByIdAndUser_Id(socialLinkId, userId)) {
            throw new UserException(UserErrorCode.NOT_REGISTERED_SOCIAL_ACCOUNT);
        }
        oAuthLinkRepository.deleteById(socialLinkId);
    }

    private User findActiveUser(Long userId) {
        return userRepository.findByIdAndStatus(userId, User.Status.ACTIVE)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }
}
