package flipnote.user.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {

	IMAGE_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_001", "이미지 서비스 처리 중 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	@Override
	public int getStatus() {
		return httpStatus.value();
	}
}
