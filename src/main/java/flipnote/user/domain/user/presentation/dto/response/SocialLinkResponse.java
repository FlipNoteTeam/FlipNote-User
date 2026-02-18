package flipnote.user.domain.user.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import flipnote.user.domain.user.domain.OAuthLink;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SocialLinkResponse {

    private Long socialLinkId;
    private String provider;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime linkedAt;

    public static SocialLinkResponse from(OAuthLink link) {
        return new SocialLinkResponse(link.getId(), link.getProvider(), link.getLinkedAt());
    }
}
