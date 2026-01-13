package ar.com.leo.super_master_backend.dominio.producto.calculo.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.AplicaSobre;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoMargen;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoMargenRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Facade que centraliza la lógica de recálculo automático de precios.
 * Determina QUÉ productos recalcular según QUÉ dato cambió.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecalculoPrecioFacade {

    private final CalculoPrecioService calculoPrecioService;
    private final ProductoCanalPrecioRepository productoCanalPrecioRepository;
    private final ProductoMargenRepository productoMargenRepository;
    private final ProductoRepository productoRepository;
    private final CanalConceptoRepository canalConceptoRepository;
    private final CanalRepository canalRepository;

    /**
     * Recalcula precios cuando cambia el costo/iva de un producto.
     * Alcance: Ese producto en todos sus canales y cuotas.
     */
    @Transactional
    public void recalcularPorCambioProducto(Integer idProducto) {
        log.info("Recalculando precios por cambio en producto: {}", idProducto);

        productoCanalPrecioRepository.findByProductoId(idProducto)
                .stream()
                .map(p -> p.getCanal().getId())
                .distinct()
                .forEach(idCanal ->
                        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(idProducto, idCanal)
                );
    }

    /**
     * Recalcula cuando cambia ProductoMargen (margen minorista/mayorista, margen fijo).
     * Alcance: Ese producto en TODOS sus canales, todas las cuotas.
     */
    @Transactional
    public void recalcularPorCambioProductoMargen(Integer idProducto) {
        log.info("Recalculando precios por cambio en producto-margen: producto={}", idProducto);

        // Recalcular en todos los canales donde el producto tiene precios
        productoCanalPrecioRepository.findByProductoId(idProducto)
                .stream()
                .map(p -> p.getCanal().getId())
                .distinct()
                .forEach(idCanal ->
                        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(idProducto, idCanal)
                );
    }

    /**
     * Recalcula cuando cambia un ConceptoGasto (porcentaje, aplicaSobre).
     * Alcance: Todos los productos de todos los canales que usan ese concepto.
     */
    @Transactional
    public void recalcularPorCambioConceptoGasto(Integer idConcepto) {
        log.info("Recalculando precios por cambio en concepto de gasto: {}", idConcepto);

        canalConceptoRepository.findByConceptoId(idConcepto)
                .stream()
                .map(cc -> cc.getCanal().getId())
                .distinct()
                .forEach(this::recalcularTodosProductosDelCanal);
    }

    /**
     * Recalcula cuando cambia CanalConceptoCuota (porcentaje de cuotas).
     * Alcance: Todos los productos del canal, todas las cuotas.
     */
    @Transactional
    public void recalcularPorCambioCuotaCanal(Integer idCanal) {
        log.info("Recalculando precios por cambio en cuotas del canal: {}", idCanal);

        recalcularTodosProductosDelCanal(idCanal);
    }

    /**
     * Recalcula cuando cambia Proveedor.porcentaje (financiación).
     * Alcance: Todos los productos de ese proveedor, en todos sus canales.
     */
    @Transactional
    public void recalcularPorCambioProveedor(Integer idProveedor) {
        log.info("Recalculando precios por cambio en proveedor: {}", idProveedor);

        productoRepository.findByProveedorId(idProveedor)
                .forEach(producto -> recalcularPorCambioProducto(producto.getId()));
    }

    /**
     * Recalcula cuando cambia una ReglaDescuento.
     * Alcance: Todos los productos del canal de esa regla.
     */
    @Transactional
    public void recalcularPorCambioReglaDescuento(Integer idCanal) {
        log.info("Recalculando precios por cambio en regla de descuento del canal: {}", idCanal);

        recalcularTodosProductosDelCanal(idCanal);
    }

    /**
     * Recalcula cuando cambia una promoción o se asigna/desasigna.
     * Alcance: Ese producto en ese canal.
     */
    @Transactional
    public void recalcularPorCambioPromocion(Integer idProducto, Integer idCanal) {
        log.info("Recalculando precios por cambio en promoción: producto={}, canal={}", idProducto, idCanal);

        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(idProducto, idCanal);
    }

    /**
     * Recalcula todos los productos de un canal.
     * Usado internamente y cuando cambian configuraciones del canal.
     */
    @Transactional
    public void recalcularTodosProductosDelCanal(Integer idCanal) {
        log.info("Recalculando todos los precios del canal: {}", idCanal);

        productoCanalPrecioRepository.findByCanalId(idCanal)
                .stream()
                .map(ProductoCanalPrecio::getProducto)
                .collect(Collectors.toMap(
                        p -> p.getId(),
                        p -> p,
                        (p1, p2) -> p1  // En caso de duplicados, quedarse con el primero
                ))
                .keySet()
                .forEach(idProducto ->
                        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(idProducto, idCanal)
                );
    }

    /**
     * Recalcula los precios de TODOS los productos con márgenes configurados
     * en TODOS los canales que tienen concepto MARGEN_MINORISTA o MARGEN_MAYORISTA,
     * siempre que el margen correspondiente del producto sea > 0.
     * Operación masiva que puede tomar tiempo considerable.
     *
     * @return Cantidad de registros recalculados
     */
    @Transactional
    public int recalcularTodos() {
        log.info("Iniciando recálculo masivo de todos los precios...");

        // Obtener todos los productos que tienen márgenes configurados
        List<ProductoMargen> productosConMargenes = productoMargenRepository.findAll();

        // Obtener todos los canales
        List<Canal> todosLosCanales = canalRepository.findAll();

        int totalRecalculados = 0;
        int canalesIgnorados = 0;

        log.info("Productos con márgenes: {}, Canales: {}",
                productosConMargenes.size(), todosLosCanales.size());

        // Para cada producto con márgenes, calcular en los canales válidos
        for (ProductoMargen productoMargen : productosConMargenes) {
            // Ignorar productos sin costo
            BigDecimal costo = productoMargen.getProducto().getCosto();
            if (costo == null || costo.compareTo(BigDecimal.ZERO) <= 0) {
                canalesIgnorados += todosLosCanales.size();
                continue;
            }

            Integer idProducto = productoMargen.getProducto().getId();

            for (Canal canal : todosLosCanales) {
                // Verificar si el canal tiene margen válido para este producto
                if (!tieneMargenValido(productoMargen, canal)) {
                    canalesIgnorados++;
                    continue;
                }

                try {
                    List<PrecioCalculadoDTO> precios = calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(idProducto, canal.getId());
                    totalRecalculados += precios.size();
                } catch (Exception e) {
                    log.warn("Error calculando precio para producto {} en canal {}: {}",
                            idProducto, canal.getId(), e.getMessage());
                }
            }
        }

        log.info("Recálculo masivo completado. Total: {} precios, {} combinaciones ignoradas",
                totalRecalculados, canalesIgnorados);
        return totalRecalculados;
    }

    /**
     * Verifica si un canal tiene margen válido (> 0) para un producto.
     * - Si el canal tiene MARGEN_MAYORISTA → verifica margenMayorista del producto
     * - Si el canal tiene MARGEN_MINORISTA → verifica margenMinorista del producto
     * - Si no tiene ninguno → retorna false
     */
    private boolean tieneMargenValido(ProductoMargen productoMargen, Canal canal) {
        List<CanalConcepto> conceptosCanal = canalConceptoRepository.findByCanalId(canal.getId());

        for (CanalConcepto cc : conceptosCanal) {
            if (cc.getConcepto() != null) {
                if (cc.getConcepto().getAplicaSobre() == AplicaSobre.MARGEN_MAYORISTA) {
                    BigDecimal margen = productoMargen.getMargenMayorista();
                    return margen != null && margen.compareTo(BigDecimal.ZERO) > 0;
                }
                if (cc.getConcepto().getAplicaSobre() == AplicaSobre.MARGEN_MINORISTA) {
                    BigDecimal margen = productoMargen.getMargenMinorista();
                    return margen != null && margen.compareTo(BigDecimal.ZERO) > 0;
                }
            }
        }

        // El canal no tiene concepto MARGEN_MINORISTA ni MARGEN_MAYORISTA
        return false;
    }
}
