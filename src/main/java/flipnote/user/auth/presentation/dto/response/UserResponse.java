package flipnote.user.auth.presentation.dto.response;

import flipnote.user.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long userId;

    public static UserResponse from(User user) {
        return new UserResponse(user.getId());
    }
}
