package flipnote.user.domain.user.infrastructure.listener;

import flipnote.user.domain.user.domain.VerificationConstants;
import flipnote.user.domain.user.domain.event.EmailVerificationSendEvent;
import flipnote.user.domain.user.infrastructure.MailService;
import flipnote.user.global.exception.EmailSendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationEventListener {

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_DELAY_MS = 2000L;

    private final MailService mailService;

    @Async
    @EventListener
    public void handle(EmailVerificationSendEvent event) {
        long delay = INITIAL_DELAY_MS;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                mailService.sendVerificationCode(event.to(), event.code(), VerificationConstants.CODE_TTL_MINUTES);
                return;
            } catch (EmailSendException e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("이메일 인증번호 전송 실패: to={}", event.to(), e);
                    return;
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                delay *= 2;
            }
        }
    }
}
