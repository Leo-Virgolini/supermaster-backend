package ar.com.leo.super_master_backend.dominio.dux.service;

import ar.com.leo.super_master_backend.dominio.common.exception.ServiceNotConfiguredException;
import ar.com.leo.super_master_backend.dominio.dux.DuxRetryHandler;
import ar.com.leo.super_master_backend.dominio.dux.config.DuxProperties;
import ar.com.leo.super_master_backend.dominio.dux.model.DuxResponse;
import ar.com.leo.super_master_backend.dominio.dux.model.Item;
import ar.com.leo.super_master_backend.dominio.dux.model.TokensDux;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DuxService {

    private static final int MAX_INTENTOS_VACIOS = 3;
    private static final long BASE_WAIT_MS = 5000L; // DUX es lento, esperar más

    private final RestClient restClient;
    private final DuxProperties properties;
    private final ObjectMapper objectMapper;

    private DuxRetryHandler retryHandler;
    private TokensDux tokens;

    public DuxService(RestClient duxRestClient, DuxProperties properties, ObjectMapper objectMapper) {
        this.restClient = duxRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        this.retryHandler = new DuxRetryHandler(
                restClient,
                BASE_WAIT_MS,
                properties.rateLimitPerSecond()
        );
        cargarTokens();
    }

    // =====================================================
    // LISTAS DE PRECIOS
    // =====================================================

    /**
     * Obtiene todas las listas de precios de venta.
     */
    public JsonNode obtenerListasPrecios() {
        verificarTokens();

        String response = retryHandler.get("/listaprecioventa", tokens.token);
        return objectMapper.readTree(response);
    }

    /**
     * Obtiene el ID de una lista de precios por su nombre.
     */
    public long obtenerIdListaPrecio(String nombreLista) {
        verificarTokens();

        String response = retryHandler.get("/listaprecioventa", tokens.token);
        JsonNode root = objectMapper.readTree(response);

        if (!root.isArray()) {
            throw new IllegalStateException("La respuesta no es un array de listas de precios");
        }

        for (JsonNode node : root) {
            String nombre = node.get("lista_precio_venta").asString("");
            if (nombre.equalsIgnoreCase(nombreLista)) {
                return node.get("id_lista_precio_venta").asLong();
            }
        }

        throw new IllegalArgumentException("No se encontró la lista de precios: " + nombreLista);
    }

    /**
     * Modifica precios de productos en una lista de precios.
     *
     * <p><b>IMPORTANTE:</b> Es obligatorio enviar el tipo de producto (SIMPLE o COMBO) junto con el precio.
     * Si no se envía el tipo correcto, DUX desconfigura el producto y puede perder su configuración
     * de componentes (en caso de combos) u otras propiedades.</p>
     *
     * @param productos     Mapa de SKU -> datos del producto (tipo, precio)
     * @param idListaPrecio ID de la lista de precios en DUX
     * @return ID del proceso de importación, o 0 si falló
     */
    public int modificarListaPrecios(Map<String, ProductoPrecioData> productos, long idListaPrecio) {
        verificarTokens();

        List<Map<String, Object>> productosJson = new ArrayList<>();

        for (Map.Entry<String, ProductoPrecioData> entry : productos.entrySet()) {
            String sku = entry.getKey();
            ProductoPrecioData data = entry.getValue();

            if (sku != null && data.precio() > 0 &&
                    ("SIMPLE".equals(data.tipo()) || "COMBO".equals(data.tipo()))) {

                Map<String, Object> producto = Map.of(
                        "cod_item", sku,
                        "tipo_producto", data.tipo(),
                        "precios", List.of(Map.of(
                                "importe", data.precio(),
                                "id_lista_precio_venta", idListaPrecio,
                                "id_moneda", 1 // ARS
                        ))
                );
                productosJson.add(producto);
            } else {
                log.warn("DUX - Producto inválido, SKU: {}, Tipo: {}, Precio: {}",
                        sku, data.tipo(), data.precio());
            }
        }

        if (productosJson.isEmpty()) {
            log.warn("DUX - No hay productos válidos para modificar");
            return 0;
        }

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(Map.of("productos", productosJson));
        } catch (Exception e) {
            log.error("DUX - Error serializando productos", e);
            throw new RuntimeException("Error preparando datos para DUX", e);
        }

        String response = retryHandler.postJson("/item/nuevoItem", tokens.token, jsonBody);

        if (response == null) {
            log.warn("DUX - No se recibió respuesta al modificar lista de precios");
            return 0;
        }

        Pattern pattern = Pattern.compile("ID de proceso:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            int idProceso = Integer.parseInt(matcher.group(1));
            log.info("DUX - Proceso iniciado con ID: {}", idProceso);
            return idProceso;
        }

        log.warn("DUX - No se encontró ID de proceso en respuesta: {}", response);
        return 0;
    }

    /**
     * Obtiene el estado de un proceso de importación.
     */
    public String obtenerEstadoProceso(int idProceso) {
        verificarTokens();
        return retryHandler.get("/obtenerEstadoItem?idProceso=" + idProceso, tokens.token);
    }

    // =====================================================
    // PRODUCTOS
    // =====================================================

    /**
     * Obtiene todos los productos de DUX con paginación automática.
     */
    public List<Item> obtenerProductos() {
        verificarTokens();

        List<Item> allItems = new ArrayList<>();
        int offset = 0;
        int total = Integer.MAX_VALUE;
        int limit = properties.itemsPerPage();
        int intentosVacios = 0;

        while (offset < total) {
            String response = retryHandler.get(
                    "/items?offset=" + offset + "&limit=" + limit,
                    tokens.token
            );

            if (response == null) {
                log.error("DUX - Error obteniendo productos en offset {}", offset);
                break;
            }

            DuxResponse duxResponse;
            try {
                duxResponse = objectMapper.readValue(response, DuxResponse.class);
            } catch (Exception e) {
                log.error("DUX - Error parseando respuesta", e);
                break;
            }

            // Actualizar total
            if (duxResponse.getPaging() != null) {
                int nuevoTotal = duxResponse.getPaging().getTotal();
                if (total == Integer.MAX_VALUE || nuevoTotal != total) {
                    total = nuevoTotal;
                    log.info("DUX - Total de productos: {}", total);
                }
            }

            // Verificar resultados
            if (duxResponse.getResults() == null || duxResponse.getResults().isEmpty()) {
                if (offset >= total) {
                    log.info("DUX - Fin de paginación (offset >= total)");
                    break;
                }

                intentosVacios++;
                log.warn("DUX - Respuesta vacía en offset {} (intento {}/{})",
                        offset, intentosVacios, MAX_INTENTOS_VACIOS);

                if (intentosVacios >= MAX_INTENTOS_VACIOS) {
                    log.warn("DUX - Terminando después de {} intentos vacíos. Obtenidos: {}/{}",
                            MAX_INTENTOS_VACIOS, allItems.size(), total);
                    break;
                }

                offset += limit;
                continue;
            }

            intentosVacios = 0;
            allItems.addAll(duxResponse.getResults());

            log.info("DUX - Obtenidos: {}/{} (offset: {})", allItems.size(), total, offset);

            offset += limit;

            if (allItems.size() >= total) {
                log.info("DUX - Todos los productos obtenidos");
                break;
            }
        }

        log.info("DUX - Descarga completa: {} productos", allItems.size());
        return allItems;
    }

    /**
     * Obtiene un producto por su código.
     */
    public Item obtenerProductoPorCodigo(String codItem) {
        verificarTokens();

        String response = retryHandler.get("/items?cod_item=" + codItem, tokens.token);

        if (response == null) {
            return null;
        }

        try {
            DuxResponse duxResponse = objectMapper.readValue(response, DuxResponse.class);
            if (duxResponse.getResults() != null && !duxResponse.getResults().isEmpty()) {
                return duxResponse.getResults().getFirst();
            }
        } catch (Exception e) {
            log.error("DUX - Error obteniendo producto {}", codItem, e);
        }

        return null;
    }

    // =====================================================
    // TOKENS
    // =====================================================

    private void verificarTokens() {
        if (tokens == null) {
            cargarTokens();
            if (tokens == null) {
                throw new ServiceNotConfiguredException("DUX",
                        "No hay tokens disponibles. Verifique el archivo dux_tokens.json");
            }
        }
    }

    private void cargarTokens() {
        try {
            File file = properties.getTokenFile().toFile();
            if (file.exists()) {
                tokens = objectMapper.readValue(file, TokensDux.class);
                log.info("DUX - Tokens cargados desde {}", file.getAbsolutePath());
            } else {
                log.warn("DUX - Archivo de tokens no encontrado: {}", file.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("DUX - Error cargando tokens: {}", e.getMessage());
        }
    }

    /**
     * Verifica si el servicio está configurado.
     */
    public boolean isConfigured() {
        return tokens != null;
    }

    // =====================================================
    // DTOs INTERNOS
    // =====================================================

    /**
     * Datos de precio para actualizar en DUX.
     *
     * @param tipo   Tipo de producto: "SIMPLE" o "COMBO". <b>OBLIGATORIO</b> - si no se envía
     *               correctamente, DUX desconfigura el producto y puede perder componentes
     * @param precio Precio del producto
     */
    public record ProductoPrecioData(String tipo, double precio) {}
}
