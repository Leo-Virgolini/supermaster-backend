package ar.com.leo.super_master_backend.dominio.nube;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Handler de reintentos para Tiendanube con soporte para:
 * - Rate limiting
 * - Reintentos con backoff exponencial
 * - Manejo de errores 5xx y errores de conexión
 *
 * <p>Los reintentos de 429 NO consumen intentos normales (5xx/conexión).</p>
 */
@Slf4j
public class NubeRetryHandler {

    private static final int MAX_RETRIES = 3;
    private static final int MAX_RETRIES_RATE_LIMIT = 5;
    private static final long MAX_WAIT_MS = 300000; // 5 minutos

    private final RestClient restClient;
    private final long baseWaitMs;
    private final RateLimiter rateLimiter;

    public NubeRetryHandler(RestClient restClient, long baseWaitMs, double permitsPerSecond) {
        this.restClient = restClient;
        this.baseWaitMs = baseWaitMs;
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
    }

    public record HttpResponse(String body, HttpHeaders headers) {}

    /**
     * Ejecuta una petición GET con reintentos, devolviendo body y headers.
     */
    public HttpResponse getWithHeaders(String uri, String accessToken) {
        int normalRetries = 0;
        int rateLimitRetries = 0;

        while (true) {
            try {
                rateLimiter.acquire();

                ResponseEntity<String> entity = restClient.get()
                        .uri(uri)
                        .header("Authentication", "bearer " + accessToken)
                        .retrieve()
                        .toEntity(String.class);
                return new HttpResponse(entity.getBody(), entity.getHeaders());

            } catch (HttpClientErrorException e) {
                int status = e.getStatusCode().value();

                if (status == 401) {
                    log.error("NUBE - 401 Unauthorized - Token inválido. Verifique nube_tokens.json");
                    throw e;
                }

                if (status == 404) {
                    throw e;
                }

                if (status == 429) {
                    if (rateLimitRetries >= MAX_RETRIES_RATE_LIMIT) {
                        log.error("NUBE - 429 Too Many Requests - Máximo de reintentos alcanzado");
                        throw e;
                    }
                    rateLimitRetries++;
                    long waitMs = Math.min(parseRetryAfter(e.getResponseHeaders(), baseWaitMs * 2), MAX_WAIT_MS);
                    log.warn("NUBE - 429 Too Many Requests. Retry en {} segundos... (intento {}/{})",
                            waitMs / 1000, rateLimitRetries, MAX_RETRIES_RATE_LIMIT);
                    sleep(waitMs);
                    continue;
                }

                throw e;

            } catch (HttpServerErrorException e) {
                normalRetries++;
                if (normalRetries >= MAX_RETRIES) throw e;
                long waitMs = baseWaitMs * (long) Math.pow(2, normalRetries - 1);
                log.warn("NUBE - 5xx Error ({}). Retry en {} ms... (intento {}/{})",
                        e.getStatusCode().value(), waitMs, normalRetries, MAX_RETRIES);
                sleep(waitMs);

            } catch (ResourceAccessException e) {
                normalRetries++;
                if (normalRetries >= MAX_RETRIES) throw e;
                long waitMs = baseWaitMs * (long) Math.pow(2, normalRetries - 1);
                log.warn("NUBE - Error de conexión. Retry en {} ms... ({}/{}): {}",
                        waitMs, normalRetries, MAX_RETRIES, e.getMessage());
                sleep(waitMs);
            }
        }
    }

    /**
     * Ejecuta una petición GET con reintentos, devolviendo solo el body.
     */
    public String get(String uri, String accessToken) {
        return getWithHeaders(uri, accessToken).body();
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
