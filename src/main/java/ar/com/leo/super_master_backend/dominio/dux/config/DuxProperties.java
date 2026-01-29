package ar.com.leo.super_master_backend.dominio.dux.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@ConfigurationProperties(prefix = "dux")
public record DuxProperties(
        String secretsDir,
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        double rateLimitPerSecond,
        int itemsPerPage
) {
    public DuxProperties {
        if (baseUrl == null) {
            baseUrl = "https://erp.duxsoftware.com.ar/WSERP/rest/services";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(10);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(30);
        }
        if (rateLimitPerSecond <= 0) {
            rateLimitPerSecond = 0.2; // 1 request cada 5 segundos (límite DUX)
        }
        if (itemsPerPage <= 0) {
            itemsPerPage = 50; // Máximo permitido por DUX
        }
    }

    public Path getTokenFile() {
        return Paths.get(secretsDir).resolve("dux_tokens.json");
    }
}
