package com.jungle_choi.namanmu.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
        List<String> corsAllowedOrigins,
        boolean cookieSecure,
        String cookieSameSite,
        boolean productionMode) {

    public List<String> normalizedCorsAllowedOrigins() {
        if (corsAllowedOrigins == null || corsAllowedOrigins.isEmpty()) {
            return List.of("http://localhost:5173", "http://127.0.0.1:5173");
        }

        return corsAllowedOrigins;
    }

    public String normalizedCookieSameSite() {
        if (cookieSameSite == null || cookieSameSite.isBlank()) {
            return "Lax";
        }

        return cookieSameSite;
    }
}
