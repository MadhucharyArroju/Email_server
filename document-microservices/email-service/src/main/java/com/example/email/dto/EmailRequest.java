package com.example.email.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload for POST /api/emails/send (multipart form).
 *
 * Maps to the JSON part of the multipart request:
 * <pre>
 * {
 *   "recipients": ["user1@gmail.com", "user2@yopmail.com"],
 *   "subject"   : "Policy Renewal Notice",
 *   "body"      : "Your policy is due for renewal...",
 *   "fromEmail" : "madhuchary21@gmail.com"
 * }
 * </pre>
 *
 * File attachments are sent as a separate multipart part named "attachments".
 *
 * Insurance Claims & Policy Management System — Email Module
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    /** One or more recipient email addresses. */
    private List<String> recipients;

    /** Email subject line. */
    private String subject;

    /** Email body (plain text or HTML fragment). */
    private String body;

    /** Sender address — drives provider selection (gmail.com → Gmail, yopmail.com → YopMail). */
    private String fromEmail;

    /** Optional comma-separated CC addresses. */
    private String cc;

    /** Optional comma-separated BCC addresses. */
    private String bcc;
}
