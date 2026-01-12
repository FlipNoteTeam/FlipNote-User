package flipnote.user.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserException extends RuntimeException {

    private final HttpStatus status;

    public UserException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static UserException emailAlreadyExists() {
        return new UserException("이미 사용 중인 이메일입니다", HttpStatus.CONFLICT);
    }

    public static UserException invalidCredentials() {
        return new UserException("이메일 또는 비밀번호가 올바르지 않습니다", HttpStatus.UNAUTHORIZED);
    }

    public static UserException userNotFound() {
        return new UserException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND);
    }
}
