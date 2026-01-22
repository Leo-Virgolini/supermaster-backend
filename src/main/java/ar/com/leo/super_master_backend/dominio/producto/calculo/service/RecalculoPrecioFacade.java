package ar.com.leo.super_master_backend.dominio.producto.calculo.service;

import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoRepository;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ProductoRepository productoRepository;
    private final CanalConceptoRepository canalConceptoRepository;

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
     * Recalcula cuando cambia un ConceptoCalculo (porcentaje, aplicaSobre).
     * Alcance: Todos los productos de todos los canales que usan ese concepto.
     */
    @Transactional
    public void recalcularPorCambioConceptoCalculo(Integer idConcepto) {
        log.info("Recalculando precios por cambio en concepto de cálculo: {}", idConcepto);

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
     * Recalcula cuando cambia una promoción o se asigna/desasigna.
     * Alcance: Ese producto en ese canal.
     */
    @Transactional
    public void recalcularPorCambioPromocion(Integer idProducto, Integer idCanal) {
        log.info("Recalculando precios por cambio en promoción: producto={}, canal={}", idProducto, idCanal);

        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(idProducto, idCanal);
    }

    /**
     * Recalcula cuando cambia el precioEnvio de un MLA.
     * Alcance: Todos los productos que tienen ese MLA, en todos sus canales.
     */
    @Transactional
    public void recalcularPorCambioMla(Integer idMla) {
        log.info("Recalculando precios por cambio en MLA: {}", idMla);

        productoRepository.findByMlaId(idMla)
                .forEach(producto -> recalcularPorCambioProducto(producto.getId()));
    }

    /**
     * Recalcula cuando cambia una ReglaDescuento.
     * Alcance: Todos los productos del canal de esa regla.
     */
    @Transactional
    public void recalcularPorCambioReglaDescuentoEnCanal(Integer idCanal) {
        log.info("Recalculando precios por cambio en regla de descuento del canal: {}", idCanal);

        recalcularTodosProductosDelCanal(idCanal);
    }

    /**
     * Recalcula cuando cambia la clasificación gastronómica (esMaquina).
     * Alcance: Todos los productos de esa clasificación en todos sus canales.
     */
    @Transactional
    public void recalcularPorCambioClasifGastro(Integer idClasifGastro) {
        log.info("Recalculando precios por cambio en clasificación gastronómica: clasifGastro={}", idClasifGastro);

        productoRepository.findByClasifGastroId(idClasifGastro)
                .forEach(producto -> recalcularPorCambioProducto(producto.getId()));
    }

    /**
     * Recalcula cuando cambia el canalBase de un canal.
     * Alcance: Todos los productos del canal cuyo canalBase cambió.
     */
    @Transactional
    public void recalcularPorCambioCanalBase(Integer idCanal) {
        log.info("Recalculando precios por cambio en canalBase del canal: {}", idCanal);

        recalcularTodosProductosDelCanal(idCanal);
    }

    /**
     * Recalcula todos los productos de un canal.
     * Usado internamente y cuando cambian configuraciones del canal.
     */
    @Transactional
    public void recalcularTodosProductosDelCanal(Integer idCanal) {
        log.info("Recalculando todos los precios del canal: {}", idCanal);

        // OPTIMIZACIÓN: Usar FETCH JOIN para evitar N+1 al acceder al Producto
        productoCanalPrecioRepository.findByCanalIdWithProductoFetch(idCanal)
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
}
