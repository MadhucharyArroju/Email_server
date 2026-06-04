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
 * Gmail SMTP provider — handles any sender whose domain is gmail.com.
 *
 * Spring features used:
 *   • @Retryable   — auto-retries on transient SMTP failures (Spring Retry)
 *   • @Qualifier   — injects the dedicated Gmail JavaMailSender bean
 *
 * Java 8 features used:
 *   • Optional for null-safe attachment handling
 *   • Stream + forEach for iterating attachments
 *
 * Insurance Claims & Policy Management System — Email Module
 */
@Slf4j
@Component
public class GmailEmailProvider implements EmailProvider {

    private static final String PROVIDER_NAME   = "GMAIL";
    private static final String SUPPORTED_DOMAIN = "gmail.com";

    private final JavaMailSender mailSender;

    public GmailEmailProvider(@Qualifier("gmailMailSender") JavaMailSender mailSender) {
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
     * Sends one email via Gmail SMTP.
     * Retried up to 3 times with exponential back-off on MessagingException.
     */
    @Override
    @Retryable(
        value  = { Exception.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public EmailResponse send(String fromEmail,
                              String toEmail,
                              String subject,
                              String body,
                              List<MultipartFile> attachments) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            // multipart = true so we can attach files
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(buildHtmlBody(body), true);   // true = isHtml

            // Java 8 Optional — attach only when list is non-null and non-empty
            Optional.ofNullable(attachments)
                    .filter(list -> !list.isEmpty())
                    .ifPresent(list -> list.stream()
                            .filter(file -> file != null && !file.isEmpty())
                            .forEach(file -> {
                                try {
                                    helper.addAttachment(
                                            Optional.ofNullable(file.getOriginalFilename()).orElse("attachment"),
                                            file
                                    );
                                    log.debug("[GMAIL] Attached file: {}", file.getOriginalFilename());
                                } catch (Exception ex) {
                                    log.warn("[GMAIL] Could not attach file {}: {}", file.getOriginalFilename(), ex.getMessage());
                                }
                            })
                    );

            mailSender.send(mimeMessage);

            String messageId = mimeMessage.getMessageID();
            log.info("[GMAIL] Email sent successfully → to={} messageId={}", toEmail, messageId);
            return EmailResponse.success(toEmail, PROVIDER_NAME, messageId);

        } catch (Exception e) {
            log.error("[GMAIL] Failed to send email → to={} error={}", toEmail, e.getMessage(), e);
            return EmailResponse.failure(toEmail, PROVIDER_NAME, e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    /**
     * Wraps plain text in a minimal HTML template with insurance branding.
     */
    private String buildHtmlBody(String body) {
        return "<!DOCTYPE html>"
             + "<html><head><meta charset='UTF-8'>"
             + "<style>"
             + "  body { font-family: Arial, sans-serif; font-size: 14px; color: #333; }"
             + "  .header { background-color: #003087; color: white; padding: 16px 24px; }"
             + "  .content { padding: 24px; }"
             + "  .footer { background-color: #f4f4f4; padding: 12px 24px; font-size: 11px; color: #888; }"
             + "</style></head><body>"
             + "<div class='header'><h2>Insurance Claims &amp; Policy Management System</h2></div>"
             + "<div class='content'><p>" + body + "</p></div>"
             + "<div class='footer'>This is an automated message. Please do not reply directly to this email.</div>"
             + "</body></html>";
    }
}
