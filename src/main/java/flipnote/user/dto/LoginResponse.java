package flipnote.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresIn;

    public static LoginResponse of(String accessToken, Long expiresIn) {
        return new LoginResponse(accessToken, "Bearer", expiresIn);
    }
}
