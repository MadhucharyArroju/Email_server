package com.example.email.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.example.email.dto.EmailRequest;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendSimpleEmail(EmailRequest emailRequest) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailRequest.getFrom() != null ? emailRequest.getFrom() : fromEmail);
            message.setTo(emailRequest.getTo());
            message.setSubject(emailRequest.getSubject());
            message.setText(emailRequest.getBody());
            
            if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
                message.setCc(emailRequest.getCc().split(","));
            }
            if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
                message.setBcc(emailRequest.getBcc().split(","));
            }
            
            javaMailSender.send(message);
            System.out.println("Email sent successfully to: " + emailRequest.getTo());
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            throw new RuntimeException("Error sending email: " + e.getMessage());
        }
    }
}
