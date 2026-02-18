package flipnote.user.user.presentation.dto.response;

import flipnote.user.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserInfoResponse {

    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private Long imageRefId;

    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(user.getId(), user.getNickname(), user.getProfileImageUrl(), null);
    }
}
