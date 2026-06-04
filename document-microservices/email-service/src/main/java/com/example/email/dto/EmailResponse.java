package com.example.email.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response payload returned after an email send attempt.
 *
 * Insurance Claims & Policy Management System — Email Module
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailResponse {

    private String recipient;
    private String provider;          // e.g. "GMAIL" or "YOPMAIL"
    private String status;            // "SUCCESS" | "FAILED"
    private String messageId;         // SMTP message-id header if available
    private String errorMessage;      // populated on failure
    private LocalDateTime sentAt;

    // ── Java 8 factory helpers ────────────────────────────────────────

    public static EmailResponse success(String recipient, String provider, String messageId) {
        return EmailResponse.builder()
                .recipient(recipient)
                .provider(provider)
                .status("SUCCESS")
                .messageId(messageId)
                .sentAt(LocalDateTime.now())
                .build();
    }

    public static EmailResponse failure(String recipient, String provider, String errorMessage) {
        return EmailResponse.builder()
                .recipient(recipient)
                .provider(provider)
                .status("FAILED")
                .errorMessage(errorMessage)
                .sentAt(LocalDateTime.now())
                .build();
    }
}
