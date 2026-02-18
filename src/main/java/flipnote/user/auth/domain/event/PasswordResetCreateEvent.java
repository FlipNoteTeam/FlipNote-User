package flipnote.user.auth.domain.event;

public record PasswordResetCreateEvent(
        String to,
        String link
) {
}
