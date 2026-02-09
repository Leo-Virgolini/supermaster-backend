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
    public void recalcularPorCambioProducto(Integer productoId) {
        log.info("Recalculando precios por cambio en producto: {}", productoId);

        productoCanalPrecioRepository.findByProductoId(productoId)
                .stream()
                .map(p -> p.getCanal().getId())
                .distinct()
                .forEach(canalId ->
                        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(productoId, canalId)
                );
    }

    /**
     * Recalcula cuando cambia ProductoMargen (margen minorista/mayorista, margen fijo).
     * Alcance: Ese producto en TODOS sus canales, todas las cuotas.
     */
    @Transactional
    public void recalcularPorCambioProductoMargen(Integer productoId) {
        log.info("Recalculando precios por cambio en producto-margen: producto={}", productoId);

        // Recalcular en todos los canales donde el producto tiene precios
        productoCanalPrecioRepository.findByProductoId(productoId)
                .stream()
                .map(p -> p.getCanal().getId())
                .distinct()
                .forEach(canalId ->
                        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(productoId, canalId)
                );
    }

    /**
     * Recalcula cuando cambia un ConceptoCalculo (porcentaje, aplicaSobre).
     * Alcance: Todos los productos de todos los canales que usan ese concepto.
     */
    @Transactional
    public void recalcularPorCambioConceptoCalculo(Integer conceptoId) {
        log.info("Recalculando precios por cambio en concepto de cálculo: {}", conceptoId);

        canalConceptoRepository.findByConceptoId(conceptoId)
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
    public void recalcularPorCambioCuotaCanal(Integer canalId) {
        log.info("Recalculando precios por cambio en cuotas del canal: {}", canalId);

        recalcularTodosProductosDelCanal(canalId);
    }

    /**
     * Recalcula cuando cambia Proveedor.financiacionPorcentaje.
     * Alcance: Todos los productos de ese proveedor, en todos sus canales.
     */
    @Transactional
    public void recalcularPorCambioProveedor(Integer proveedorId) {
        log.info("Recalculando precios por cambio en proveedor: {}", proveedorId);

        productoRepository.findByProveedorId(proveedorId)
                .forEach(producto -> recalcularPorCambioProducto(producto.getId()));
    }

    /**
     * Recalcula cuando cambia un precio inflado o se asigna/desasigna.
     * Alcance: Ese producto en ese canal.
     */
    @Transactional
    public void recalcularPorCambioPrecioInflado(Integer productoId, Integer canalId) {
        log.info("Recalculando precios por cambio en precio inflado: producto={}, canal={}", productoId, canalId);

        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(productoId, canalId);
    }

    /**
     * Recalcula cuando cambia el precioEnvio de un MLA.
     * Alcance: Todos los productos que tienen ese MLA, en todos sus canales.
     */
    @Transactional
    public void recalcularPorCambioMla(Integer mlaId) {
        log.info("Recalculando precios por cambio en MLA: {}", mlaId);

        productoRepository.findByMlaId(mlaId)
                .forEach(producto -> recalcularPorCambioProducto(producto.getId()));
    }

    /**
     * Recalcula cuando cambia una ReglaDescuento.
     * Alcance: Todos los productos del canal de esa regla.
     */
    @Transactional
    public void recalcularPorCambioReglaDescuentoEnCanal(Integer canalId) {
        log.info("Recalculando precios por cambio en regla de descuento del canal: {}", canalId);

        recalcularTodosProductosDelCanal(canalId);
    }

    /**
     * Recalcula cuando cambia la clasificación gastronómica (esMaquina).
     * Alcance: Todos los productos de esa clasificación en todos sus canales.
     */
    @Transactional
    public void recalcularPorCambioClasifGastro(Integer clasifGastroId) {
        log.info("Recalculando precios por cambio en clasificación gastronómica: clasifGastro={}", clasifGastroId);

        productoRepository.findByClasifGastroId(clasifGastroId)
                .forEach(producto -> recalcularPorCambioProducto(producto.getId()));
    }

    /**
     * Recalcula cuando cambia el canalBase de un canal.
     * Alcance: Todos los productos del canal cuyo canalBase cambió.
     */
    @Transactional
    public void recalcularPorCambioCanalBase(Integer canalId) {
        log.info("Recalculando precios por cambio en canalBase del canal: {}", canalId);

        recalcularTodosProductosDelCanal(canalId);
    }

    /**
     * Recalcula todos los productos de un canal.
     * Usado internamente y cuando cambian configuraciones del canal.
     */
    @Transactional
    public void recalcularTodosProductosDelCanal(Integer canalId) {
        log.info("Recalculando todos los precios del canal: {}", canalId);

        // OPTIMIZACIÓN: Usar FETCH JOIN para evitar N+1 al acceder al Producto
        productoCanalPrecioRepository.findByCanalIdWithProductoFetch(canalId)
                .stream()
                .map(ProductoCanalPrecio::getProducto)
                .collect(Collectors.toMap(
                        p -> p.getId(),
                        p -> p,
                        (p1, p2) -> p1  // En caso de duplicados, quedarse con el primero
                ))
                .keySet()
                .forEach(productoId ->
                        calculoPrecioService.recalcularYGuardarPrecioCanalTodasCuotas(productoId, canalId)
                );
    }
}
