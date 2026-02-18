package flipnote.user.domain.user.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenValidateResponse {

    private Long userId;
    private String email;
    private String role;
}
