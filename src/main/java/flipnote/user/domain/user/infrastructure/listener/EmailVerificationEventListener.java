package flipnote.user.domain.user.infrastructure.listener;

import flipnote.user.domain.user.domain.VerificationConstants;
import flipnote.user.domain.user.domain.event.EmailVerificationSendEvent;
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
public class EmailVerificationEventListener {

    private final MailService mailService;

    @Async
    @Retryable(
            maxAttempts = 3,
            retryFor = {EmailSendException.class},
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    @EventListener
    public void handle(EmailVerificationSendEvent event) {
        mailService.sendVerificationCode(event.to(), event.code(), VerificationConstants.CODE_TTL_MINUTES);
    }

    @Recover
    public void recover(EmailSendException ex, EmailVerificationSendEvent event) {
        log.error("이메일 인증번호 전송 실패: to={}", event.to(), ex);
    }
}
