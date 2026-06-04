package com.example.email.service;

import com.example.email.dto.EmailRequest;
import com.example.email.dto.EmailResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service abstraction for sending emails in the
 * Insurance Claims & Policy Management System.
 *
 * Decouples the controller from any specific mail implementation,
 * enabling easy substitution (e.g., swapping SMTP for SendGrid).
 */
public interface IEmailService {

    /**
     * Send an email to multiple recipients, optionally with file attachments.
     *
     * @param request     contains recipients, subject, body, fromEmail
     * @param attachments files to attach (may be null or empty)
     * @return per-recipient send results
     */
    List<EmailResponse> sendEmail(EmailRequest request, List<MultipartFile> attachments);

    /**
     * Send a simple email (no attachments) — convenience overload.
     */
    default List<EmailResponse> sendEmail(EmailRequest request) {
        return sendEmail(request, null);
    }
}
