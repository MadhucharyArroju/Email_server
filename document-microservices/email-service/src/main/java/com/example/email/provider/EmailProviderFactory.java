package com.example.email.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Factory that selects the correct {@link EmailProvider} for a given sender address.
 *
 * Resolution order:
 *   1. Iterate registered providers and pick the first that {@code supports()} the sender domain.
 *   2. Fall back to the Gmail provider (index 0) if nothing matches.
 *
 * Java 8 features used: Stream, Optional, method references.
 *
 * Insurance Claims & Policy Management System — Email Module
 */
@Slf4j
@Component
public class EmailProviderFactory {

    private final List<EmailProvider> providers;

    /**
     * Spring injects all EmailProvider beans automatically via List injection.
     */
    public EmailProviderFactory(List<EmailProvider> providers) {
        this.providers = providers;
    }

    /**
     * Returns the best-matching provider for the given sender email address.
     *
     * @param fromEmail full sender address, e.g. "agent@gmail.com"
     * @return the matching {@link EmailProvider}
     */
    public EmailProvider getProvider(String fromEmail) {
        String domain = extractDomain(fromEmail);
        log.debug("Resolving email provider for sender domain: {}", domain);

        return providers.stream()
                .filter(p -> p.supports(domain))
                .findFirst()
                .orElseGet(() -> {
                    log.debug("No provider matched domain '{}'; falling back to first registered provider: {}",
                            domain, providers.isEmpty() ? "NONE" : providers.get(0).getProviderName());
                    return providers.stream()
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("No EmailProvider beans registered"));
                });
    }

    /**
     * Extracts the domain portion of an email address.
     * Returns the whole string if '@' is absent (defensive).
     */
    private String extractDomain(String email) {
        return Optional.ofNullable(email)
                .filter(e -> e.contains("@"))
                .map(e -> e.substring(e.lastIndexOf('@') + 1).toLowerCase())
                .orElse("");
    }
}
