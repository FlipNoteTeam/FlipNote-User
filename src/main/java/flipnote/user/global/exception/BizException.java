package flipnote.user.global.exception;

import flipnote.user.global.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BizException extends RuntimeException {

	private ErrorCode errorCode;
}
