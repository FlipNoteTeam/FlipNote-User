package flipnote.user.domain.user.infrastructure.listener;

import flipnote.user.domain.user.domain.PasswordResetConstants;
import flipnote.user.domain.user.domain.event.PasswordResetCreateEvent;
import flipnote.user.domain.user.infrastructure.MailService;
import flipnote.user.global.exception.EmailSendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetEventListener {

    private final MailService mailService;

    @Async
    @Retryable(
            maxAttempts = 3,
            retryFor = {EmailSendException.class},
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    @EventListener
    public void handle(PasswordResetCreateEvent event) {
        mailService.sendPasswordResetLink(event.to(), event.link(), PasswordResetConstants.TOKEN_TTL_MINUTES);
    }

    @Recover
    public void recover(EmailSendException ex, PasswordResetCreateEvent event) {
        log.error("비밀번호 재설정 링크 전송 실패: to={}", event.to(), ex);
    }
}
