package com.example.email.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configures separate JavaMailSender beans for Gmail and YopMail providers.
 *
 * Insurance Claims & Policy Management System
 */
@Configuration
public class EmailProviderConfig {

    // ── Gmail ────────────────────────────────────────────────────────
    @Value("${app.mail.gmail.host}")
    private String gmailHost;

    @Value("${app.mail.gmail.port}")
    private int gmailPort;

    @Value("${app.mail.gmail.username}")
    private String gmailUsername;

    @Value("${app.mail.gmail.password}")
    private String gmailPassword;

    // ── YopMail ──────────────────────────────────────────────────────
    @Value("${app.mail.yopmail.host}")
    private String yopMailHost;

    @Value("${app.mail.yopmail.port}")
    private int yopMailPort;

    @Value("${app.mail.yopmail.username}")
    private String yopMailUsername;

    @Value("${app.mail.yopmail.password}")
    private String yopMailPassword;

    /**
     * Primary (default) mail sender — Gmail SMTP.
     */
    @Primary
    @Bean(name = "gmailMailSender")
    public JavaMailSender gmailMailSender() {
        return buildSender(gmailHost, gmailPort, gmailUsername, gmailPassword, "smtp.gmail.com");
    }

    /**
     * Secondary mail sender — YopMail SMTP relay.
     * YopMail does not expose its own outbound SMTP, so we use Gmail SMTP with
     * the yopmail credentials block so that the abstraction stays clean and
     * the password can be rotated independently in application.yml / Config Server.
     */
    @Bean(name = "yopMailSender")
    public JavaMailSender yopMailSender() {
        return buildSender(yopMailHost, yopMailPort, yopMailUsername, yopMailPassword, yopMailHost);
    }

    // ─────────────────────────────────────────────────────────────────
    private JavaMailSender buildSender(String host, int port,
                                       String username, String password,
                                       String sslTrust) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth",           "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust",       sslTrust);
        props.put("mail.debug",                "false");

        return sender;
    }
}
