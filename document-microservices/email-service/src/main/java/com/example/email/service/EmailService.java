package com.example.email.service;

import com.example.email.dto.EmailRequest;
import com.example.email.dto.EmailResponse;
import com.example.email.provider.EmailProvider;
import com.example.email.provider.EmailProviderFactory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Concrete implementation of {@link IEmailService}.
 *
 * Spring Cloud features:
 *   • @CircuitBreaker (Resilience4J) — open on repeated failures, fallback returns FAILED responses
 *   • Eureka-registered service (wired at application level)
 *
 * Java 8 features:
 *   • Stream API for fan-out to multiple recipients
 *   • Optional for null safety
 *   • Collectors, lambda, method references
 *   • LocalDateTime for timestamps
 *
 * Insurance Claims & Policy Management System — Email Module
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private static final String CB_NAME = "emailService";

    private final EmailProviderFactory providerFactory;

    /**
     * Sends email to all recipients.
     * Protected by a Resilience4J circuit breaker; on open-state,
     * {@link #sendEmailFallback} is called instead.
     *
     * Fan-out logic (Java 8 Stream):
     *   For each recipient → resolve provider → send → collect EmailResponse.
     */
    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "sendEmailFallback")
    public List<EmailResponse> sendEmail(EmailRequest request, List<MultipartFile> attachments) {

        // Validate request
        validateRequest(request);

        String fromEmail = Optional.ofNullable(request.getFromEmail())
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("fromEmail must not be blank"));

        List<String> recipients = Optional.ofNullable(request.getRecipients())
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("At least one recipient is required"));

        // Resolve provider once (based on sender domain)
        EmailProvider provider = providerFactory.getProvider(fromEmail);
        log.info("Sending email via provider={} from={} recipients={} attachments={}",
                provider.getProviderName(), fromEmail, recipients.size(),
                Optional.ofNullable(attachments).map(List::size).orElse(0));

        // Java 8 Stream fan-out: send to each recipient, collect results
        List<EmailResponse> results = recipients.stream()
                .filter(Objects::nonNull)
                .filter(r -> !r.isBlank())
                .map(recipient -> {
                    log.debug("Dispatching to recipient: {}", recipient);
                    return provider.send(fromEmail, recipient,
                            request.getSubject(), request.getBody(), attachments);
                })
                .collect(Collectors.toList());

        long successCount = results.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        long failCount    = results.size() - successCount;
        log.info("Email dispatch complete — success={} failed={}", successCount, failCount);

        return results;
    }

    // ── Circuit-breaker fallback ──────────────────────────────────────

    /**
     * Called by Resilience4J when the circuit is open or on repeated failure.
     * Returns FAILED responses for all requested recipients so the caller
     * receives a structured error rather than an exception.
     */
    @SuppressWarnings("unused")
    public List<EmailResponse> sendEmailFallback(EmailRequest request,
                                                  List<MultipartFile> attachments,
                                                  Throwable t) {
        log.error("[CIRCUIT BREAKER] Email service unavailable — falling back. Cause: {}", t.getMessage());

        List<String> recipients = Optional.ofNullable(request)
                .map(EmailRequest::getRecipients)
                .orElse(Collections.emptyList());

        return recipients.stream()
                .map(r -> EmailResponse.builder()
                        .recipient(r)
                        .provider("CIRCUIT_BREAKER_FALLBACK")
                        .status("FAILED")
                        .errorMessage("Email service temporarily unavailable: " + t.getMessage())
                        .sentAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void validateRequest(EmailRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("EmailRequest must not be null");
        }
        Optional.ofNullable(request.getSubject())
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Email subject must not be blank"));
        Optional.ofNullable(request.getBody())
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Email body must not be blank"));
    }
}
