package flipnote.user.interfaces.http.dto.response;

import flipnote.user.domain.entity.User;
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
