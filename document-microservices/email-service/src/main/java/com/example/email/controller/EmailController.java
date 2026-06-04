package com.example.email.controller;

import com.example.email.dto.ApiResponse;
import com.example.email.dto.EmailRequest;
import com.example.email.dto.EmailResponse;
import com.example.email.service.IEmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for the Email microservice.
 *
 * POST /api/emails/send   — send to multiple recipients with optional attachments
 * GET  /api/emails/health — liveness probe
 *
 * Insurance Claims & Policy Management System — Email Module
 */
@Slf4j
@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final IEmailService emailService;
    private final ObjectMapper  objectMapper;

    // ─────────────────────────────────────────────────────────────────
    // POST /api/emails/send
    // Content-Type: multipart/form-data
    //   Part "emailRequest" → JSON  (see EmailRequest)
    //   Part "attachments"  → files (optional, ≤ 100 KB each)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Send email with optional attachments to one or more recipients.
     *
     * <p>Accepts multipart/form-data so callers can attach files alongside the
     * JSON request body.  The {@code emailRequest} part must be a JSON string
     * matching {@link EmailRequest}.</p>
     *
     * <p>In Postman set Body → form-data:
     * <ul>
     *   <li>Key {@code emailRequest} (type Text) → paste the JSON</li>
     *   <li>Key {@code attachments}  (type File)  → attach file(s) ≤ 100 KB</li>
     * </ul>
     * </p>
     */
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendEmail(
            @RequestPart("emailRequest")                            String emailRequestJson,
            @RequestPart(value = "attachments", required = false)   List<MultipartFile> attachments) {

        try {
            // Deserialise JSON part (Java 8 – ObjectMapper is thread-safe)
            EmailRequest request = objectMapper.readValue(emailRequestJson, EmailRequest.class);

            log.info("Received sendEmail request: from={} recipients={} attachments={}",
                    request.getFromEmail(),
                    Optional.ofNullable(request.getRecipients()).map(List::size).orElse(0),
                    Optional.ofNullable(attachments).map(List::size).orElse(0));

            // Validate attachment sizes (≤ 100 KB)
            validateAttachmentSizes(attachments);

            // Delegate to service layer
            List<EmailResponse> results = emailService.sendEmail(request, attachments);

            // Java 8 Stream — partition success vs failure
            long successCount = results.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
            long failCount    = results.size() - successCount;

            Map<String, Object> responseData = Map.of(
                    "totalRecipients",   results.size(),
                    "successCount",      successCount,
                    "failCount",         failCount,
                    "timestamp",         LocalDateTime.now().toString(),
                    "results",           results
            );

            boolean allSucceeded = failCount == 0;
            String  message      = allSucceeded
                    ? "All " + successCount + " email(s) sent successfully"
                    : successCount + " sent, " + failCount + " failed";

            HttpStatus status = allSucceeded ? HttpStatus.OK : HttpStatus.MULTI_STATUS;
            return ResponseEntity.status(status)
                    .body(new ApiResponse<>(allSucceeded, message, responseData));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid email request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Validation error: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error processing sendEmail: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error: " + e.getMessage(), null));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /api/emails/health
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status",    "UP",
                "service",   "EMAIL-SERVICE",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private static final long MAX_ATTACHMENT_BYTES = 100 * 1024L; // 100 KB

    private void validateAttachmentSizes(List<MultipartFile> attachments) {
        Optional.ofNullable(attachments).ifPresent(files ->
                files.stream()
                     .filter(f -> f != null && !f.isEmpty())
                     .forEach(f -> {
                         if (f.getSize() > MAX_ATTACHMENT_BYTES) {
                             throw new IllegalArgumentException(
                                     "Attachment '" + f.getOriginalFilename() +
                                     "' exceeds the 100 KB limit (" + f.getSize() + " bytes)");
                         }
                     })
        );
    }
}
