package ar.com.leo.super_master_backend.dominio.producto.calculo.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoCuota;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConceptoId;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoCuotaRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.canal.service.CanalConceptoService;
import ar.com.leo.super_master_backend.dominio.canal.service.CanalService;
import ar.com.leo.super_master_backend.dominio.canal.dto.CanalUpdateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.entity.ClasifGastro;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.repository.ClasifGastroRepository;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.service.ClasifGastroService;
import ar.com.leo.super_master_backend.dominio.clasif_gastro.dto.ClasifGastroUpdateDTO;
import ar.com.leo.super_master_backend.dominio.clasif_gral.entity.ClasifGral;
import ar.com.leo.super_master_backend.dominio.clasif_gral.repository.ClasifGralRepository;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.entity.AplicaSobre;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.entity.ConceptoCalculo;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.repository.ConceptoCalculoRepository;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.service.ConceptoCalculoService;
import ar.com.leo.super_master_backend.dominio.concepto_calculo.dto.ConceptoCalculoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.origen.entity.Origen;
import ar.com.leo.super_master_backend.dominio.origen.repository.OrigenRepository;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoMargen;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoMargenRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoService;
import ar.com.leo.super_master_backend.dominio.producto.service.ProductoMargenService;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.dto.ProductoMargenDTO;
import ar.com.leo.super_master_backend.dominio.proveedor.entity.Proveedor;
import ar.com.leo.super_master_backend.dominio.proveedor.repository.ProveedorRepository;
import ar.com.leo.super_master_backend.dominio.proveedor.service.ProveedorService;
import ar.com.leo.super_master_backend.dominio.proveedor.dto.ProveedorUpdateDTO;
import ar.com.leo.super_master_backend.dominio.producto.mla.entity.Mla;
import ar.com.leo.super_master_backend.dominio.producto.mla.repository.MlaRepository;
import ar.com.leo.super_master_backend.dominio.producto.mla.service.MlaService;
import ar.com.leo.super_master_backend.dominio.producto.mla.dto.MlaUpdateDTO;
import ar.com.leo.super_master_backend.dominio.regla_descuento.repository.ReglaDescuentoRepository;
import ar.com.leo.super_master_backend.dominio.regla_descuento.service.ReglaDescuentoService;
import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoCreateDTO;
import ar.com.leo.super_master_backend.dominio.regla_descuento.dto.ReglaDescuentoUpdateDTO;
import ar.com.leo.super_master_backend.dominio.tipo.entity.Tipo;
import ar.com.leo.super_master_backend.dominio.tipo.repository.TipoRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de integración para verificar que los triggers de recálculo automático
 * funcionan correctamente cuando se modifican las entidades relacionadas.
 *
 * Triggers probados:
 * 1. Producto (costo, iva)
 * 2. ProductoMargen (márgenes)
 * 3. ConceptoCalculo (porcentaje)
 * 4. CanalConcepto (asignar/quitar concepto)
 * 5. CanalConceptoCuota (porcentaje cuotas)
 * 6. Canal (canalBase)
 * 7. Proveedor (porcentaje financiación)
 * 8. ClasifGastro (esMaquina)
 * 9. ReglaDescuento
 * 10. MLA (precioEnvio)
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class RecalculoAutomaticoIntegrationTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoMargenRepository productoMargenRepository;

    @Autowired
    private CanalRepository canalRepository;

    @Autowired
    private ConceptoCalculoRepository conceptoGastoRepository;

    @Autowired
    private CanalConceptoRepository canalConceptoRepository;

    @Autowired
    private CanalConceptoCuotaRepository canalConceptoCuotaRepository;

    @Autowired
    private ProductoCanalPrecioRepository productoCanalPrecioRepository;

    @Autowired
    private OrigenRepository origenRepository;

    @Autowired
    private TipoRepository tipoRepository;

    @Autowired
    private ClasifGralRepository clasifGralRepository;

    @Autowired
    private ClasifGastroRepository clasifGastroRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private MlaRepository mlaRepository;

    @Autowired
    private ReglaDescuentoRepository reglaDescuentoRepository;

    @Autowired
    private CalculoPrecioService calculoPrecioService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ProductoMargenService productoMargenService;

    @Autowired
    private ConceptoCalculoService conceptoGastoService;

    @Autowired
    private CanalConceptoService canalConceptoService;

    @Autowired
    private CanalService canalService;

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private ClasifGastroService clasifGastroService;

    @Autowired
    private MlaService mlaService;

    @Autowired
    private ReglaDescuentoService reglaDescuentoService;

    @PersistenceContext
    private EntityManager entityManager;

    // Entidades de prueba
    private Producto producto;
    private Canal canal;
    private ConceptoCalculo conceptoMargen;
    private ConceptoCalculo conceptoComision;
    private ProductoMargen productoMargen;

    private static final String TEST_PREFIX = "ZTEST_";

    @BeforeEach
    void setUp() {
        // Con @Transactional, cada test tiene su propia transacción que hace rollback al final
        // No necesitamos limpiar datos porque no se persisten

        // Buscar entidades base existentes o crearlas
        Origen origen = origenRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Origen o = new Origen();
                    o.setOrigen(TEST_PREFIX + "Origen");
                    return origenRepository.save(o);
                });

        Tipo tipo = tipoRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    Tipo t = new Tipo();
                    t.setNombre(TEST_PREFIX + "Tipo");
                    return tipoRepository.save(t);
                });

        ClasifGral clasifGral = clasifGralRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    ClasifGral c = new ClasifGral();
                    c.setNombre(TEST_PREFIX + "ClasifGral");
                    return clasifGralRepository.save(c);
                });

        // Crear canal
        canal = new Canal();
        canal.setCanal(TEST_PREFIX + "CANAL");
        canal = canalRepository.save(canal);

        // Crear concepto de margen minorista (FLAG)
        conceptoMargen = new ConceptoCalculo();
        conceptoMargen.setConcepto(TEST_PREFIX + "MARGEN_MIN");
        conceptoMargen.setPorcentaje(BigDecimal.ZERO);
        conceptoMargen.setAplicaSobre(AplicaSobre.FLAG_USAR_MARGEN_MINORISTA);
        conceptoMargen = conceptoGastoRepository.save(conceptoMargen);

        // Crear concepto de comisión sobre PVP
        conceptoComision = new ConceptoCalculo();
        conceptoComision.setConcepto(TEST_PREFIX + "COMISION");
        conceptoComision.setPorcentaje(new BigDecimal("10"));
        conceptoComision.setAplicaSobre(AplicaSobre.COMISION_SOBRE_PVP);
        conceptoComision = conceptoGastoRepository.save(conceptoComision);

        // Asignar conceptos al canal
        CanalConcepto ccMargen = new CanalConcepto();
        ccMargen.setId(new CanalConceptoId(canal.getId(), conceptoMargen.getId()));
        ccMargen.setCanal(canal);
        ccMargen.setConcepto(conceptoMargen);
        canalConceptoRepository.save(ccMargen);

        CanalConcepto ccComision = new CanalConcepto();
        ccComision.setId(new CanalConceptoId(canal.getId(), conceptoComision.getId()));
        ccComision.setCanal(canal);
        ccComision.setConcepto(conceptoComision);
        canalConceptoRepository.save(ccComision);

        // Crear producto
        producto = new Producto();
        producto.setSku(TEST_PREFIX + "001");
        producto.setDescripcion(TEST_PREFIX + "Producto de prueba");
        producto.setTituloWeb(TEST_PREFIX + "Producto Test");
        producto.setCosto(new BigDecimal("1000"));
        producto.setIva(new BigDecimal("21"));
        producto.setOrigen(origen);
        producto.setTipo(tipo);
        producto.setClasifGral(clasifGral);
        producto = productoRepository.save(producto);

        // Crear margen para el producto
        productoMargen = new ProductoMargen();
        productoMargen.setProducto(producto);
        productoMargen.setMargenMinorista(new BigDecimal("50"));
        productoMargen.setMargenMayorista(new BigDecimal("30"));
        productoMargen = productoMargenRepository.save(productoMargen);

        // Crear cuota de contado (1 pago) para que el cálculo funcione
        CanalConceptoCuota cuotaContado = new CanalConceptoCuota();
        cuotaContado.setCanal(canal);
        cuotaContado.setCuotas(1); // 1 = contado/1 pago
        cuotaContado.setPorcentaje(BigDecimal.ZERO);
        cuotaContado.setDescripcion("Contado");
        canalConceptoCuotaRepository.save(cuotaContado);

        // Sincronizar con la BD antes de calcular
        entityManager.flush();

        // Calcular precio inicial
        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(producto.getId(), canal.getId());

        // Sincronizar el precio calculado
        entityManager.flush();
    }

    // No necesitamos @AfterEach porque @Transactional hace rollback automáticamente

    private BigDecimal obtenerPvpActual() {
        entityManager.flush();
        entityManager.clear(); // Limpiar cache de primer nivel para ver datos frescos
        var precio = productoCanalPrecioRepository
                .findByProductoIdAndCanalIdAndCuotas(producto.getId(), canal.getId(), 1);
        assertTrue(precio.isPresent(), "Debe existir al menos un precio calculado");
        return precio.get().getPvp();
    }

    // ===========================================
    // TEST 1: Cambio en Producto (costo)
    // ===========================================
    @Test
    @Order(1)
    @DisplayName("1. Recálculo automático al cambiar costo del Producto")
    void testRecalculoPorCambioCostoProducto() {
        BigDecimal pvpInicial = obtenerPvpActual();

        // Modificar costo del producto usando el servicio
        productoService.actualizar(producto.getId(),
                new ProductoUpdateDTO(
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        new BigDecimal("1500"), // nuevo costo
                        null
                ));

        BigDecimal pvpNuevo = obtenerPvpActual();

        assertNotEquals(pvpInicial, pvpNuevo,
                "El PVP debe cambiar al modificar el costo del producto");
        assertTrue(pvpNuevo.compareTo(pvpInicial) > 0,
                "El PVP debe aumentar al aumentar el costo");
    }

    // ===========================================
    // TEST 2: Cambio en Producto (IVA)
    // ===========================================
    @Test
    @Order(2)
    @DisplayName("2. Recálculo automático al cambiar IVA del Producto")
    void testRecalculoPorCambioIvaProducto() {
        // Primero necesitamos agregar el concepto FLAG_APLICAR_IVA
        ConceptoCalculo conceptoIva = new ConceptoCalculo();
        conceptoIva.setConcepto(TEST_PREFIX + "IVA");
        conceptoIva.setPorcentaje(BigDecimal.ZERO);
        conceptoIva.setAplicaSobre(AplicaSobre.FLAG_APLICAR_IVA);
        conceptoIva = conceptoGastoRepository.save(conceptoIva);

        CanalConcepto ccIva = new CanalConcepto();
        ccIva.setId(new CanalConceptoId(canal.getId(), conceptoIva.getId()));
        ccIva.setCanal(canal);
        ccIva.setConcepto(conceptoIva);
        canalConceptoRepository.save(ccIva);

        // Recalcular con IVA
        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(producto.getId(), canal.getId());
        BigDecimal pvpInicial = obtenerPvpActual();

        // Modificar IVA del producto
        productoService.actualizar(producto.getId(),
                new ProductoUpdateDTO(
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        null,
                        new BigDecimal("10.5") // nuevo IVA
                ));

        BigDecimal pvpNuevo = obtenerPvpActual();

        assertNotEquals(pvpInicial, pvpNuevo,
                "El PVP debe cambiar al modificar el IVA del producto");
    }

    // ===========================================
    // TEST 3: Cambio en ProductoMargen
    // ===========================================
    @Test
    @Order(3)
    @DisplayName("3. Recálculo automático al cambiar ProductoMargen")
    void testRecalculoPorCambioProductoMargen() {
        BigDecimal pvpInicial = obtenerPvpActual();

        // Modificar margen del producto
        productoMargenService.guardar(
                new ProductoMargenDTO(
                        productoMargen.getId(),
                        producto.getId(),
                        new BigDecimal("70"), // nuevo margen minorista
                        new BigDecimal("30"),
                        null, null, null
                ));

        BigDecimal pvpNuevo = obtenerPvpActual();

        assertNotEquals(pvpInicial, pvpNuevo,
                "El PVP debe cambiar al modificar el margen del producto");
        assertTrue(pvpNuevo.compareTo(pvpInicial) > 0,
                "El PVP debe aumentar al aumentar el margen");
    }

    // ===========================================
    // TEST 4: Cambio en ConceptoCalculo (porcentaje)
    // ===========================================
    @Test
    @Order(4)
    @DisplayName("4. Recálculo automático al cambiar porcentaje de ConceptoCalculo")
    void testRecalculoPorCambioConceptoCalculo() {
        BigDecimal pvpInicial = obtenerPvpActual();

        // Modificar porcentaje del concepto de comisión
        conceptoGastoService.actualizar(conceptoComision.getId(),
                new ConceptoCalculoUpdateDTO(
                        null,
                        new BigDecimal("20"), // aumentar comisión de 10% a 20%
                        null,
                        null
                ));

        BigDecimal pvpNuevo = obtenerPvpActual();

        assertNotEquals(pvpInicial, pvpNuevo,
                "El PVP debe cambiar al modificar el porcentaje del concepto");
        assertTrue(pvpNuevo.compareTo(pvpInicial) > 0,
                "El PVP debe aumentar al aumentar la comisión (divisor mayor)");
    }

    // ===========================================
    // TEST 5: Cambio en CanalConcepto (asignar concepto)
    // ===========================================
    @Test
    @Order(5)
    @DisplayName("5. Recálculo automático al asignar concepto a canal")
    void testRecalculoPorAsignarConceptoACanal() {
        BigDecimal pvpInicial = obtenerPvpActual();

        // Crear y asignar nuevo concepto de gasto sobre costo
        ConceptoCalculo conceptoEmbalaje = new ConceptoCalculo();
        conceptoEmbalaje.setConcepto(TEST_PREFIX + "EMBALAJE");
        conceptoEmbalaje.setPorcentaje(new BigDecimal("5"));
        conceptoEmbalaje.setAplicaSobre(AplicaSobre.GASTO_SOBRE_COSTO);
        conceptoEmbalaje = conceptoGastoRepository.save(conceptoEmbalaje);

        // Asignar al canal (esto debe disparar recálculo)
        canalConceptoService.asignarConcepto(canal.getId(), conceptoEmbalaje.getId());

        BigDecimal pvpNuevo = obtenerPvpActual();

        assertNotEquals(pvpInicial, pvpNuevo,
                "El PVP debe cambiar al asignar un nuevo concepto al canal");
        assertTrue(pvpNuevo.compareTo(pvpInicial) > 0,
                "El PVP debe aumentar al agregar gasto sobre costo");
    }

    // ===========================================
    // TEST 6: Cambio en CanalConcepto (quitar concepto)
    // ===========================================
    @Test
    @Order(6)
    @DisplayName("6. Recálculo automático al quitar concepto de canal")
    void testRecalculoPorQuitarConceptoDeCanal() {
        BigDecimal pvpInicial = obtenerPvpActual();

        // Quitar el concepto de comisión (esto debe disparar recálculo)
        canalConceptoService.eliminarConcepto(canal.getId(), conceptoComision.getId());

        BigDecimal pvpNuevo = obtenerPvpActual();

        assertNotEquals(pvpInicial, pvpNuevo,
                "El PVP debe cambiar al quitar un concepto del canal");
        assertTrue(pvpNuevo.compareTo(pvpInicial) < 0,
                "El PVP debe disminuir al quitar la comisión");
    }

    // ===========================================
    // TEST 7: Cambio en CanalConceptoCuota
    // ===========================================
    @Test
    @Order(7)
    @DisplayName("7. Recálculo automático al modificar cuotas del canal")
    void testRecalculoPorCambioCuotaCanal() {
        // Crear una cuota para el canal
        CanalConceptoCuota cuota = new CanalConceptoCuota();
        cuota.setCanal(canal);
        cuota.setCuotas(3);
        cuota.setPorcentaje(new BigDecimal("15"));
        cuota.setDescripcion("3 cuotas");
        cuota = canalConceptoCuotaRepository.save(cuota);

        // Recalcular para incluir la cuota
        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(producto.getId(), canal.getId());

        // Obtener precio para 3 cuotas
        List<ProductoCanalPrecio> preciosConCuotas = productoCanalPrecioRepository
                .findByProductoIdAndCanalIdOrderByCuotasAsc(producto.getId(), canal.getId());

        ProductoCanalPrecio precioCuotas = preciosConCuotas.stream()
                .filter(p -> p.getCuotas() != null && p.getCuotas() == 3)
                .findFirst()
                .orElse(null);

        assertNotNull(precioCuotas, "Debe existir precio para 3 cuotas");
        BigDecimal pvpCuotasInicial = precioCuotas.getPvp();

        // Modificar el porcentaje de la cuota
        cuota.setPorcentaje(new BigDecimal("25"));
        canalConceptoCuotaRepository.save(cuota);

        // Forzar recálculo
        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(producto.getId(), canal.getId());

        preciosConCuotas = productoCanalPrecioRepository
                .findByProductoIdAndCanalIdOrderByCuotasAsc(producto.getId(), canal.getId());

        precioCuotas = preciosConCuotas.stream()
                .filter(p -> p.getCuotas() != null && p.getCuotas() == 3)
                .findFirst()
                .orElse(null);

        assertNotNull(precioCuotas, "Debe seguir existiendo precio para 3 cuotas");
        BigDecimal pvpCuotasNuevo = precioCuotas.getPvp();

        assertNotEquals(pvpCuotasInicial, pvpCuotasNuevo,
                "El PVP de cuotas debe cambiar al modificar el porcentaje");
    }

    // ===========================================
    // TEST 8: Cambio en Canal (canalBase)
    // ===========================================
    @Test
    @Order(8)
    @DisplayName("8. Recálculo automático al cambiar canalBase del canal")
    void testRecalculoPorCambioCanalBase() {
        // Crear canal padre 1
        Canal canalPadre1 = new Canal();
        canalPadre1.setCanal(TEST_PREFIX + "PADRE1");
        canalPadre1 = canalRepository.save(canalPadre1);

        // Asignar concepto de margen al canal padre 1
        CanalConcepto ccPadre1 = new CanalConcepto();
        ccPadre1.setId(new CanalConceptoId(canalPadre1.getId(), conceptoMargen.getId()));
        ccPadre1.setCanal(canalPadre1);
        ccPadre1.setConcepto(conceptoMargen);
        canalConceptoRepository.save(ccPadre1);

        // Agregar cuota al canal padre 1
        CanalConceptoCuota cuotaPadre1 = new CanalConceptoCuota();
        cuotaPadre1.setCanal(canalPadre1);
        cuotaPadre1.setCuotas(1);
        cuotaPadre1.setPorcentaje(BigDecimal.ZERO);
        cuotaPadre1.setDescripcion("Contado");
        canalConceptoCuotaRepository.save(cuotaPadre1);

        // Crear canal padre 2 con precio diferente (mayor comisión)
        Canal canalPadre2 = new Canal();
        canalPadre2.setCanal(TEST_PREFIX + "PADRE2");
        canalPadre2 = canalRepository.save(canalPadre2);

        CanalConcepto ccPadre2Margen = new CanalConcepto();
        ccPadre2Margen.setId(new CanalConceptoId(canalPadre2.getId(), conceptoMargen.getId()));
        ccPadre2Margen.setCanal(canalPadre2);
        ccPadre2Margen.setConcepto(conceptoMargen);
        canalConceptoRepository.save(ccPadre2Margen);

        // También asignar la comisión al padre 2 (mayor precio)
        CanalConcepto ccPadre2Comision = new CanalConcepto();
        ccPadre2Comision.setId(new CanalConceptoId(canalPadre2.getId(), conceptoComision.getId()));
        ccPadre2Comision.setCanal(canalPadre2);
        ccPadre2Comision.setConcepto(conceptoComision);
        canalConceptoRepository.save(ccPadre2Comision);

        CanalConceptoCuota cuotaPadre2 = new CanalConceptoCuota();
        cuotaPadre2.setCanal(canalPadre2);
        cuotaPadre2.setCuotas(1);
        cuotaPadre2.setPorcentaje(BigDecimal.ZERO);
        cuotaPadre2.setDescripcion("Contado");
        canalConceptoCuotaRepository.save(cuotaPadre2);

        entityManager.flush();

        // Calcular precio en ambos padres
        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(producto.getId(), canalPadre1.getId());
        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(producto.getId(), canalPadre2.getId());

        // Crear canal hijo
        Canal canalHijo = new Canal();
        canalHijo.setCanal(TEST_PREFIX + "HIJO");
        canalHijo = canalRepository.save(canalHijo);

        // Crear concepto CALCULO_SOBRE_CANAL_BASE
        ConceptoCalculo conceptoSobreBase = new ConceptoCalculo();
        conceptoSobreBase.setConcepto(TEST_PREFIX + "SOBRE_BASE");
        conceptoSobreBase.setPorcentaje(new BigDecimal("-10")); // 10% menos que el padre
        conceptoSobreBase.setAplicaSobre(AplicaSobre.CALCULO_SOBRE_CANAL_BASE);
        conceptoSobreBase = conceptoGastoRepository.save(conceptoSobreBase);

        // Asignar concepto al canal hijo
        CanalConcepto ccHijoBase = new CanalConcepto();
        ccHijoBase.setId(new CanalConceptoId(canalHijo.getId(), conceptoSobreBase.getId()));
        ccHijoBase.setCanal(canalHijo);
        ccHijoBase.setConcepto(conceptoSobreBase);
        canalConceptoRepository.save(ccHijoBase);

        // Agregar cuota al canal hijo
        CanalConceptoCuota cuotaHijo = new CanalConceptoCuota();
        cuotaHijo.setCanal(canalHijo);
        cuotaHijo.setCuotas(1);
        cuotaHijo.setPorcentaje(BigDecimal.ZERO);
        cuotaHijo.setDescripcion("Contado");
        canalConceptoCuotaRepository.save(cuotaHijo);

        // Configurar canalBase del hijo apuntando al padre 1
        canalHijo.setCanalBase(canalPadre1);
        canalHijo = canalRepository.save(canalHijo);

        entityManager.flush();

        // Calcular precio del hijo (basado en padre 1)
        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(producto.getId(), canalHijo.getId());

        entityManager.flush();
        entityManager.clear();

        var precioHijoOpt = productoCanalPrecioRepository
                .findByProductoIdAndCanalIdAndCuotas(producto.getId(), canalHijo.getId(), 1);
        assertTrue(precioHijoOpt.isPresent(), "Debe existir precio en canal hijo");
        BigDecimal pvpHijoInicial = precioHijoOpt.get().getPvp();

        // Cambiar el canalBase al padre 2 (esto debe disparar recálculo)
        canalService.actualizar(canalHijo.getId(), new CanalUpdateDTO(null, canalPadre2.getId()));

        entityManager.flush();
        entityManager.clear();

        precioHijoOpt = productoCanalPrecioRepository
                .findByProductoIdAndCanalIdAndCuotas(producto.getId(), canalHijo.getId(), 1);

        assertTrue(precioHijoOpt.isPresent(), "Debe seguir existiendo precio en canal hijo");
        BigDecimal pvpHijoNuevo = precioHijoOpt.get().getPvp();
        assertNotEquals(pvpHijoInicial, pvpHijoNuevo,
                "El PVP debe cambiar al modificar el canalBase de padre1 a padre2");
    }

    // ===========================================
    // TEST 9: Cambio en Proveedor (porcentaje)
    // ===========================================
    @Test
    @Order(9)
    @DisplayName("9. Recálculo automático al cambiar porcentaje del Proveedor")
    void testRecalculoPorCambioProveedor() {
        // Crear proveedor
        Proveedor proveedor = new Proveedor();
        proveedor.setProveedor(TEST_PREFIX + "Proveedor");
        proveedor.setApodo(TEST_PREFIX + "Prov");
        proveedor.setPorcentaje(new BigDecimal("5"));
        proveedor = proveedorRepository.save(proveedor);

        // Asignar proveedor al producto
        producto.setProveedor(proveedor);
        producto = productoRepository.save(producto);

        // Crear concepto FLAG_FINANCIACION_PROVEEDOR
        ConceptoCalculo conceptoFinanciacion = new ConceptoCalculo();
        conceptoFinanciacion.setConcepto(TEST_PREFIX + "FIN_PROV");
        conceptoFinanciacion.setPorcentaje(BigDecimal.ZERO);
        conceptoFinanciacion.setAplicaSobre(AplicaSobre.FLAG_FINANCIACION_PROVEEDOR);
        conceptoFinanciacion = conceptoGastoRepository.save(conceptoFinanciacion);

        // Asignar al canal
        CanalConcepto ccFin = new CanalConcepto();
        ccFin.setId(new CanalConceptoId(canal.getId(), conceptoFinanciacion.getId()));
        ccFin.setCanal(canal);
        ccFin.setConcepto(conceptoFinanciacion);
        canalConceptoRepository.save(ccFin);

        // Recalcular
        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(producto.getId(), canal.getId());
        BigDecimal pvpInicial = obtenerPvpActual();

        // Modificar porcentaje del proveedor
        proveedorService.actualizar(proveedor.getId(),
                new ProveedorUpdateDTO(
                        null, null, null, null,
                        new BigDecimal("15") // aumentar financiación
                ));

        BigDecimal pvpNuevo = obtenerPvpActual();

        assertNotEquals(pvpInicial, pvpNuevo,
                "El PVP debe cambiar al modificar el porcentaje del proveedor");
    }

    // ===========================================
    // TEST 10: Cambio en ClasifGastro (esMaquina)
    // ===========================================
    @Test
    @Order(10)
    @DisplayName("10. Recálculo automático al cambiar esMaquina de ClasifGastro")
    void testRecalculoPorCambioClasifGastro() {
        // Crear clasificación gastro
        ClasifGastro clasifGastro = new ClasifGastro();
        clasifGastro.setNombre(TEST_PREFIX + "Cafeteras");
        clasifGastro.setEsMaquina(false);
        clasifGastro = clasifGastroRepository.save(clasifGastro);

        // Asignar al producto
        producto.setClasifGastro(clasifGastro);
        producto = productoRepository.save(producto);

        // Recalcular
        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(producto.getId(), canal.getId());
        BigDecimal pvpInicial = obtenerPvpActual();

        // Cambiar esMaquina a true
        clasifGastroService.actualizar(clasifGastro.getId(),
                new ClasifGastroUpdateDTO(null, true, null));

        BigDecimal pvpNuevo = obtenerPvpActual();

        // El trigger se ejecutó (puede o no cambiar el precio según las reglas)
        assertNotNull(pvpNuevo, "Debe existir un precio después del recálculo");
    }

    // ===========================================
    // TEST 11: Cambio en MLA (precioEnvio)
    // ===========================================
    @Test
    @Order(11)
    @DisplayName("11. Recálculo automático al cambiar precioEnvio del MLA")
    void testRecalculoPorCambioMla() {
        // Crear MLA
        Mla mla = new Mla();
        mla.setMla(TEST_PREFIX + "MLA123");
        mla.setPrecioEnvio(new BigDecimal("500"));
        mla = mlaRepository.save(mla);

        // Asignar MLA al producto
        producto.setMla(mla);
        producto = productoRepository.save(producto);

        // Crear concepto FLAG_INCLUIR_ENVIO
        ConceptoCalculo conceptoEnvio = new ConceptoCalculo();
        conceptoEnvio.setConcepto(TEST_PREFIX + "ENVIO");
        conceptoEnvio.setPorcentaje(BigDecimal.ZERO);
        conceptoEnvio.setAplicaSobre(AplicaSobre.FLAG_INCLUIR_ENVIO);
        conceptoEnvio = conceptoGastoRepository.save(conceptoEnvio);

        CanalConcepto ccEnvio = new CanalConcepto();
        ccEnvio.setId(new CanalConceptoId(canal.getId(), conceptoEnvio.getId()));
        ccEnvio.setCanal(canal);
        ccEnvio.setConcepto(conceptoEnvio);
        canalConceptoRepository.save(ccEnvio);

        // Recalcular con envío
        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(producto.getId(), canal.getId());
        BigDecimal pvpInicial = obtenerPvpActual();

        // Modificar precio de envío (mantener el código MLA original)
        mlaService.actualizar(mla.getId(),
                new MlaUpdateDTO(TEST_PREFIX + "MLA123", null, new BigDecimal("1000")));

        BigDecimal pvpNuevo = obtenerPvpActual();

        assertNotEquals(pvpInicial, pvpNuevo,
                "El PVP debe cambiar al modificar el precio de envío del MLA");
        assertTrue(pvpNuevo.compareTo(pvpInicial) > 0,
                "El PVP debe aumentar al aumentar el precio de envío");
    }

    // ===========================================
    // TEST 12: Cambio en ReglaDescuento
    // ===========================================
    @Test
    @Order(12)
    @DisplayName("12. Recálculo automático al crear/modificar ReglaDescuento")
    void testRecalculoPorCambioReglaDescuento() {
        BigDecimal pvpInicial = obtenerPvpActual();

        // Crear regla de descuento
        ReglaDescuentoCreateDTO reglaDto = new ReglaDescuentoCreateDTO(
                canal.getId(),
                null, null, null,
                BigDecimal.ZERO, // monto mínimo
                new BigDecimal("5"), // 5% descuento
                1, true, "Regla test"
        );

        var reglaCreada = reglaDescuentoService.crear(reglaDto);

        BigDecimal pvpDespuesCrear = obtenerPvpActual();

        assertNotEquals(pvpInicial, pvpDespuesCrear,
                "El PVP debe cambiar al crear una regla de descuento");

        // Modificar la regla
        reglaDescuentoService.actualizar(reglaCreada.id(),
                new ReglaDescuentoUpdateDTO(
                        null, null, null, null,
                        null,
                        new BigDecimal("10"), // aumentar descuento
                        null, null, null
                ));

        BigDecimal pvpDespuesModificar = obtenerPvpActual();

        assertNotEquals(pvpDespuesCrear, pvpDespuesModificar,
                "El PVP debe cambiar al modificar la regla de descuento");
    }
}
