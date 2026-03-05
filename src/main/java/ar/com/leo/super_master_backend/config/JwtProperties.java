package ar.com.leo.super_master_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        long accessTokenExpirationMs
) {
    public JwtProperties {
        if (accessTokenExpirationMs <= 0) accessTokenExpirationMs = 43200000; // 12 horas
    }
}
