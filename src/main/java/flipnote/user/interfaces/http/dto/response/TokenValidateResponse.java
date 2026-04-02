package flipnote.user.interfaces.http.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenValidateResponse {

    private Long userId;
    private String email;
    private String role;
}
