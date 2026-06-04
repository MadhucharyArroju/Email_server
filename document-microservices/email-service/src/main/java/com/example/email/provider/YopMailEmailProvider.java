package com.example.email.provider;

import com.example.email.dto.EmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Optional;

/**
 * YopMail provider — routes email whose SENDER domain is "yopmail.com".
 *
 * YopMail is a disposable-address email service widely used for QA / testing
 * in insurance claims workflows (e.g., claim-acknowledgement receipts to
 * test inboxes).  Because YopMail does not expose outbound SMTP credentials,
 * this provider uses the dedicated yopMailSender bean configured in
 * EmailProviderConfig (which you can point at any relay SMTP you prefer).
 *
 * Spring features used:
 *   • @Retryable   — Spring Retry with exponential back-off
 *   • @Qualifier   — separate bean injection from GmailEmailProvider
 *
 * Java 8 features:
 *   • Stream API, Optional, lambda, method references
 *
 * Insurance Claims & Policy Management System — Email Module
 */
@Slf4j
@Component
public class YopMailEmailProvider implements EmailProvider {

    private static final String PROVIDER_NAME    = "YOPMAIL";
    private static final String SUPPORTED_DOMAIN = "yopmail.com";

    private final JavaMailSender mailSender;

    public YopMailEmailProvider(@Qualifier("yopMailSender") JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supports(String senderDomain) {
        return SUPPORTED_DOMAIN.equalsIgnoreCase(senderDomain);
    }

    /**
     * Sends one email through the YopMail relay.
     * Retried up to 3 times with exponential back-off.
     */
    @Override
    @Retryable(
        value  = { Exception.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 3000, multiplier = 2)
    )
    public EmailResponse send(String fromEmail,
                              String toEmail,
                              String subject,
                              String body,
                              List<MultipartFile> attachments) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[TEST] " + subject);     // mark test emails clearly
            helper.setText(buildTestHtmlBody(body), true);

            // Java 8 Optional + Stream for attachments
            Optional.ofNullable(attachments)
                    .filter(list -> !list.isEmpty())
                    .ifPresent(list -> list.stream()
                            .filter(file -> file != null && !file.isEmpty())
                            .forEach(file -> {
                                try {
                                    helper.addAttachment(
                                            Optional.ofNullable(file.getOriginalFilename()).orElse("test-attachment"),
                                            file
                                    );
                                    log.debug("[YOPMAIL] Attached file: {}", file.getOriginalFilename());
                                } catch (Exception ex) {
                                    log.warn("[YOPMAIL] Could not attach file {}: {}", file.getOriginalFilename(), ex.getMessage());
                                }
                            })
                    );

            mailSender.send(mimeMessage);

            String messageId = mimeMessage.getMessageID();
            log.info("[YOPMAIL] Test email sent → to={} messageId={}", toEmail, messageId);
            return EmailResponse.success(toEmail, PROVIDER_NAME, messageId);

        } catch (Exception e) {
            log.error("[YOPMAIL] Failed to send email → to={} error={}", toEmail, e.getMessage(), e);
            return EmailResponse.failure(toEmail, PROVIDER_NAME, e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private String buildTestHtmlBody(String body) {
        return "<!DOCTYPE html>"
             + "<html><head><meta charset='UTF-8'>"
             + "<style>"
             + "  body { font-family: Arial, sans-serif; font-size: 14px; color: #333; }"
             + "  .banner { background-color: #f0a500; color: #333; padding: 10px 24px; font-weight: bold; }"
             + "  .header { background-color: #003087; color: white; padding: 16px 24px; }"
             + "  .content { padding: 24px; }"
             + "  .footer { background-color: #f4f4f4; padding: 12px 24px; font-size: 11px; color: #888; }"
             + "</style></head><body>"
             + "<div class='banner'>⚠ TEST EMAIL — Insurance Claims QA Environment</div>"
             + "<div class='header'><h2>Insurance Claims &amp; Policy Management System</h2></div>"
             + "<div class='content'><p>" + body + "</p></div>"
             + "<div class='footer'>This is a QA/test message sent via the YopMail provider. Do not forward.</div>"
             + "</body></html>";
    }
}
