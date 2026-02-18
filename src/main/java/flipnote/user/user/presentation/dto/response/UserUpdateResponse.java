package flipnote.user.user.presentation.dto.response;

import flipnote.user.user.domain.User;
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

    public static UserUpdateResponse from(User user) {
        return new UserUpdateResponse(
                user.getId(),
                user.getNickname(),
                user.getPhone(),
                user.isSmsAgree(),
                user.getProfileImageUrl(),
                null
        );
    }
}
