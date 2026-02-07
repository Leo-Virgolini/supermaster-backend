package ar.com.leo.super_master_backend.dominio.nube.service;

import ar.com.leo.super_master_backend.dominio.common.exception.ServiceNotConfiguredException;
import ar.com.leo.super_master_backend.dominio.nube.NubeRetryHandler;
import ar.com.leo.super_master_backend.dominio.nube.config.NubeProperties;
import ar.com.leo.super_master_backend.dominio.nube.dto.StockNubeDTO;
import ar.com.leo.super_master_backend.dominio.nube.dto.VentaNubeDTO;
import ar.com.leo.super_master_backend.dominio.nube.model.NubeCredentials;
import ar.com.leo.super_master_backend.dominio.nube.model.NubeCredentials.StoreCredentials;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TiendaNubeService {

    private static final String STORE_HOGAR = "KT HOGAR";
    private static final String STORE_GASTRO = "KT GASTRO";
    private static final long BASE_WAIT_MS = 2000L;

    private final RestClient restClient;
    private final NubeProperties properties;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${app.secrets-dir}")
    private String secretsDir;

    private NubeRetryHandler retryHandler;
    private NubeCredentials credentials;

    public TiendaNubeService(RestClient nubeRestClient, NubeProperties properties, ObjectMapper objectMapper) {
        this.restClient = nubeRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        this.retryHandler = new NubeRetryHandler(
                restClient,
                BASE_WAIT_MS,
                properties.rateLimitPerSecond()
        );
        cargarCredenciales();
    }

    // =====================================================
    // VENTAS
    // =====================================================

    public List<VentaNubeDTO> obtenerVentasHogar() {
        StoreCredentials store = getStore(STORE_HOGAR);
        if (store == null) {
            log.warn("NUBE - Credenciales de {} no disponibles.", STORE_HOGAR);
            return List.of();
        }
        return obtenerVentas(store, STORE_HOGAR);
    }

    public List<VentaNubeDTO> obtenerVentasGastro() {
        StoreCredentials store = getStore(STORE_GASTRO);
        if (store == null) {
            log.warn("NUBE - Credenciales de {} no disponibles.", STORE_GASTRO);
            return List.of();
        }
        return obtenerVentas(store, STORE_GASTRO);
    }

    public List<VentaNubeDTO> obtenerTodasLasVentas() {
        List<VentaNubeDTO> ventas = new ArrayList<>();
        ventas.addAll(obtenerVentasHogar());
        ventas.addAll(obtenerVentasGastro());
        return ventas;
    }

    /**
     * Obtiene todas las ventas pagadas, abiertas y sin empaquetar de una tienda Nube.
     * Filtra client-side por fulfillment_orders con status UNPACKED.
     */
    private List<VentaNubeDTO> obtenerVentas(StoreCredentials store, String label) {
        verificarCredenciales();

        List<VentaNubeDTO> ventas = new ArrayList<>();
        int page = 1;
        final int perPage = 200;
        boolean hasMore = true;

        while (hasMore) {
            String uri = String.format(
                    "/%s/orders?payment_status=paid&shipping_status=unpacked&status=open&aggregates=fulfillment_orders&per_page=%d&page=%d",
                    store.getStoreId(), perPage, page);

            String response;
            try {
                response = retryHandler.get(uri, store.getAccessToken());
            } catch (HttpClientErrorException e) {
                // 404 con "Last page is 0" significa que no hay órdenes
                if (e.getStatusCode().value() == 404 && e.getResponseBodyAsString().contains("Last page is 0")) {
                    break;
                }
                log.warn("NUBE ({}) - Error al obtener órdenes (página {}): {}", label, page, e.getMessage());
                break;
            }

            if (response == null) {
                log.warn("NUBE ({}) - Respuesta nula al obtener órdenes (página {})", label, page);
                break;
            }

            JsonNode ordersArray = objectMapper.readTree(response);
            if (!ordersArray.isArray() || ordersArray.isEmpty()) {
                hasMore = false;
                break;
            }

            for (JsonNode order : ordersArray) {
                long orderId = order.path("id").asLong(0);

                // Filtrar por fulfillment_orders con status UNPACKED
                if (!tieneFulfillmentUnpacked(order)) continue;

                // Omitir órdenes de retiro en local que tengan alguna nota
                if (esPickup(order) && tieneNota(order)) {
                    log.info("NUBE ({}) - Omitida orden pickup con nota: {}", label, orderId);
                    continue;
                }

                JsonNode products = order.path("products");
                if (!products.isArray()) continue;

                for (JsonNode product : products) {
                    String sku = product.path("sku").asString("");
                    double quantity = product.path("quantity").asDouble(0);
                    String productName = product.path("name").asString("");

                    if (quantity <= 0) {
                        log.warn("NUBE ({}) - Producto con cantidad inválida en orden {}: {}", label, orderId, sku);
                        String errorSku = sku.isBlank() ? productName : sku;
                        ventas.add(new VentaNubeDTO("CANT INVALIDA: " + errorSku, quantity, label));
                        continue;
                    }

                    if (sku.isBlank()) {
                        log.warn("NUBE ({}) - Producto sin SKU en orden {}: {}", label, orderId, productName);
                        ventas.add(new VentaNubeDTO("SIN SKU: " + productName, quantity, label));
                        continue;
                    }

                    ventas.add(new VentaNubeDTO(sku, quantity, label));
                }
            }

            if (ordersArray.size() < perPage) {
                hasMore = false;
            } else {
                page++;
            }
        }

        log.info("NUBE ({}) - Ventas obtenidas: {}", label, ventas.size());
        return ventas;
    }

    // =====================================================
    // STOCK
    // =====================================================

    /**
     * Obtiene el stock de un producto por SKU buscando en todas las tiendas.
     *
     * @return StockNubeDTO con el stock encontrado, o null si no se encuentra
     */
    public StockNubeDTO obtenerStockPorSku(String sku) {
        verificarCredenciales();

        for (var entry : credentials.getStores().entrySet()) {
            String storeName = entry.getKey();
            StoreCredentials store = entry.getValue();

            int stock = obtenerStockEnTienda(store, sku);
            if (stock >= 0) {
                return new StockNubeDTO(sku, stock, storeName);
            }
        }
        return null;
    }

    /**
     * Obtiene el stock de un producto por SKU en una tienda específica.
     */
    private int obtenerStockEnTienda(StoreCredentials store, String sku) {
        String uri = String.format("/%s/products/sku/%s",
                store.getStoreId(),
                URLEncoder.encode(sku, StandardCharsets.UTF_8));

        String response;
        try {
            response = retryHandler.get(uri, store.getAccessToken());
        } catch (HttpClientErrorException e) {
            return -1;
        }

        if (response == null) {
            return -1;
        }

        try {
            JsonNode product = objectMapper.readTree(response);
            JsonNode variants = product.path("variants");
            if (variants.isArray()) {
                for (JsonNode variant : variants) {
                    String variantSku = variant.path("sku").asString("");
                    if (sku.equals(variantSku)) {
                        return variant.path("stock").asInt(0);
                    }
                }
            }
            return -1;
        } catch (Exception e) {
            log.warn("NUBE - Error al obtener stock de SKU {}: {}", sku, e.getMessage());
            return -1;
        }
    }

    // =====================================================
    // ÓRDENES
    // =====================================================

    /**
     * Busca una orden por número en todas las tiendas.
     */
    public JsonNode buscarOrdenPorNumero(String numeroOrden) {
        verificarCredenciales();

        for (var entry : credentials.getStores().entrySet()) {
            String storeName = entry.getKey();
            StoreCredentials store = entry.getValue();

            String uri = String.format("/%s/orders?q=%s", store.getStoreId(), numeroOrden);

            try {
                String response = retryHandler.get(uri, store.getAccessToken());
                if (response != null) {
                    JsonNode result = objectMapper.readTree(response);
                    if (result.isArray() && !result.isEmpty()) {
                        log.info("NUBE - Orden {} encontrada en {}", numeroOrden, storeName);
                        return result;
                    }
                }
            } catch (Exception e) {
                log.warn("NUBE - Error buscando orden {} en {}: {}", numeroOrden, storeName, e.getMessage());
            }
        }

        return null;
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private boolean tieneFulfillmentUnpacked(JsonNode order) {
        JsonNode fulfillments = order.path("fulfillments");
        if (!fulfillments.isArray() || fulfillments.isEmpty()) return false;
        for (JsonNode fo : fulfillments) {
            if ("unpacked".equalsIgnoreCase(fo.path("status").asString(""))) {
                return true;
            }
        }
        return false;
    }

    private boolean esPickup(JsonNode order) {
        JsonNode fulfillments = order.path("fulfillments");
        if (!fulfillments.isArray()) return false;
        for (JsonNode fo : fulfillments) {
            if ("pickup".equalsIgnoreCase(fo.path("shipping").path("type").asString(""))) {
                return true;
            }
        }
        return false;
    }

    private boolean tieneNota(JsonNode order) {
        String nota = order.path("owner_note").asString("").trim();
        return !nota.isEmpty();
    }

    // =====================================================
    // CREDENCIALES
    // =====================================================

    private StoreCredentials getStore(String storeName) {
        if (credentials == null || credentials.getStores() == null) return null;
        return credentials.getStores().get(storeName);
    }

    private void verificarCredenciales() {
        if (credentials == null || credentials.getStores() == null || credentials.getStores().isEmpty()) {
            cargarCredenciales();
            if (credentials == null || credentials.getStores() == null || credentials.getStores().isEmpty()) {
                throw new ServiceNotConfiguredException("NUBE",
                        "No hay credenciales disponibles. Verifique el archivo nube_tokens.json");
            }
        }
    }

    private void cargarCredenciales() {
        try {
            File file = java.nio.file.Paths.get(secretsDir).resolve("nube_tokens.json").toFile();
            if (file.exists()) {
                credentials = objectMapper.readValue(file, NubeCredentials.class);
                log.info("NUBE - Credenciales cargadas desde {}. Stores: {}",
                        file.getAbsolutePath(),
                        credentials.getStores() != null ? credentials.getStores().keySet() : "ninguna");
            } else {
                log.warn("NUBE - Archivo de credenciales no encontrado: {}", file.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("NUBE - Error cargando credenciales: {}", e.getMessage());
        }
    }

    public boolean isConfigured() {
        return credentials != null && credentials.getStores() != null && !credentials.getStores().isEmpty();
    }

    public List<String> getStoresDisponibles() {
        if (credentials == null || credentials.getStores() == null) return List.of();
        return List.copyOf(credentials.getStores().keySet());
    }
}
