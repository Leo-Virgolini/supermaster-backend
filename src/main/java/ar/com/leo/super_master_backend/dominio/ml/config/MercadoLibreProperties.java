package ar.com.leo.super_master_backend.dominio.ml.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@ConfigurationProperties(prefix = "mercadolibre")
public record MercadoLibreProperties(
        String secretsDir,
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        double rateLimitPerSecond,
        long retryBaseWaitMs
) {
    public MercadoLibreProperties {
        // Valores por defecto
        if (baseUrl == null) {
            baseUrl = "https://api.mercadolibre.com";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(10);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(30);
        }
        if (rateLimitPerSecond <= 0) {
            rateLimitPerSecond = 5.0; // Límite de ML API
        }
        if (retryBaseWaitMs <= 0) {
            retryBaseWaitMs = 2000L;
        }
    }

    public Path getCredentialsFile() {
        return Paths.get(secretsDir).resolve("ml_credentials.json");
    }

    public Path getTokenFile() {
        return Paths.get(secretsDir).resolve("ml_tokens.json");
    }
}
