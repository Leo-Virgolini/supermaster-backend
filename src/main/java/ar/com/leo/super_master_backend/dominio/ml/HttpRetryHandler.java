package ar.com.leo.super_master_backend.dominio.ml;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
public class HttpRetryHandler {

    private static final int MAX_RETRIES = 3;
    private static final int MAX_RETRIES_RATE_LIMIT = 5;
    private static final int MAX_RETRIES_AUTH = 2;
    private static final long MAX_WAIT_MS = 300000; // 5 minutos
    private static final long CONFLICT_BASE_WAIT_MS = 1000; // 1 segundo para conflictos

    private final long baseWaitMs;
    private final RateLimiter rateLimiter;
    private final HttpClient client;
    private final Runnable tokenRefresher;

    /**
     * Constructor con rate limit por defecto (5 req/seg según documentación ML).
     */
    public HttpRetryHandler(HttpClient client, long baseWaitMs, Runnable tokenRefresher) {
        this(client, baseWaitMs, 5.0, tokenRefresher);
    }

    /**
     * Constructor con rate limit configurable.
     */
    public HttpRetryHandler(HttpClient client, long baseWaitMs, double permitsPerSecond, Runnable tokenRefresher) {
        this.client = client;
        this.baseWaitMs = baseWaitMs;
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
        this.tokenRefresher = tokenRefresher;
    }

    public HttpResponse<String> sendWithRetry(Supplier<HttpRequest> requestSupplier) {
        HttpResponse<String> response = null;
        int authRetries = 0;
        int rateLimitRetries = 0;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                rateLimiter.acquire();

                HttpRequest request = requestSupplier.get();
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                // OK
                if (status >= 200 && status < 300) {
                    return response;
                }

                // Token expirado
                if (status == 401) {
                    authRetries++;
                    if (authRetries > MAX_RETRIES_AUTH) {
                        log.error("401 Unauthorized - Máximo de reintentos de autenticación alcanzado ({}). Body: {}",
                                authRetries, truncateBody(response.body()));
                        return response;
                    }
                    log.warn("401 Unauthorized → actualizando tokens... (intento {}/{})", authRetries, MAX_RETRIES_AUTH);
                    if (tokenRefresher != null) {
                        tokenRefresher.run();
                    }
                    continue;
                }

                // Error de concurrencia
                if (status == 409 || status == 423) {
                    if (attempt >= MAX_RETRIES) {
                        log.error("409/423 Conflict - Máximo de reintentos alcanzado ({}). Body: {}",
                                attempt, truncateBody(response.body()));
                        return response;
                    }
                    long waitMs = CONFLICT_BASE_WAIT_MS + ThreadLocalRandom.current().nextInt(200, 800);
                    log.warn("409/423 Conflict. Retry en {} ms... (intento {}/{}). Body: {}",
                            waitMs, attempt, MAX_RETRIES, truncateBody(response.body()));
                    Thread.sleep(waitMs);
                    continue;
                }

                // Too Many Requests
                if (status == 429) {
                    rateLimitRetries++;
                    if (rateLimitRetries > MAX_RETRIES_RATE_LIMIT) {
                        log.error("429 Too Many Requests - Máximo de reintentos alcanzado ({}). Body: {}",
                                rateLimitRetries, truncateBody(response.body()));
                        return response;
                    }
                    long waitMs = parseRetryAfter(response, baseWaitMs);
                    waitMs = Math.min(waitMs, MAX_WAIT_MS);
                    log.warn("429 Too Many Requests. Retry en {} segundos... (intento {}/{})",
                            waitMs / 1000, rateLimitRetries, MAX_RETRIES_RATE_LIMIT);
                    Thread.sleep(waitMs);
                    attempt--;
                    continue;
                }

                // Errores de servidor
                if (status >= 500 && status < 600) {
                    if (attempt >= MAX_RETRIES) {
                        log.error("5xx Error - Máximo de reintentos alcanzado ({}). Body: {}",
                                attempt, truncateBody(response.body()));
                        return response;
                    }
                    long waitMs = baseWaitMs * (long) Math.pow(2, attempt - 1);
                    log.warn("5xx Error ({}). Retry en {} ms... (intento {}/{}). Body: {}",
                            status, waitMs, attempt, MAX_RETRIES, truncateBody(response.body()));
                    Thread.sleep(waitMs);
                    continue;
                }

                // Errores 400-499 no recuperables
                log.error("HTTP {} Error - No recuperable. Body: {}", status, truncateBody(response.body()));
                return response;

            } catch (IOException e) {
                long waitMs = baseWaitMs * (long) Math.pow(2, attempt - 1);
                log.warn("IOException. Retry en {} ms... ({}/{})", waitMs, attempt, MAX_RETRIES);
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return response;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return response;
            }
        }

        return response;
    }

    private long parseRetryAfter(HttpResponse<String> response, long defaultMs) {
        return response.headers().firstValue("Retry-After").map(value -> {
            try {
                return Long.parseLong(value) * 1000;
            } catch (NumberFormatException e) {
                try {
                    long epoch = java.time.ZonedDateTime
                            .parse(value, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
                            .toInstant()
                            .toEpochMilli();
                    return Math.max(epoch - System.currentTimeMillis(), defaultMs);
                } catch (Exception ignored) {
                    return defaultMs;
                }
            }
        }).orElse(defaultMs);
    }

    /**
     * Trunca el body para logging (máximo 500 caracteres).
     */
    private String truncateBody(String body) {
        if (body == null) {
            return "null";
        }
        if (body.length() <= 500) {
            return body;
        }
        return body.substring(0, 500) + "... [truncated]";
    }
}
