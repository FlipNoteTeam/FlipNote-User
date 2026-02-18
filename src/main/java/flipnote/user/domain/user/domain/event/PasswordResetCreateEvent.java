package flipnote.user.domain.user.domain.event;

public record PasswordResetCreateEvent(
        String to,
        String link
) {
}
