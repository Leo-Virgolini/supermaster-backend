package ar.com.leo.super_master_backend.dominio.dux;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Handler de reintentos para DUX con soporte para:
 * - Rate limiting (1 request cada 5 segundos por defecto)
 * - Reintentos con backoff exponencial
 * - Manejo de errores 5xx, conflictos, errores de conexión
 *
 * <p>A diferencia de ML, DUX usa un token fijo que no requiere refresh automático.</p>
 */
@Slf4j
public class DuxRetryHandler {

    private static final int MAX_RETRIES = 3;
    private static final int MAX_RETRIES_RATE_LIMIT = 5;
    private static final long MAX_WAIT_MS = 300000; // 5 minutos
    private static final long CONFLICT_BASE_WAIT_MS = 2000; // DUX es más lento

    private final RestClient restClient;
    private final long baseWaitMs;
    private final RateLimiter rateLimiter;

    public DuxRetryHandler(RestClient restClient, long baseWaitMs, double permitsPerSecond) {
        this.restClient = restClient;
        this.baseWaitMs = baseWaitMs;
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
    }

    /**
     * Ejecuta una petición GET con reintentos.
     */
    public String get(String uri, String token) {
        return executeGet(uri, token, String.class);
    }

    /**
     * Ejecuta una petición GET con reintentos y deserializa a un tipo específico.
     */
    public <T> T get(String uri, String token, Class<T> responseType) {
        return executeGet(uri, token, responseType);
    }

    private <T> T executeGet(String uri, String token, Class<T> responseType) {
        int rateLimitRetries = 0;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                rateLimiter.acquire();

                return restClient.get()
                        .uri(uri)
                        .header("authorization", token)
                        .retrieve()
                        .body(responseType);

            } catch (HttpClientErrorException e) {
                if (handleClientError(e, attempt, rateLimitRetries)) {
                    if (e.getStatusCode().value() == 429) rateLimitRetries++;
                    continue;
                }
                throw e;
            } catch (HttpServerErrorException e) {
                if (attempt >= MAX_RETRIES) throw e;
                long waitMs = baseWaitMs * (long) Math.pow(2, attempt - 1);
                log.warn("DUX - 5xx Error ({}). Retry en {} ms... (intento {}/{})",
                        e.getStatusCode().value(), waitMs, attempt, MAX_RETRIES);
                sleep(waitMs);
            } catch (ResourceAccessException e) {
                if (attempt >= MAX_RETRIES) throw e;
                long waitMs = baseWaitMs * (long) Math.pow(2, attempt - 1);
                log.warn("DUX - Error de conexión. Retry en {} ms... ({}/{}): {}",
                        waitMs, attempt, MAX_RETRIES, e.getMessage());
                sleep(waitMs);
            }
        }
        return null;
    }

    /**
     * Ejecuta una petición POST JSON con reintentos.
     */
    public String postJson(String uri, String token, String jsonBody) {
        int rateLimitRetries = 0;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                rateLimiter.acquire();

                return restClient.post()
                        .uri(uri)
                        .header("authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(jsonBody)
                        .retrieve()
                        .body(String.class);

            } catch (HttpClientErrorException e) {
                if (handleClientError(e, attempt, rateLimitRetries)) {
                    if (e.getStatusCode().value() == 429) rateLimitRetries++;
                    continue;
                }
                throw e;
            } catch (HttpServerErrorException e) {
                if (attempt >= MAX_RETRIES) throw e;
                long waitMs = baseWaitMs * (long) Math.pow(2, attempt - 1);
                log.warn("DUX - 5xx Error ({}). Retry en {} ms... (intento {}/{})",
                        e.getStatusCode().value(), waitMs, attempt, MAX_RETRIES);
                sleep(waitMs);
            } catch (ResourceAccessException e) {
                if (attempt >= MAX_RETRIES) throw e;
                long waitMs = baseWaitMs * (long) Math.pow(2, attempt - 1);
                log.warn("DUX - Error de conexión. Retry en {} ms... ({}/{}): {}",
                        waitMs, attempt, MAX_RETRIES, e.getMessage());
                sleep(waitMs);
            }
        }
        return null;
    }

    /**
     * Maneja errores de cliente (4xx). Retorna true si debe reintentar.
     */
    private boolean handleClientError(HttpClientErrorException e, int attempt, int rateLimitRetries) {
        int status = e.getStatusCode().value();

        // 401 Unauthorized - Token inválido (no hay refresh en DUX)
        if (status == 401) {
            log.error("DUX - 401 Unauthorized - Token inválido o expirado. Verifique dux_tokens.json");
            return false;
        }

        // 409/423 Conflicto - Retry con espera
        if (status == 409 || status == 423) {
            if (attempt >= MAX_RETRIES) return false;
            long waitMs = CONFLICT_BASE_WAIT_MS + ThreadLocalRandom.current().nextInt(500, 1500);
            log.warn("DUX - 409/423 Conflict. Retry en {} ms... (intento {}/{})", waitMs, attempt, MAX_RETRIES);
            sleep(waitMs);
            return true;
        }

        // 429 Too Many Requests
        if (status == 429) {
            if (rateLimitRetries >= MAX_RETRIES_RATE_LIMIT) return false;
            long waitMs = parseRetryAfter(e.getResponseHeaders(), baseWaitMs * 2);
            waitMs = Math.min(waitMs, MAX_WAIT_MS);
            log.warn("DUX - 429 Too Many Requests. Retry en {} segundos... (intento {}/{})",
                    waitMs / 1000, rateLimitRetries + 1, MAX_RETRIES_RATE_LIMIT);
            sleep(waitMs);
            return true;
        }

        return false;
    }

    private long parseRetryAfter(HttpHeaders headers, long defaultMs) {
        if (headers == null) return defaultMs;

        String retryAfter = headers.getFirst("Retry-After");
        if (retryAfter == null) return defaultMs;

        try {
            return Long.parseLong(retryAfter) * 1000;
        } catch (NumberFormatException e) {
            return defaultMs;
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
