package flipnote.user.auth.domain.event;

public record EmailVerificationSendEvent(
        String to,
        String code
) {
}
