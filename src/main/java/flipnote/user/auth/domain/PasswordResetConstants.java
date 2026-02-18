package flipnote.user.auth.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PasswordResetConstants {

    public static final int TOKEN_TTL_MINUTES = 30;
}
