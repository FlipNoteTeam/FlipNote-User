package flipnote.user.auth.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenValidateResponse {

    private Long userId;
    private String email;
    private String role;
}
