package com.example.email.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.email.dto.EmailRequest;
import com.example.email.dto.ApiResponse;
import com.example.email.service.EmailService;
import java.util.Map;

@RestController
@RequestMapping("/api/emails")
public class EmailController {
    @Autowired
    private EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Map<String, String>>> sendEmail(@RequestBody EmailRequest emailRequest) {
        try {
            emailService.sendSimpleEmail(emailRequest);
            return ResponseEntity.ok(new ApiResponse<>(true,
                "Email sent successfully to " + emailRequest.getTo(),
                Map.of("status", "SUCCESS", "recipient", emailRequest.getTo())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error sending email: " + e.getMessage(), null));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "EMAIL-SERVICE",
            "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }
}
