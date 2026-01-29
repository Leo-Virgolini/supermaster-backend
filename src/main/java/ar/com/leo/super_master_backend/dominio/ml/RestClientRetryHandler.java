package ar.com.leo.super_master_backend.dominio.ml;

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
 * Handler de reintentos para RestClient con soporte para:
 * - Rate limiting (Guava RateLimiter)
 * - Reintentos con backoff exponencial
 * - Renovación automática de tokens en 401
 * - Manejo de errores 429, 5xx, conflictos
 */
@Slf4j
public class RestClientRetryHandler {

    private static final int MAX_RETRIES = 3;
    private static final int MAX_RETRIES_RATE_LIMIT = 5;
    private static final int MAX_RETRIES_AUTH = 2;
    private static final long MAX_WAIT_MS = 300000; // 5 minutos
    private static final long CONFLICT_BASE_WAIT_MS = 1000;

    private final RestClient restClient;
    private final long baseWaitMs;
    private final RateLimiter rateLimiter;
    private final Runnable tokenRefresher;

    public RestClientRetryHandler(RestClient restClient, long baseWaitMs, double permitsPerSecond, Runnable tokenRefresher) {
        this.restClient = restClient;
        this.baseWaitMs = baseWaitMs;
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
        this.tokenRefresher = tokenRefresher;
    }

    /**
     * Ejecuta una petición GET con reintentos y retorna el body como String.
     */
    public String get(String uri, String authToken) {
        return executeGet(uri, authToken, String.class);
    }

    /**
     * Ejecuta una petición GET con reintentos y deserializa a un tipo específico.
     */
    public <T> T get(String uri, String authToken, Class<T> responseType) {
        return executeGet(uri, authToken, responseType);
    }

    private <T> T executeGet(String uri, String authToken, Class<T> responseType) {
        int authRetries = 0;
        int rateLimitRetries = 0;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                rateLimiter.acquire();

                return restClient.get()
                        .uri(uri)
                        .header("Authorization", "Bearer " + authToken)
                        .retrieve()
                        .body(responseType);

            } catch (HttpClientErrorException e) {
                if (handleClientError(e, attempt, authRetries, rateLimitRetries)) {
                    if (e.getStatusCode().value() == 401) authRetries++;
                    if (e.getStatusCode().value() == 429) rateLimitRetries++;
                    continue;
                }
                throw e;
            } catch (HttpServerErrorException e) {
                if (attempt >= MAX_RETRIES) throw e;
                long waitMs = baseWaitMs * (long) Math.pow(2, attempt - 1);
                log.warn("5xx Error ({}). Retry en {} ms... (intento {}/{})", e.getStatusCode().value(), waitMs, attempt, MAX_RETRIES);
                sleep(waitMs);
            } catch (ResourceAccessException e) {
                if (attempt >= MAX_RETRIES) throw e;
                long waitMs = baseWaitMs * (long) Math.pow(2, attempt - 1);
                log.warn("Error de conexión. Retry en {} ms... ({}/{})", waitMs, attempt, MAX_RETRIES);
                sleep(waitMs);
            }
        }
        return null;
    }

    /**
     * Ejecuta una petición POST form-urlencoded con reintentos.
     */
    public String postForm(String uri, String formBody) {
        int authRetries = 0;
        int rateLimitRetries = 0;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                rateLimiter.acquire();

                return restClient.post()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(formBody)
                        .retrieve()
                        .body(String.class);

            } catch (HttpClientErrorException e) {
                if (handleClientError(e, attempt, authRetries, rateLimitRetries)) {
                    if (e.getStatusCode().value() == 401) authRetries++;
                    if (e.getStatusCode().value() == 429) rateLimitRetries++;
                    continue;
                }
                throw e;
            } catch (HttpServerErrorException e) {
                if (attempt >= MAX_RETRIES) throw e;
                long waitMs = baseWaitMs * (long) Math.pow(2, attempt - 1);
                log.warn("5xx Error ({}). Retry en {} ms... (intento {}/{})", e.getStatusCode().value(), waitMs, attempt, MAX_RETRIES);
                sleep(waitMs);
            } catch (ResourceAccessException e) {
                if (attempt >= MAX_RETRIES) throw e;
                long waitMs = baseWaitMs * (long) Math.pow(2, attempt - 1);
                log.warn("Error de conexión. Retry en {} ms... ({}/{})", waitMs, attempt, MAX_RETRIES);
                sleep(waitMs);
            }
        }
        return null;
    }

    /**
     * Maneja errores de cliente (4xx). Retorna true si debe reintentar.
     */
    private boolean handleClientError(HttpClientErrorException e, int attempt, int authRetries, int rateLimitRetries) {
        int status = e.getStatusCode().value();

        if (status == 401) {
            if (authRetries >= MAX_RETRIES_AUTH) {
                log.error("401 Unauthorized - Máximo de reintentos de autenticación alcanzado");
                return false;
            }
            log.warn("401 Unauthorized → actualizando tokens... (intento {}/{})", authRetries + 1, MAX_RETRIES_AUTH);
            if (tokenRefresher != null) tokenRefresher.run();
            return true;
        }

        if (status == 409 || status == 423) {
            if (attempt >= MAX_RETRIES) return false;
            long waitMs = CONFLICT_BASE_WAIT_MS + ThreadLocalRandom.current().nextInt(200, 800);
            log.warn("409/423 Conflict. Retry en {} ms... (intento {}/{})", waitMs, attempt, MAX_RETRIES);
            sleep(waitMs);
            return true;
        }

        if (status == 429) {
            if (rateLimitRetries >= MAX_RETRIES_RATE_LIMIT) return false;
            long waitMs = parseRetryAfter(e.getResponseHeaders(), baseWaitMs);
            waitMs = Math.min(waitMs, MAX_WAIT_MS);
            log.warn("429 Too Many Requests. Retry en {} segundos... (intento {}/{})", waitMs / 1000, rateLimitRetries + 1, MAX_RETRIES_RATE_LIMIT);
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
            try {
                long epoch = java.time.ZonedDateTime
                        .parse(retryAfter, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant()
                        .toEpochMilli();
                return Math.max(epoch - System.currentTimeMillis(), defaultMs);
            } catch (Exception ignored) {
                return defaultMs;
            }
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
