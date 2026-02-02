package ar.com.leo.super_master_backend.dominio.dux.service;

import ar.com.leo.super_master_backend.dominio.common.exception.ServiceNotConfiguredException;
import ar.com.leo.super_master_backend.dominio.dux.DuxRetryHandler;
import ar.com.leo.super_master_backend.dominio.dux.config.DuxProperties;
import ar.com.leo.super_master_backend.dominio.dux.dto.ExportDuxResultDTO;
import ar.com.leo.super_master_backend.dominio.dux.dto.ImportDuxResultDTO;
import ar.com.leo.super_master_backend.dominio.dux.model.DuxResponse;
import ar.com.leo.super_master_backend.dominio.dux.model.Item;
import ar.com.leo.super_master_backend.dominio.dux.model.TokensDux;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.proveedor.entity.Proveedor;
import ar.com.leo.super_master_backend.dominio.proveedor.repository.ProveedorRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DuxService {

    private static final int MAX_INTENTOS_VACIOS = 3;
    private static final long BASE_WAIT_MS = 5000L; // DUX es lento, esperar más
    private static final BigDecimal COSTO_MAXIMO = new BigDecimal("99999999.99");

    private final RestClient restClient;
    private final DuxProperties properties;
    private final ObjectMapper objectMapper;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final RecalculoPrecioFacade recalculoPrecioFacade;

    private DuxRetryHandler retryHandler;
    private TokensDux tokens;

    private final Map<String, Proveedor> cacheProveedores = new HashMap<>();

    public DuxService(RestClient duxRestClient, DuxProperties properties, ObjectMapper objectMapper,
                      ProductoRepository productoRepository, ProveedorRepository proveedorRepository,
                      RecalculoPrecioFacade recalculoPrecioFacade) {
        this.restClient = duxRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.recalculoPrecioFacade = recalculoPrecioFacade;
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
     * Los campos UNIDAD MEDIDA, TIPO DE PRODUCTO, FECHA_ULT_COSTO y UNIDADES POR BULTO no
     * están en la respuesta de la API de DUX.
     * Solo permite obtener de maximo 50 productos por request cada 5 segundos.
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
    // IMPORT: DUX → Local
    // =====================================================

    /**
     * Importa productos desde DUX y actualiza los productos locales existentes.
     * Solo actualiza productos que ya existen en la base local (match por SKU).
     * Dispara recálculo de precios para productos con cambios en costo/iva/proveedor.
     */
    @Transactional
    public ImportDuxResultDTO importarProductosDesdeDux() {
        log.info("Iniciando importación de productos desde DUX...");

        List<Item> itemsDux = obtenerProductos();
        int totalDux = itemsDux.size();
        log.info("DUX - {} productos obtenidos para importar", totalDux);

        List<String> skusNoEncontrados = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        List<Integer> productosARecalcular = new ArrayList<>();
        int productosActualizados = 0;

        // Cache de proveedores
        cacheProveedores.clear();
        int proveedoresAntes = (int) proveedorRepository.count();

        for (Item item : itemsDux) {
            try {
                String sku = item.getCodItem();
                if (sku == null || sku.isBlank()) {
                    continue;
                }

                Optional<Producto> productoOpt = productoRepository.findBySku(sku.trim());
                if (productoOpt.isEmpty()) {
                    skusNoEncontrados.add(sku);
                    continue;
                }

                Producto producto = productoOpt.get();
                boolean actualizado = false;

                // Guardar valores anteriores para detectar cambios que afectan precio
                BigDecimal costoAnterior = producto.getCosto();
                BigDecimal ivaAnterior = producto.getIva();
                Integer proveedorIdAnterior = producto.getProveedor() != null ? producto.getProveedor().getId() : null;

                // descripcion ← item.item
                if (item.getItem() != null && !item.getItem().isBlank()) {
                    String desc = item.getItem().trim();
                    producto.setDescripcion(desc.length() > 100 ? desc.substring(0, 100) : desc);
                    actualizado = true;
                }

                // costo ← item.costo
                if (item.getCosto() != null && !item.getCosto().isBlank()) {
                    try {
                        BigDecimal costo = new BigDecimal(item.getCosto().replace(",", "."))
                                .setScale(2, RoundingMode.HALF_UP);
                        if (costo.compareTo(COSTO_MAXIMO) > 0) {
                            errores.add("SKU " + sku + ": COSTO excede límite (" + item.getCosto() + " > 99,999,999.99)");
                        } else if (costo.compareTo(BigDecimal.ZERO) >= 0) {
                            producto.setCosto(costo);
                            actualizado = true;
                        }
                    } catch (NumberFormatException e) {
                        errores.add("SKU " + sku + ": COSTO inválido '" + item.getCosto() + "'");
                    }
                }

                // codExt ← item.codigoExterno
                if (item.getCodigoExterno() != null && !item.getCodigoExterno().isBlank()) {
                    String codExt = item.getCodigoExterno().trim();
                    producto.setCodExt(codExt.length() > 45 ? codExt.substring(0, 45) : codExt);
                    actualizado = true;
                }

                // proveedor ← item.proveedor.proveedor
                if (item.getProveedor() != null && item.getProveedor().getProveedor() != null
                        && !item.getProveedor().getProveedor().isBlank()) {
                    Proveedor proveedor = buscarOCrearProveedor(item.getProveedor().getProveedor().trim());
                    if (proveedor != null) {
                        producto.setProveedor(proveedor);
                        actualizado = true;
                    }
                }

                // iva ← item.porcIva
                if (item.getPorcIva() != null && !item.getPorcIva().isBlank()) {
                    try {
                        BigDecimal iva = new BigDecimal(item.getPorcIva().replace(",", "."));
                        if (iva.compareTo(BigDecimal.ZERO) >= 0 && iva.compareTo(new BigDecimal("100")) <= 0) {
                            producto.setIva(iva);
                            actualizado = true;
                        }
                    } catch (NumberFormatException e) {
                        errores.add("SKU " + sku + ": IVA inválido '" + item.getPorcIva() + "'");
                    }
                }

                // activo ← item.habilitado ("S" → true, otro → false)
                if (item.getHabilitado() != null) {
                    producto.setActivo("S".equalsIgnoreCase(item.getHabilitado().trim()));
                    actualizado = true;
                }

                if (actualizado) {
                    productoRepository.save(producto);
                    productosActualizados++;

                    // Detectar cambios que afectan precios
                    boolean cambioCosto = !bigDecimalEquals(costoAnterior, producto.getCosto());
                    boolean cambioIva = !bigDecimalEquals(ivaAnterior, producto.getIva());
                    Integer proveedorIdNuevo = producto.getProveedor() != null ? producto.getProveedor().getId() : null;
                    boolean cambioProveedor = !Objects.equals(proveedorIdAnterior, proveedorIdNuevo);

                    if (cambioCosto || cambioIva || cambioProveedor) {
                        productosARecalcular.add(producto.getId());
                    }
                }

            } catch (Exception e) {
                String sku = item.getCodItem() != null ? item.getCodItem() : "desconocido";
                errores.add("SKU " + sku + ": Error inesperado - " + e.getMessage());
                log.warn("Error procesando item DUX {}: {}", sku, e.getMessage());
            }
        }

        // Contar proveedores creados
        int proveedoresDespues = (int) proveedorRepository.count();
        int proveedoresCreados = proveedoresDespues - proveedoresAntes;

        // Recalcular precios para productos con cambios relevantes
        if (!productosARecalcular.isEmpty()) {
            log.info("Recalculando precios para {} productos con cambios relevantes...", productosARecalcular.size());
            for (Integer idProducto : productosARecalcular) {
                try {
                    recalculoPrecioFacade.recalcularPorCambioProducto(idProducto);
                } catch (Exception e) {
                    log.warn("Error recalculando precios para producto {}: {}", idProducto, e.getMessage());
                }
            }
        }

        log.info("Importación DUX completada: {} actualizados, {} no encontrados, {} proveedores creados, {} recalculados, {} errores",
                productosActualizados, skusNoEncontrados.size(), proveedoresCreados, productosARecalcular.size(), errores.size());

        return new ImportDuxResultDTO(
                productosActualizados,
                skusNoEncontrados.size(),
                proveedoresCreados,
                totalDux,
                skusNoEncontrados,
                errores
        );
    }

    // =====================================================
    // EXPORT: Local → DUX
    // =====================================================

    /**
     * Exporta productos locales a DUX usando el endpoint nuevoItem.
     *
     * @param skus lista de SKUs a exportar, o null para exportar todos
     * @return resultado con cantidad enviada e ID de proceso
     */
    public ExportDuxResultDTO exportarProductosADux(List<String> skus) {
        log.info("Iniciando exportación de productos a DUX...");

        List<String> errores = new ArrayList<>();

        List<Producto> productos;
        if (skus != null && !skus.isEmpty()) {
            productos = new ArrayList<>();
            for (String sku : skus) {
                productoRepository.findBySku(sku.trim())
                        .ifPresentOrElse(
                                productos::add,
                                () -> errores.add("SKU no encontrado: " + sku)
                        );
            }
        } else {
            productos = productoRepository.findAll();
        }

        if (productos.isEmpty()) {
            log.warn("DUX Export - No hay productos para exportar");
            return new ExportDuxResultDTO(0, 0, errores);
        }

        // Mapear productos al formato DUX
        List<Map<String, Object>> productosJson = new ArrayList<>();
        for (Producto producto : productos) {
            try {
                String tipo = Boolean.TRUE.equals(producto.getEsCombo()) ? "COMBO" : "SIMPLE";

                Map<String, Object> itemDux = new HashMap<>();
                itemDux.put("cod_item", producto.getSku());
                itemDux.put("item", producto.getDescripcion() != null ? producto.getDescripcion() : "");
                itemDux.put("tipo_producto", tipo);
                itemDux.put("habilitado", Boolean.TRUE.equals(producto.getActivo()) ? "S" : "N");

                if (producto.getCosto() != null) {
                    itemDux.put("costo", producto.getCosto().doubleValue());
                }
                if (producto.getCodExt() != null) {
                    itemDux.put("codigo_externo", producto.getCodExt());
                }
                if (producto.getIva() != null) {
                    itemDux.put("porc_iva", producto.getIva().doubleValue());
                }
                if (producto.getUxb() != null) {
                    itemDux.put("ctd_unidades_por_bulto", producto.getUxb());
                }

                productosJson.add(itemDux);
            } catch (Exception e) {
                errores.add("SKU " + producto.getSku() + ": Error mapeando - " + e.getMessage());
                log.warn("Error mapeando producto {} para DUX: {}", producto.getSku(), e.getMessage());
            }
        }

        if (productosJson.isEmpty()) {
            log.warn("DUX Export - No hay productos válidos para enviar");
            return new ExportDuxResultDTO(0, 0, errores);
        }

        // Enviar a DUX
        verificarTokens();

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(Map.of("productos", productosJson));
        } catch (Exception e) {
            log.error("DUX Export - Error serializando productos", e);
            errores.add("Error preparando datos para DUX: " + e.getMessage());
            return new ExportDuxResultDTO(0, 0, errores);
        }

        String response = retryHandler.postJson("/item/nuevoItem", tokens.token, jsonBody);

        int idProceso = 0;
        if (response != null) {
            Pattern pattern = Pattern.compile("ID de proceso:\\s*(\\d+)");
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                idProceso = Integer.parseInt(matcher.group(1));
                log.info("DUX Export - Proceso iniciado con ID: {}", idProceso);
            } else {
                log.warn("DUX Export - No se encontró ID de proceso en respuesta: {}", response);
            }
        } else {
            errores.add("No se recibió respuesta de DUX");
        }

        log.info("DUX Export - {} productos enviados, proceso ID: {}, {} errores",
                productosJson.size(), idProceso, errores.size());

        return new ExportDuxResultDTO(productosJson.size(), idProceso, errores);
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private Proveedor buscarOCrearProveedor(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String nombreNormalizado = nombre.trim();
        String key = nombreNormalizado.toUpperCase();
        return cacheProveedores.computeIfAbsent(key, k ->
                proveedorRepository.findByProveedorIgnoreCase(nombreNormalizado)
                        .orElseGet(() -> {
                            Proveedor nuevo = new Proveedor();
                            nuevo.setProveedor(nombreNormalizado);
                            String apodo = nombreNormalizado.length() > 50
                                    ? nombreNormalizado.substring(0, 50)
                                    : nombreNormalizado;
                            nuevo.setApodo(apodo);
                            return proveedorRepository.save(nuevo);
                        })
        );
    }

    private boolean bigDecimalEquals(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
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
    public record ProductoPrecioData(String tipo, double precio) {
    }
}
