package flipnote.user.auth.domain;

public record TokenClaims(
        Long userId,
        String email,
        String role
) {
}
