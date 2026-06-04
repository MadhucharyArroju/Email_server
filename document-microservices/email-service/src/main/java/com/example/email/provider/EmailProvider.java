package com.example.email.provider;

import com.example.email.dto.EmailRequest;
import com.example.email.dto.EmailResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Strategy interface for sending emails.
 * Each provider (Gmail, YopMail, etc.) implements this contract.
 *
 * Insurance Claims & Policy Management System — Email Module
 */
public interface EmailProvider {

    /**
     * Unique name of this provider (e.g. "GMAIL", "YOPMAIL").
     */
    String getProviderName();

    /**
     * Returns true if this provider can handle the given sender domain.
     *
     * @param senderDomain e.g. "gmail.com" or "yopmail.com"
     */
    boolean supports(String senderDomain);

    /**
     * Send email to a single recipient, optionally with attachments.
     *
     * @param fromEmail   sender address
     * @param toEmail     recipient address
     * @param subject     email subject
     * @param body        HTML or plain-text body
     * @param attachments optional file attachments (may be null or empty)
     * @return EmailResponse summarising the result
     */
    EmailResponse send(String fromEmail,
                       String toEmail,
                       String subject,
                       String body,
                       List<MultipartFile> attachments);
}
