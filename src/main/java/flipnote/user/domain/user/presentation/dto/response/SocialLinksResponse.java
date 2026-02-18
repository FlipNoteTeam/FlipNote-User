package flipnote.user.domain.user.presentation.dto.response;

import flipnote.user.domain.user.domain.OAuthLink;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SocialLinksResponse {

    private List<SocialLinkResponse> socialLinks;

    public static SocialLinksResponse from(List<OAuthLink> links) {
        List<SocialLinkResponse> socialLinks = links.stream()
                .map(SocialLinkResponse::from)
                .toList();
        return new SocialLinksResponse(socialLinks);
    }
}
