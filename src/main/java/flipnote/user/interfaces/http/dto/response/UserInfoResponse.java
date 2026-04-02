package flipnote.user.interfaces.http.dto.response;

import flipnote.user.domain.entity.User;
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
