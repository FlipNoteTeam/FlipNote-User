package flipnote.user.domain.user.domain.event;

public record EmailVerificationSendEvent(
        String to,
        String code
) {
}
