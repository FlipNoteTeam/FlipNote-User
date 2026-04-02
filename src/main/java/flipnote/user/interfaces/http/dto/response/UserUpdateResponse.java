package flipnote.user.interfaces.http.dto.response;

import flipnote.user.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserUpdateResponse {

    private Long userId;
    private String nickname;
    private String phone;
    private Boolean smsAgree;
    private String profileImageUrl;
    private Long imageRefId;

    public static UserUpdateResponse from(User user, Long imageRefId) {
        return new UserUpdateResponse(
            user.getId(),
            user.getNickname(),
            user.getPhone(),
            user.isSmsAgree(),
            user.getProfileImageUrl(),
            imageRefId
        );
    }
}
