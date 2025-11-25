package ar.com.leo.super_master_backend.dominio.producto.calculo.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.Canal;
import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoRepository;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalRepository;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalRepository;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.regla_descuento.entity.ReglaDescuento;
import ar.com.leo.super_master_backend.dominio.regla_descuento.repository.ReglaDescuentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalculoPrecioServiceImpl implements CalculoPrecioService {

    private final ProductoRepository productoRepository;
    private final ProductoCanalRepository productoCanalRepository;
    private final ProductoCanalPrecioRepository productoCanalPrecioRepository;
    private final CanalRepository canalRepository;
    private final CanalConceptoRepository canalConceptoRepository;
    private final ReglaDescuentoRepository reglaDescuentoRepository;

    @Override
    public PrecioCalculadoDTO calcular(Integer idProducto, Integer idCanal) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        ProductoCanal productoCanal = productoCanalRepository
                .findByIdProductoIdAndIdCanalId(idProducto, idCanal)
                .orElseThrow(() -> new RuntimeException("Configuración Producto-Canal no encontrada"));

        Canal canal = canalRepository.findById(idCanal)
                .orElseThrow(() -> new RuntimeException("Canal no encontrado"));

        // 1) costo base
        BigDecimal costoBase = producto.getCosto() != null
                ? producto.getCosto()
                : BigDecimal.ZERO;

        // 2) gastos del canal (conceptos_gastos vinculados al canal)
        List<CanalConcepto> conceptos = canalConceptoRepository.findByIdCanalId(idCanal);
        BigDecimal gastosPorcentaje = calcularGastosPorcentaje(conceptos);

        // 3) margen base del canal (ProductoCanal)
        BigDecimal margenPorcentaje = productoCanal.getMargenPorcentaje() != null
                ? productoCanal.getMargenPorcentaje()
                : BigDecimal.ZERO;

        // 4) aplicar reglas de descuento (reglas_descuentos)
        List<ReglaDescuento> reglas = reglaDescuentoRepository
                .findByIdCanalIdAndActivoTrueOrderByPrioridadAsc(idCanal);
        BigDecimal descuentoPorcentaje = calcularDescuentoPorcentaje(producto, reglas);

        // 5) cálculo del costo total (costo + gastos)
        BigDecimal costoTotal = calcularCostoTotal(costoBase, gastosPorcentaje);

        // 6) cálculo del PVP (en base a costoTotal, margen, descuentos, etc.)
        BigDecimal pvp = calcularPvp(costoTotal, margenPorcentaje, descuentoPorcentaje);

        // 7) cálculo de ganancia absoluta y % sobre costo total
        BigDecimal gananciaAbs = pvp.subtract(costoTotal);
        BigDecimal gananciaPorcentaje = costoTotal.compareTo(BigDecimal.ZERO) > 0
                ? gananciaAbs.divide(costoTotal, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return new PrecioCalculadoDTO(
                pvp,
                costoTotal,
                gananciaAbs,
                gananciaPorcentaje,
                gastosPorcentaje
        );
    }

    @Override
    public PrecioCalculadoDTO recalcularYGuardar(Integer idProducto, Integer idCanal) {
        PrecioCalculadoDTO dto = calcular(idProducto, idCanal);

        // Buscar si ya existe registro de producto_canal_precios
        ProductoCanalPrecio entity = productoCanalPrecioRepository
                .findByProductoIdAndCanalId(idProducto, idCanal)
                .orElseGet(() -> {
                    ProductoCanalPrecio nuevo = new ProductoCanalPrecio();
                    nuevo.setProducto(new Producto(idProducto));
                    nuevo.setCanal(new Canal(idCanal));
                    return nuevo;
                });

        entity.setPvp(dto.pvp());
        entity.setCostoTotal(dto.costoTotal());
        entity.setGananciaAbs(dto.gananciaAbs());
        entity.setGananciaPorcentaje(dto.gananciaPorcentaje());
        entity.setGastosTotalPorcentaje(dto.gastosTotalPorcentaje());
        // fecha_ultimo_calculo la puede setear la DB con default CURRENT_TIMESTAMP

        productoCanalPrecioRepository.save(entity);

        return dto;
    }

    // ==========================
    //  Métodos privados de cálculo
    // ==========================

    /**
     * Suma el porcentaje total de gastos de los conceptos_gastos asociados al canal.
     * Equivale a lo que en el Excel hacés con "conceptos gastos por canal".
     */
    private BigDecimal calcularGastosPorcentaje(List<CanalConcepto> conceptos) {
        return conceptos.stream()
                .map(cc -> cc.getConcepto().getPorcentaje())
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, (bigDecimal, augend) -> bigDecimal.add(augend));
    }

    /**
     * Aplica la lógica de reglas de descuento sobre el producto.
     * Acá deberías replicar la forma en que el Excel elige la regla
     * (por monto mínimo, clasificaciones, catálogo, prioridad, etc.).
     */
    private BigDecimal calcularDescuentoPorcentaje(Producto producto, List<ReglaDescuento> reglas) {
        // TODO: implementar según tu criterio / Excel:
        // - filtrar por catálogo del producto
        // - filtrar por clasif_gral / clasif_gastro
        // - comparar monto_minimo con el subtotal correspondiente
        // - usar la regla de mayor prioridad que matchee
        //
        // Por ahora devolvemos 0 para no romper nada:
        return BigDecimal.ZERO;
    }

    /**
     * Costo total = costo base + gastos sobre costo (o sobre PVP según tus reglas).
     * En el Excel, esto puede depender de si el concepto aplica sobre PVP, costo, etc.
     */
    private BigDecimal calcularCostoTotal(BigDecimal costoBase, BigDecimal gastosPorcentaje) {
        // TODO: si tus conceptos de gastos aplican sobre PVP o sobre costo,
        // acá va la lógica. De momento, asumimos gastos sobre costo:
        BigDecimal factorGastos = BigDecimal.ONE.add(
                gastosPorcentaje.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
        );
        return costoBase.multiply(factorGastos);
    }

    /**
     * Cálculo base del PVP en función de costoTotal, margen y descuento.
     * La fórmula exacta debe venir de tu Excel (lo podés ajustar acá).
     */
    private BigDecimal calcularPvp(BigDecimal costoTotal,
                                   BigDecimal margenPorcentaje,
                                   BigDecimal descuentoPorcentaje) {

        // margen bruto
        BigDecimal factorMargen = BigDecimal.ONE.add(
                margenPorcentaje.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
        );

        BigDecimal pvpBase = costoTotal.multiply(factorMargen);

        // aplicar descuento sobre PVP si corresponde
        if (descuentoPorcentaje != null && descuentoPorcentaje.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal factorDesc = BigDecimal.ONE.subtract(
                    descuentoPorcentaje.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
            );
            return pvpBase.multiply(factorDesc);
        }

        return pvpBase;
    }

}