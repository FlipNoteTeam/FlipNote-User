package flipnote.user.domain.user.infrastructure;

public record TokenClaims(
        Long userId,
        String email,
        String role
) {
}
