package flipnote.user.domain.user.domain;

import flipnote.user.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "이미 탈퇴한 사용자입니다."),

    ALREADY_ISSUED_VERIFICATION_CODE(HttpStatus.TOO_MANY_REQUESTS, "이미 인증코드가 발송되었습니다. 잠시 후 다시 시도해 주세요."),
    NOT_ISSUED_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증코드가 발송되지 않았습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증코드가 올바르지 않습니다."),
    UNVERIFIED_EMAIL(HttpStatus.FORBIDDEN, "이메일 인증이 완료되지 않았습니다."),

    ALREADY_SENT_PASSWORD_RESET_LINK(HttpStatus.TOO_MANY_REQUESTS, "이미 비밀번호 재설정 링크가 발송되었습니다. 잠시 후 다시 시도해 주세요."),
    INVALID_PASSWORD_RESET_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 비밀번호 재설정 토큰입니다."),

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "무효화된 토큰입니다."),
    INVALIDATED_SESSION(HttpStatus.UNAUTHORIZED, "세션이 무효화되었습니다. 다시 로그인해 주세요."),

    INVALID_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth 제공자입니다."),
    NOT_REGISTERED_SOCIAL_ACCOUNT(HttpStatus.NOT_FOUND, "연결된 소셜 계정이 없습니다."),
    ALREADY_LINKED_SOCIAL_ACCOUNT(HttpStatus.CONFLICT, "이미 연결된 소셜 계정입니다."),
    INVALID_SOCIAL_LINK_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 소셜 연동 토큰입니다.");

    private final HttpStatus status;
    private final String message;
}
