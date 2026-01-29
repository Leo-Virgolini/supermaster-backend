package ar.com.leo.super_master_backend.dominio.ml.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.ServiceNotConfiguredException;
import ar.com.leo.super_master_backend.dominio.ml.RestClientRetryHandler;
import ar.com.leo.super_master_backend.dominio.ml.config.MercadoLibreProperties;
import ar.com.leo.super_master_backend.dominio.ml.dto.CostoEnvioMasivoResponseDTO;
import ar.com.leo.super_master_backend.dominio.ml.dto.CostoEnvioResponseDTO;
import ar.com.leo.super_master_backend.dominio.ml.dto.CostoVentaMasivoResponseDTO;
import ar.com.leo.super_master_backend.dominio.ml.dto.CostoVentaResponseDTO;
import ar.com.leo.super_master_backend.dominio.ml.dto.ProcesoMasivoEstadoDTO;
import ar.com.leo.super_master_backend.dominio.ml.entity.ConfiguracionMl;
import ar.com.leo.super_master_backend.dominio.ml.model.MLCredentials;
import ar.com.leo.super_master_backend.dominio.ml.model.Producto;
import ar.com.leo.super_master_backend.dominio.ml.model.TokensML;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.CalculoPrecioService;
import ar.com.leo.super_master_backend.dominio.producto.calculo.service.RecalculoPrecioFacade;
import ar.com.leo.super_master_backend.dominio.producto.mla.entity.Mla;
import ar.com.leo.super_master_backend.dominio.producto.mla.repository.MlaRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class MercadoLibreService {

    private static final String CANAL_ML = "ML";
    private static final BigDecimal IVA_DIVISOR = new BigDecimal("1.21");

    private final ObjectMapper objectMapper;
    private final MlaRepository mlaRepository;
    private final ProductoRepository productoRepository;
    private final CanalRepository canalRepository;
    private final ConfiguracionMlService configuracionMlService;
    private final CalculoPrecioService calculoPrecioService;
    private final RecalculoPrecioFacade recalculoPrecioFacade;
    private final RestClient restClient;
    private final MercadoLibreProperties properties;
    private final Object tokenLock = new Object();

    // Control de ejecución masiva - Costo de Envío
    private final AtomicBoolean cancelarProcesoMasivo = new AtomicBoolean(false);
    private final AtomicBoolean procesoMasivoEnEjecucion = new AtomicBoolean(false);

    // Estado y resultados del proceso masivo - Costo de Envío
    private volatile ProcesoMasivoEstadoDTO estadoProcesoMasivo = ProcesoMasivoEstadoDTO.idle();
    private volatile CostoEnvioMasivoResponseDTO resultadoProcesoMasivo = null;

    // Control de ejecución masiva - Costo de Venta
    private final AtomicBoolean cancelarProcesoMasivoCostoVenta = new AtomicBoolean(false);
    private final AtomicBoolean procesoMasivoCostoVentaEnEjecucion = new AtomicBoolean(false);

    // Estado y resultados del proceso masivo - Costo de Venta
    private volatile ProcesoMasivoEstadoDTO estadoProcesoMasivoCostoVenta = ProcesoMasivoEstadoDTO.idle();
    private volatile CostoVentaMasivoResponseDTO resultadoProcesoMasivoCostoVenta = null;

    // Auto-inyección para que las llamadas internas pasen por el proxy de Spring
    // y respeten @Transactional
    @Lazy
    @Autowired
    private MercadoLibreService self;

    private RestClientRetryHandler retryHandler;
    private MLCredentials credentials;
    private TokensML tokens;
    private String cachedUserId;

    public MercadoLibreService(ObjectMapper objectMapper,
                               MlaRepository mlaRepository,
                               ProductoRepository productoRepository,
                               CanalRepository canalRepository,
                               ConfiguracionMlService configuracionMlService,
                               CalculoPrecioService calculoPrecioService,
                               RecalculoPrecioFacade recalculoPrecioFacade,
                               RestClient mercadoLibreRestClient,
                               MercadoLibreProperties properties) {
        this.objectMapper = objectMapper;
        this.mlaRepository = mlaRepository;
        this.productoRepository = productoRepository;
        this.canalRepository = canalRepository;
        this.configuracionMlService = configuracionMlService;
        this.calculoPrecioService = calculoPrecioService;
        this.recalculoPrecioFacade = recalculoPrecioFacade;
        this.restClient = mercadoLibreRestClient;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        this.retryHandler = new RestClientRetryHandler(
                restClient,
                properties.retryBaseWaitMs(),
                properties.rateLimitPerSecond(),
                this::verificarTokens
        );
        cargarCredentials();
        cargarTokens();
    }

    /**
     * Calcula el costo de envío para el vendedor de un producto de ML.
     *
     * - PVP >= umbral ($33,000): Consulta API ML para obtener costo real de envío gratis
     * - PVP < umbral: Usa tiers fijos:
     *   - PVP < $15,000 → $1,115
     *   - $15,000 <= PVP < $25,000 → $2,300
     *   - $25,000 <= PVP < $33,000 → $2,810
     *
     * El cálculo es iterativo: al agregar el costo de envío, el PVP puede cambiar
     * de tier, requiriendo recalcular hasta estabilizar.
     */
    @Transactional
    public CostoEnvioResponseDTO calcularCostoEnvioGratis(String mlaCode) {
        final int MAX_ITERACIONES = 10;

        // Obtener configuración
        ConfiguracionMl config = configuracionMlService.obtenerEntidad();
        BigDecimal umbralEnvioGratis = config.getUmbralEnvioGratis();

        // Buscar el MLA y su producto asociado
        Optional<Mla> mlaOpt = mlaRepository.findByMla(mlaCode);
        if (mlaOpt.isEmpty()) {
            log.warn("ML - MLA {} no encontrado en la base de datos", mlaCode);
            return new CostoEnvioResponseDTO(mlaCode, null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                    "MLA no encontrado en la base de datos");
        }

        Mla mla = mlaOpt.get();
        ar.com.leo.super_master_backend.dominio.producto.entity.Producto productoDb =
                mla.getProductos().stream().findFirst().orElse(null);

        if (productoDb == null) {
            log.warn("ML - MLA {} no tiene producto asociado", mlaCode);
            return new CostoEnvioResponseDTO(mlaCode, null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                    "MLA sin producto asociado");
        }

        // Si no tiene comisionPorcentaje, obtenerla primero (es necesaria para calcular el PVP correctamente)
        if (mla.getComisionPorcentaje() == null) {
            log.info("ML - MLA {} no tiene comisión, obteniendo primero costo de venta...", mlaCode);
            CostoVentaResponseDTO costoVentaResult = obtenerCostoVenta(mlaCode);
            if (costoVentaResult.porcentajeTotal() != null && costoVentaResult.porcentajeTotal().compareTo(BigDecimal.ZERO) > 0) {
                log.info("ML - Comisión obtenida para MLA {}: {}%", mlaCode, costoVentaResult.porcentajeTotal());
                // Recargar el MLA para tener la comisión actualizada
                mla = mlaRepository.findByMla(mlaCode).orElse(mla);
            } else {
                log.warn("ML - No se pudo obtener la comisión para MLA {}: {}", mlaCode, costoVentaResult.mensaje());
            }
        }

        // Buscar el canal ML
        Canal canalMl = canalRepository.findByCanalIgnoreCase(CANAL_ML)
                .orElseThrow(() -> new IllegalStateException("No se encontró el canal " + CANAL_ML));

        // Variables para API ML (se inicializan solo si es necesario)
        Producto productoMl = null;
        String userId = null;
        String status = null;

        // CÁLCULO ITERATIVO
        // costoEnvioActual: valor SIN IVA que se pasa al cálculo de precio
        // costoEnvioConIvaActual: valor CON IVA para comparar con API/tiers
        BigDecimal costoEnvioActual = BigDecimal.ZERO;
        BigDecimal costoEnvioConIvaActual = BigDecimal.ZERO;
        BigDecimal pvpActual = BigDecimal.ZERO;
        int iteracion = 0;
        String tipoCalculo = "";

        while (iteracion < MAX_ITERACIONES) {
            iteracion++;

            // Calcular PVP con el costo de envío actual
            PrecioCalculadoDTO precioCalculado;
            try {
                precioCalculado = calculoPrecioService.calcularPrecioCanalConEnvio(
                        productoDb.getId(), canalMl.getId(), 0, costoEnvioActual);
            } catch (Exception e) {
                log.warn("ML - MLA {} - Error calculando PVP en iteración {}: {}",
                        mlaCode, iteracion, e.getMessage());
                return new CostoEnvioResponseDTO(mlaCode, status, pvpActual, BigDecimal.ZERO, BigDecimal.ZERO,
                        "Error calculando PVP: " + e.getMessage());
            }

            pvpActual = precioCalculado.pvp();

            // Determinar costo de envío según el PVP
            BigDecimal nuevoCostoEnvio;

            if (pvpActual.compareTo(umbralEnvioGratis) >= 0) {
                // PVP >= umbral: consultar API ML
                tipoCalculo = "API ML";

                // Inicializar conexión con ML si es la primera vez
                if (productoMl == null) {
                    verificarTokens();
                    productoMl = getItemByMLA(mlaCode);
                    if (productoMl == null) {
                        log.warn("ML - No se pudo obtener el producto con MLA: {}", mlaCode);
                        return new CostoEnvioResponseDTO(mlaCode, null, pvpActual, BigDecimal.ZERO, BigDecimal.ZERO,
                                "No se pudo obtener el producto de MercadoLibre");
                    }
                    status = productoMl.status;
                    if (!"active".equals(status)) {
                        log.warn("ML - El producto id: {} se encuentra en estado: '{}'", productoMl.id, status);
                    }
                    try {
                        userId = getUserId();
                    } catch (IOException e) {
                        log.error("Error al obtener userId de ML", e);
                        return new CostoEnvioResponseDTO(mlaCode, status, pvpActual, BigDecimal.ZERO, BigDecimal.ZERO,
                                "Error al obtener userId de MercadoLibre");
                    }
                }

                nuevoCostoEnvio = calcularCostoEnvioInterno(userId, productoMl, pvpActual);
            } else {
                // PVP < umbral: usar tiers fijos
                nuevoCostoEnvio = configuracionMlService.obtenerCostoEnvioPorPvp(pvpActual);
                if (nuevoCostoEnvio == null) {
                    log.warn("ML - MLA {} - Tiers no configurados", mlaCode);
                    return new CostoEnvioResponseDTO(mlaCode, status, pvpActual, BigDecimal.ZERO, BigDecimal.ZERO,
                            "Tiers de costo de envío no configurados");
                }
                tipoCalculo = "Tier";
            }

            log.info("ML - MLA {} - Iteración {}: costoEnvioSinIva=${}, PVP=${}, nuevoCostoEnvioConIva=${} ({})",
                    mlaCode, iteracion, costoEnvioActual, pvpActual, nuevoCostoEnvio, tipoCalculo);

            // Verificar si se estabilizó (comparar valores CON IVA)
            if (nuevoCostoEnvio.compareTo(costoEnvioConIvaActual) == 0) {
                log.info("ML - MLA {} - Estabilizado en iteración {}: PVP=${}, costoEnvioConIva=${}, costoEnvioSinIva=${} ({})",
                        mlaCode, iteracion, pvpActual, nuevoCostoEnvio, costoEnvioActual, tipoCalculo);
                break;
            }

            // Guardar el valor CON IVA para comparación
            costoEnvioConIvaActual = nuevoCostoEnvio;
            // Convertir a SIN IVA para el cálculo de precio
            costoEnvioActual = nuevoCostoEnvio.compareTo(BigDecimal.ZERO) > 0
                    ? nuevoCostoEnvio.divide(IVA_DIVISOR, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }

        // Los valores finales
        BigDecimal costoEnvioConIva = costoEnvioConIvaActual;
        BigDecimal costoEnvioSinIva = costoEnvioActual;

        // Guardar resultado (se guarda el costo SIN IVA)
        String mensaje;
        if (costoEnvioSinIva.compareTo(BigDecimal.ZERO) > 0) {
            mensaje = String.format("Costo envío: $%.2f (sin IVA: $%.2f) - %s (iteraciones: %d)",
                    costoEnvioConIva, costoEnvioSinIva, tipoCalculo, iteracion);
            guardarCostoEnvio(mlaCode, costoEnvioSinIva);
        } else {
            mensaje = "No se pudo calcular el costo de envío";
            log.warn("ML - MLA {} - {}", mlaCode, mensaje);
        }

        return new CostoEnvioResponseDTO(mlaCode, status, pvpActual, costoEnvioConIva, costoEnvioSinIva, mensaje);
    }

    /**
     * Calcula el costo de envío para un producto a partir de su ID.
     * Busca el MLA asociado al producto y delega al método principal.
     *
     * @param productoId ID del producto
     * @return DTO con el costo de envío calculado
     */
    @Transactional
    public CostoEnvioResponseDTO calcularCostoEnvioPorProducto(Integer productoId) {
        Optional<ar.com.leo.super_master_backend.dominio.producto.entity.Producto> productoOpt =
                productoRepository.findById(productoId);

        if (productoOpt.isEmpty()) {
            log.warn("ML - Producto con ID {} no encontrado", productoId);
            return new CostoEnvioResponseDTO(null, null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                    "Producto no encontrado con ID: " + productoId);
        }

        ar.com.leo.super_master_backend.dominio.producto.entity.Producto producto = productoOpt.get();
        Mla mla = producto.getMla();

        if (mla == null) {
            log.warn("ML - Producto {} no tiene MLA asociado", productoId);
            return new CostoEnvioResponseDTO(null, null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                    "El producto no tiene MLA asociado");
        }

        return self.calcularCostoEnvioGratis(mla.getMla());
    }

    // =====================================================
    // COSTO DE VENTA (COMISIONES ML)
    // =====================================================

    /**
     * Obtiene los costos de venta (comisiones) de un producto en MercadoLibre
     * y guarda el porcentaje de comisión en la base de datos.
     *
     * @param mlaCode Código MLA del producto
     * @return DTO con los costos de venta
     */
    @Transactional
    public CostoVentaResponseDTO obtenerCostoVenta(String mlaCode) {
        verificarTokens();

        // Obtener producto de ML
        Producto productoMl = getItemByMLA(mlaCode);
        if (productoMl == null) {
            log.warn("ML - No se pudo obtener el producto con MLA: {}", mlaCode);
            return new CostoVentaResponseDTO(mlaCode, null, null, null, null, null, null, null,
                    null, null, "No se pudo obtener el producto de MercadoLibre");
        }

        String status = productoMl.status;
        BigDecimal precio = BigDecimal.valueOf(productoMl.price).setScale(2, RoundingMode.HALF_UP);

        // Consultar API de costos de venta
        String uri = String.format(
                "/sites/%s/listing_prices?category_id=%s&price=%s&currency_id=%s&logistic_type=%s",
                productoMl.siteId,
                productoMl.categoryId,
                productoMl.price,
                "ARS",
                productoMl.shipping.logisticType);

        String responseBody = retryHandler.get(uri, tokens.accessToken);

        if (responseBody == null) {
            log.warn("ML - Error al obtener costos de venta para MLA {}", mlaCode);
            return new CostoVentaResponseDTO(mlaCode, status, precio, null, null, null, null, null,
                    productoMl.listingTypeId, null, "Error al consultar costos de venta");
        }

        try {
            JsonNode json = objectMapper.readTree(responseBody);
            log.info("ML - Costos de venta para MLA {}: {}", mlaCode, responseBody);

            // Buscar el listing_type correspondiente
            BigDecimal comisionVentaTotal = BigDecimal.ZERO;
            BigDecimal costoFijo = BigDecimal.ZERO;
            BigDecimal cargoFinanciacion = BigDecimal.ZERO;
            BigDecimal porcentajeMeli = BigDecimal.ZERO;
            BigDecimal porcentajeTotal = BigDecimal.ZERO;
            String listingTypeName = null;

            for (JsonNode listing : json) {
                if (productoMl.listingTypeId.equals(listing.path("listing_type_id").asText())) {
                    // Total de comisión de venta
                    comisionVentaTotal = BigDecimal.valueOf(listing.path("sale_fee_amount").asDouble(0));
                    listingTypeName = listing.path("listing_type_name").asText(null);

                    // Detalles desglosados de sale_fee_details
                    JsonNode details = listing.path("sale_fee_details");
                    if (!details.isMissingNode()) {
                        costoFijo = BigDecimal.valueOf(details.path("fixed_fee").asDouble(0));
                        cargoFinanciacion = BigDecimal.valueOf(details.path("financing_add_on_fee").asDouble(0));
                        porcentajeMeli = BigDecimal.valueOf(details.path("meli_percentage_fee").asDouble(0));
                        porcentajeTotal = BigDecimal.valueOf(details.path("percentage_fee").asDouble(0));
                    }
                    break;
                }
            }

            // Guardar el porcentaje de comisión si se obtuvo correctamente
            if (porcentajeTotal != null && porcentajeTotal.compareTo(BigDecimal.ZERO) > 0) {
                guardarComisionPorcentaje(mlaCode, porcentajeTotal);
            }

            return new CostoVentaResponseDTO(
                    mlaCode,
                    status,
                    precio,
                    comisionVentaTotal,
                    costoFijo,
                    cargoFinanciacion,
                    porcentajeMeli,
                    porcentajeTotal,
                    productoMl.listingTypeId,
                    listingTypeName,
                    String.format("Comisión total: $%.2f (Fijo: $%.2f + Financiación: $%.2f), Porcentaje: %.2f%%",
                            comisionVentaTotal, costoFijo, cargoFinanciacion, porcentajeTotal)
            );

        } catch (Exception e) {
            log.error("Error parseando respuesta de costos de venta", e);
            return new CostoVentaResponseDTO(mlaCode, status, precio, null, null, null, null, null,
                    productoMl.listingTypeId, null, "Error parseando respuesta: " + e.getMessage());
        }
    }

    /**
     * Obtiene los costos de venta de un producto a partir de su ID
     * y guarda el porcentaje de comisión en la base de datos.
     *
     * @param productoId ID del producto
     * @return DTO con los costos de venta
     */
    @Transactional
    public CostoVentaResponseDTO obtenerCostoVentaPorProducto(Integer productoId) {
        Optional<ar.com.leo.super_master_backend.dominio.producto.entity.Producto> productoOpt =
                productoRepository.findById(productoId);

        if (productoOpt.isEmpty()) {
            log.warn("ML - Producto con ID {} no encontrado", productoId);
            return new CostoVentaResponseDTO(null, null, null, null, null, null, null, null,
                    null, null, "Producto no encontrado con ID: " + productoId);
        }

        ar.com.leo.super_master_backend.dominio.producto.entity.Producto producto = productoOpt.get();
        Mla mla = producto.getMla();

        if (mla == null) {
            log.warn("ML - Producto {} no tiene MLA asociado", productoId);
            return new CostoVentaResponseDTO(null, null, null, null, null, null, null, null,
                    null, null, "El producto no tiene MLA asociado");
        }

        return obtenerCostoVenta(mla.getMla());
    }

    // =====================================================
    // PROCESO MASIVO - COSTO DE VENTA
    // =====================================================

    /**
     * Inicia el cálculo de costo de venta para todos los MLAs de forma asincrónica.
     * @return true si se inició el proceso, false si ya había uno en ejecución
     */
    public boolean iniciarCalculoCostoVentaTodos() {
        // Verificar si ya hay un proceso en ejecución
        if (!procesoMasivoCostoVentaEnEjecucion.compareAndSet(false, true)) {
            log.warn("ML - Ya hay un proceso masivo de costo de venta en ejecución");
            return false;
        }

        // Resetear estado
        cancelarProcesoMasivoCostoVenta.set(false);
        resultadoProcesoMasivoCostoVenta = null;

        int total = (int) mlaRepository.count();
        estadoProcesoMasivoCostoVenta = ProcesoMasivoEstadoDTO.iniciado(total, LocalDateTime.now());

        // Ejecutar en background
        self.calcularCostoVentaTodosAsync();

        return true;
    }

    /**
     * Calcula el costo de venta para todos los MLAs en la base de datos de forma asincrónica.
     * Este método se ejecuta en un thread separado.
     */
    @Async
    public void calcularCostoVentaTodosAsync() {
        List<Mla> mlas = mlaRepository.findAll();
        List<CostoVentaResponseDTO> resultados = new ArrayList<>();
        int exitosos = 0;
        int errores = 0;
        int omitidos = 0;

        LocalDateTime iniciadoEn = estadoProcesoMasivoCostoVenta.iniciadoEn();
        log.info("ML - Iniciando cálculo masivo de costos de venta para {} MLAs", mlas.size());

        try {
            for (Mla mla : mlas) {
                // Verificar si se solicitó cancelación
                if (cancelarProcesoMasivoCostoVenta.get()) {
                    omitidos = mlas.size() - resultados.size();
                    log.info("ML - Proceso masivo de costo de venta cancelado. Procesados: {}, Omitidos: {}",
                            resultados.size(), omitidos);
                    break;
                }

                try {
                    CostoVentaResponseDTO resultado = obtenerCostoVenta(mla.getMla());
                    resultados.add(resultado);

                    if (resultado.comisionVentaTotal() != null && resultado.comisionVentaTotal().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        exitosos++;
                    } else {
                        errores++;
                    }

                } catch (Exception e) {
                    log.error("ML - Error procesando costo de venta MLA {}: {}", mla.getMla(), e.getMessage());
                    CostoVentaResponseDTO error = new CostoVentaResponseDTO(
                            mla.getMla(), null, null, null, null, null, null, null,
                            null, null, "Error: " + e.getMessage());
                    resultados.add(error);
                    errores++;
                }

                // Actualizar estado de progreso
                estadoProcesoMasivoCostoVenta = new ProcesoMasivoEstadoDTO(
                        true,
                        mlas.size(),
                        resultados.size(),
                        exitosos,
                        errores,
                        "ejecutando",
                        iniciadoEn,
                        null,
                        String.format("Procesando %d/%d", resultados.size(), mlas.size())
                );
            }

            // Proceso terminado
            LocalDateTime finalizadoEn = LocalDateTime.now();
            String estado = cancelarProcesoMasivoCostoVenta.get() ? "cancelado" : "completado";

            log.info("ML - Cálculo masivo de costo de venta {}. Exitosos: {}, Errores: {}, Omitidos: {}",
                    estado, exitosos, errores, omitidos);

            // Guardar resultado
            resultadoProcesoMasivoCostoVenta = new CostoVentaMasivoResponseDTO(
                    resultados.size(),
                    exitosos,
                    errores,
                    omitidos,
                    resultados
            );

            // Actualizar estado final
            estadoProcesoMasivoCostoVenta = new ProcesoMasivoEstadoDTO(
                    false,
                    mlas.size(),
                    resultados.size(),
                    exitosos,
                    errores,
                    estado,
                    iniciadoEn,
                    finalizadoEn,
                    String.format("Proceso %s. Exitosos: %d, Errores: %d, Omitidos: %d",
                            estado, exitosos, errores, omitidos)
            );

        } catch (Exception e) {
            log.error("ML - Error fatal en proceso masivo de costo de venta: {}", e.getMessage(), e);
            estadoProcesoMasivoCostoVenta = new ProcesoMasivoEstadoDTO(
                    false, 0, 0, 0, 0, "error", null, LocalDateTime.now(),
                    "Error fatal: " + e.getMessage()
            );
        } finally {
            procesoMasivoCostoVentaEnEjecucion.set(false);
            cancelarProcesoMasivoCostoVenta.set(false);
        }
    }

    /**
     * Cancela el proceso masivo de cálculo de costos de venta en ejecución.
     * @return true si había un proceso en ejecución que fue marcado para cancelar
     */
    public boolean cancelarProcesoMasivoCostoVenta() {
        if (procesoMasivoCostoVentaEnEjecucion.get()) {
            cancelarProcesoMasivoCostoVenta.set(true);
            log.info("ML - Solicitud de cancelación de proceso masivo de costo de venta recibida");
            return true;
        }
        log.info("ML - No hay proceso masivo de costo de venta en ejecución para cancelar");
        return false;
    }

    /**
     * Obtiene el estado actual del proceso masivo de costo de venta.
     * @return DTO con el estado del proceso
     */
    public ProcesoMasivoEstadoDTO obtenerEstadoProcesoMasivoCostoVenta() {
        return estadoProcesoMasivoCostoVenta;
    }

    /**
     * Obtiene el resultado del último proceso masivo de costo de venta completado.
     * @return DTO con los resultados o null si no hay resultados disponibles
     */
    public CostoVentaMasivoResponseDTO obtenerResultadoProcesoMasivoCostoVenta() {
        return resultadoProcesoMasivoCostoVenta;
    }

    /**
     * Verifica si hay un proceso masivo de costo de venta en ejecución.
     * @return true si hay un proceso en ejecución
     */
    public boolean isProcesoMasivoCostoVentaEnEjecucion() {
        return procesoMasivoCostoVentaEnEjecucion.get();
    }

    // =====================================================
    // PROCESO MASIVO - COSTO DE ENVÍO
    // =====================================================

    /**
     * Inicia el cálculo de costo de envío para todos los MLAs de forma asincrónica.
     * @return true si se inició el proceso, false si ya había uno en ejecución
     */
    public boolean iniciarCalculoCostoEnvioTodos() {
        // Verificar si ya hay un proceso en ejecución
        if (!procesoMasivoEnEjecucion.compareAndSet(false, true)) {
            log.warn("ML - Ya hay un proceso masivo en ejecución");
            return false;
        }

        // Resetear estado
        cancelarProcesoMasivo.set(false);
        resultadoProcesoMasivo = null;

        int total = (int) mlaRepository.count();
        estadoProcesoMasivo = ProcesoMasivoEstadoDTO.iniciado(total, LocalDateTime.now());

        // Ejecutar en background
        self.calcularCostoEnvioTodosAsync();

        return true;
    }

    /**
     * Calcula el costo de envío para todos los MLAs en la base de datos de forma asincrónica.
     * Este método se ejecuta en un thread separado.
     */
    @Async
    public void calcularCostoEnvioTodosAsync() {
        List<Mla> mlas = mlaRepository.findAll();
        List<CostoEnvioResponseDTO> resultados = new ArrayList<>();
        int exitosos = 0;
        int errores = 0;
        int omitidos = 0;

        LocalDateTime iniciadoEn = estadoProcesoMasivo.iniciadoEn();
        log.info("ML - Iniciando cálculo masivo de costos de envío para {} MLAs", mlas.size());

        try {
            for (Mla mla : mlas) {
                // Verificar si se solicitó cancelación
                if (cancelarProcesoMasivo.get()) {
                    omitidos = mlas.size() - resultados.size();
                    log.info("ML - Proceso masivo cancelado. Procesados: {}, Omitidos: {}",
                            resultados.size(), omitidos);
                    break;
                }

                try {
                    // Llamar via self para que pase por el proxy y respete @Transactional
                    CostoEnvioResponseDTO resultado = self.calcularCostoEnvioGratis(mla.getMla());
                    resultados.add(resultado);

                    if (resultado.costoEnvioSinIva().compareTo(BigDecimal.ZERO) > 0) {
                        exitosos++;
                    } else {
                        errores++;
                    }

                } catch (Exception e) {
                    log.error("ML - Error procesando MLA {}: {}", mla.getMla(), e.getMessage());
                    CostoEnvioResponseDTO error = new CostoEnvioResponseDTO(
                            mla.getMla(), null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                            "Error: " + e.getMessage());
                    resultados.add(error);
                    errores++;
                }

                // Actualizar estado de progreso
                estadoProcesoMasivo = new ProcesoMasivoEstadoDTO(
                        true,
                        mlas.size(),
                        resultados.size(),
                        exitosos,
                        errores,
                        "ejecutando",
                        iniciadoEn,
                        null,
                        String.format("Procesando %d/%d", resultados.size(), mlas.size())
                );
            }

            // Proceso terminado
            LocalDateTime finalizadoEn = LocalDateTime.now();
            String estado = cancelarProcesoMasivo.get() ? "cancelado" : "completado";

            log.info("ML - Cálculo masivo {}. Exitosos: {}, Errores: {}, Omitidos: {}",
                    estado, exitosos, errores, omitidos);

            // Guardar resultado final
            resultadoProcesoMasivo = new CostoEnvioMasivoResponseDTO(
                    resultados.size(), exitosos, errores, omitidos, resultados);

            // Actualizar estado final
            estadoProcesoMasivo = new ProcesoMasivoEstadoDTO(
                    false,
                    mlas.size(),
                    resultados.size(),
                    exitosos,
                    errores,
                    estado,
                    iniciadoEn,
                    finalizadoEn,
                    String.format("Proceso %s. Exitosos: %d, Errores: %d, Omitidos: %d",
                            estado, exitosos, errores, omitidos)
            );

        } finally {
            // Siempre liberar el flag de ejecución
            procesoMasivoEnEjecucion.set(false);
            cancelarProcesoMasivo.set(false);
        }
    }

    /**
     * Cancela el proceso masivo de cálculo de costos de envío en ejecución.
     * @return true si había un proceso en ejecución que fue marcado para cancelar
     */
    public boolean cancelarProcesoMasivo() {
        if (procesoMasivoEnEjecucion.get()) {
            cancelarProcesoMasivo.set(true);
            log.info("ML - Solicitud de cancelación de proceso masivo recibida");
            return true;
        }
        log.info("ML - No hay proceso masivo en ejecución para cancelar");
        return false;
    }

    /**
     * Obtiene el estado actual del proceso masivo.
     * @return DTO con el estado del proceso
     */
    public ProcesoMasivoEstadoDTO obtenerEstadoProcesoMasivo() {
        return estadoProcesoMasivo;
    }

    /**
     * Obtiene el resultado del último proceso masivo completado.
     * @return DTO con los resultados o null si no hay resultados disponibles
     */
    public CostoEnvioMasivoResponseDTO obtenerResultadoProcesoMasivo() {
        return resultadoProcesoMasivo;
    }

    /**
     * Verifica si hay un proceso masivo en ejecución.
     * @return true si hay un proceso en ejecución
     */
    public boolean isProcesoMasivoEnEjecucion() {
        return procesoMasivoEnEjecucion.get();
    }

    /**
     * Guarda el porcentaje de comisión en la entidad Mla.
     */
    private void guardarComisionPorcentaje(String mlaCode, BigDecimal porcentaje) {
        Optional<Mla> mlaOpt = mlaRepository.findByMla(mlaCode);
        if (mlaOpt.isPresent()) {
            Mla mla = mlaOpt.get();
            BigDecimal comisionAnterior = mla.getComisionPorcentaje();

            mla.setComisionPorcentaje(porcentaje);
            mla.setFechaCalculoComision(LocalDateTime.now());
            mlaRepository.save(mla);
            log.info("ML - Porcentaje de comisión guardado para MLA {}: {}%", mlaCode, porcentaje);

            // Recalcular precios si cambió la comisión
            if (comisionAnterior == null || comisionAnterior.compareTo(porcentaje) != 0) {
                recalculoPrecioFacade.recalcularPorCambioMla(mla.getId());
                log.info("ML - Precios recalculados para MLA: {}", mlaCode);
            }
        } else {
            log.warn("ML - No se encontró el MLA {} en la base de datos para guardar la comisión", mlaCode);
        }
    }

    /**
     * Guarda el costo de envío en la entidad Mla.
     */
    private void guardarCostoEnvio(String mlaCode, BigDecimal costoEnvio) {
        Optional<Mla> mlaOpt = mlaRepository.findByMla(mlaCode);
        if (mlaOpt.isPresent()) {
            Mla mla = mlaOpt.get();
            BigDecimal precioAnterior = mla.getPrecioEnvio();

            mla.setPrecioEnvio(costoEnvio);
            mla.setFechaCalculoEnvio(LocalDateTime.now());
            mlaRepository.save(mla);
            log.info("ML - Costo de envío (sin IVA) guardado para MLA {}: ${}", mlaCode, costoEnvio);

            // Recalcular precios si cambió el costo de envío
            if (precioAnterior == null || precioAnterior.compareTo(costoEnvio) != 0) {
                recalculoPrecioFacade.recalcularPorCambioMla(mla.getId());
                log.info("ML - Precios recalculados para MLA: {}", mlaCode);
            }
        } else {
            log.warn("ML - No se encontró el MLA {} en la base de datos para guardar el costo", mlaCode);
        }
    }

    /**
     * Calcula el costo de envío gratis usando la API de ML.
     */
    private BigDecimal calcularCostoEnvioInterno(String userId, Producto producto, BigDecimal precioEnvioGratis) {
        verificarTokens();

        final String itemId = producto.id;
        final String itemPrice = String.format(Locale.forLanguageTag("en-US"), "%.2f", precioEnvioGratis);
        final String listingType = producto.listingTypeId;
        final String mode = producto.shipping.mode;
        final String condition = producto.condition;
        final String logisticType = producto.shipping.logisticType;
        final String zipCode = producto.sellerAddress.zipCode;

        final String uri = String.format(
                "/users/%s/shipping_options/free?item_id=%s&item_price=%s&listing_type_id=%s&mode=%s&condition=%s&logistic_type=%s&zip_code=%s&verbose=true",
                userId, itemId, itemPrice, listingType, mode, condition, logisticType, zipCode);

        String responseBody = retryHandler.get(uri, tokens.accessToken);

        if (responseBody == null) {
            log.warn("ML - Error al obtener el costo de envío del producto {}", itemId);
            return BigDecimal.ZERO;
        }

        try {
            JsonNode json = objectMapper.readTree(responseBody);
            log.info("ML - API Response para precio {}: {}", itemPrice, responseBody);
            double cost = json.path("coverage").path("all_country").path("list_cost").asDouble(0);
            return BigDecimal.valueOf(cost);
        } catch (Exception e) {
            log.error("Error parseando respuesta de costo de envío", e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Obtiene un producto de ML por su MLA.
     */
    public Producto getItemByMLA(String itemId) {
        verificarTokens();

        try {
            return retryHandler.get("/items/" + itemId, tokens.accessToken, Producto.class);
        } catch (Exception e) {
            log.warn("ML - No se pudo obtener item {}: {}", itemId, e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene el userId del usuario autenticado.
     */
    public String getUserId() throws IOException {
        // Usar cache si está disponible
        if (cachedUserId != null) {
            return cachedUserId;
        }

        verificarTokens();

        String responseBody = retryHandler.get("/users/me", tokens.accessToken);

        if (responseBody == null) {
            throw new IOException("Error al obtener el user ID de ML");
        }

        try {
            cachedUserId = objectMapper.readTree(responseBody).get("id").asString();
            return cachedUserId;
        } catch (Exception e) {
            throw new IOException("Error parseando userId de ML", e);
        }
    }

    // ==================== MANEJO DE CREDENCIALES ====================

    private void cargarCredentials() {
        try {
            File credFile = properties.getCredentialsFile().toFile();
            if (credFile.exists()) {
                credentials = objectMapper.readValue(credFile, MLCredentials.class);
                log.info("ML - Credenciales cargadas desde {}", credFile.getAbsolutePath());
            } else {
                log.warn("ML - No se encontraron credenciales en {}", credFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error cargando credenciales ML: {}", e.getMessage());
        }
    }

    // ==================== MANEJO DE TOKENS ====================

    private void verificarTokens() {
        if (tokens == null) {
            log.warn("ML - Tokens no inicializados. Intentando cargar...");
            cargarTokens();
            if (tokens == null) {
                throw new ServiceNotConfiguredException("MercadoLibre",
                        "No hay tokens disponibles. Debe generar los tokens de autenticación primero.");
            }
            return;
        }

        if (!tokens.isExpired()) {
            return;
        }

        synchronized (tokenLock) {
            if (tokens == null || !tokens.isExpired()) {
                return;
            }

            if (credentials == null) {
                throw new ServiceNotConfiguredException("MercadoLibre",
                        "No hay credenciales configuradas para renovar el token expirado. " +
                        "Verifique el archivo ml_credentials.json");
            }

            log.info("ML - Access token expirado, renovando...");
            try {
                tokens = refreshAccessToken(tokens.refreshToken);
                tokens.issuedAt = System.currentTimeMillis();
                guardarTokens(tokens);
                // Limpiar cache de userId al renovar tokens
                cachedUserId = null;
                log.info("ML - Token renovado correctamente.");
            } catch (Exception e) {
                log.error("ML - Error al renovar token", e);
                throw new ServiceNotConfiguredException("MercadoLibre",
                        "Error al renovar el token: " + e.getMessage() + ". " +
                        "Es posible que el refresh_token haya expirado y necesite re-autenticarse.");
            }
        }
    }

    private void cargarTokens() {
        try {
            File file = properties.getTokenFile().toFile();
            if (file.exists()) {
                tokens = objectMapper.readValue(file, TokensML.class);
                log.info("ML - Tokens cargados desde {}", file.getAbsolutePath());
            } else {
                log.warn("ML - Archivo de tokens no encontrado en {}", file.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Error cargando tokens ML", e);
        }
    }

    private void guardarTokens(TokensML tokens) {
        try {
            File file = properties.getTokenFile().toFile();
            file.getParentFile().mkdirs();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, tokens);
            log.info("ML - Tokens guardados en {}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("Error guardando tokens ML", e);
        }
    }

    private TokensML refreshAccessToken(String refreshToken) {
        if (credentials == null) {
            throw new ServiceNotConfiguredException("MercadoLibre",
                    "No hay credenciales configuradas para renovar el token. " +
                    "Verifique el archivo ml_credentials.json");
        }

        String formBody = String.format(
                "grant_type=refresh_token&client_id=%s&client_secret=%s&refresh_token=%s",
                credentials.clientId, credentials.clientSecret, refreshToken);

        String responseBody = retryHandler.postForm("/oauth/token", formBody);

        if (responseBody == null) {
            throw new RuntimeException("Error al refrescar access_token");
        }

        try {
            TokensML newTokens = objectMapper.readValue(responseBody, TokensML.class);
            newTokens.issuedAt = System.currentTimeMillis();
            return newTokens;
        } catch (Exception e) {
            throw new RuntimeException("Error parseando tokens de ML", e);
        }
    }

    /**
     * Verifica si el servicio tiene configuración completa.
     */
    public boolean isConfigured() {
        return tokens != null && credentials != null;
    }
}
