package flipnote.user.user.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import flipnote.user.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MyInfoResponse {

    private Long userId;
    private String email;
    private String nickname;
    private String name;
    private String phone;
    private Boolean smsAgree;
    private String profileImageUrl;
    private Long imageRefId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modifiedAt;

    public static MyInfoResponse from(User user) {
        return new MyInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getName(),
                user.getPhone(),
                user.isSmsAgree(),
                user.getProfileImageUrl(),
                null,
                user.getCreatedAt(),
                user.getModifiedAt()
        );
    }
}
