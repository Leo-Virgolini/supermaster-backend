package ar.com.leo.super_master_backend.dominio.producto.calculo.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoRepository;
import ar.com.leo.super_master_backend.dominio.concepto_gasto.entity.AplicaSobre;
import ar.com.leo.super_master_backend.dominio.producto.calculo.dto.PrecioCalculadoDTO;
import ar.com.leo.super_master_backend.dominio.producto.entity.Producto;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanal;
import ar.com.leo.super_master_backend.dominio.producto.entity.ProductoCanalPrecio;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalPrecioRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoCanalRepository;
import ar.com.leo.super_master_backend.dominio.producto.repository.ProductoRepository;
import ar.com.leo.super_master_backend.dominio.common.exception.NotFoundException;
import ar.com.leo.super_master_backend.dominio.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalculoPrecioServiceImpl implements CalculoPrecioService {

    private final ProductoRepository productoRepository;
    private final ProductoCanalRepository productoCanalRepository;
    private final ProductoCanalPrecioRepository productoCanalPrecioRepository;
    private final CanalConceptoRepository canalConceptoRepository;

    // ====================================================
    //  API PÚBLICA
    // ====================================================

    @Override
    @Transactional(readOnly = true)
    public PrecioCalculadoDTO calcularPrecioCanal(Integer idProducto, Integer idCanal) {
        Producto producto = obtenerProducto(idProducto);
        ProductoCanal productoCanal = obtenerProductoCanal(idProducto, idCanal);
        List<CanalConcepto> conceptos = canalConceptoRepository.findByCanalId(idCanal);

        return calcularPrecioInterno(producto, productoCanal, conceptos);
    }

    @Override
    @Transactional
    public PrecioCalculadoDTO recalcularYGuardarPrecioCanal(Integer idProducto, Integer idCanal) {
        Producto producto = obtenerProducto(idProducto);
        ProductoCanal productoCanal = obtenerProductoCanal(idProducto, idCanal);
        List<CanalConcepto> conceptos = canalConceptoRepository.findByCanalId(idCanal);

        PrecioCalculadoDTO dto = calcularPrecioInterno(producto, productoCanal, conceptos);

        // Persistimos/actualizamos en producto_canal_precios
        ProductoCanalPrecio pcp = productoCanalPrecioRepository
                .findByProductoIdAndCanalId(idProducto, idCanal)
                .orElseGet(ProductoCanalPrecio::new);

        if (pcp.getId() == null) {
            pcp.setProducto(producto);
            pcp.setCanal(productoCanal.getCanal());
        }

        pcp.setPvp(dto.pvp());
        pcp.setCostoTotal(dto.costoTotal());
        pcp.setGananciaAbs(dto.gananciaAbs());
        pcp.setGananciaPorcentaje(dto.gananciaPorcentaje());
        pcp.setGastosTotalPorcentaje(dto.gastosTotalPorcentaje());

        productoCanalPrecioRepository.save(pcp);

        return dto;
    }

    // ====================================================
    //  LÓGICA DE CÁLCULO
    // ====================================================

    /**
     * Calcula el precio de un producto para un canal basándose en la lógica del Excel.
     * 
     * Fórmula principal del Excel:
     * PVP = ((COSTO + COSTO * ganancia) * IMP) / (1 - gastosSobrePVP) / (1 - descuentos)
     * 
     * Donde:
     * - IMP = 1 + IVA/100 (factor de impuestos)
     * - gastosSobrePVP = suma de conceptos que se aplican sobre PVP
     * - descuentos = cupones y otros descuentos
     */
    private PrecioCalculadoDTO calcularPrecioInterno(
            Producto producto,
            ProductoCanal productoCanal,
            List<CanalConcepto> conceptos
    ) {
        if (producto.getCosto() == null) {
            throw new BadRequestException("El producto no tiene costo cargado");
        }

        if (producto.getIva() == null) {
            throw new BadRequestException("El producto no tiene IVA cargado");
        }

        BigDecimal costo = producto.getCosto();
        BigDecimal iva = producto.getIva();
        BigDecimal cien = BigDecimal.valueOf(100);

        // 1) Calcular factor de impuestos (IMP = 1 + IVA/100)
        BigDecimal imp = BigDecimal.ONE.add(iva.divide(cien, 6, RoundingMode.HALF_UP));

        // 2) Obtener margen base del canal
        BigDecimal margenPorcentaje = productoCanal.getMargenPorcentaje();
        if (margenPorcentaje == null) {
            margenPorcentaje = BigDecimal.ZERO;
        }

        // 3) Aplicar márgenes adicionales si existen
        BigDecimal margenFijo = productoCanal.getMargenFijo() != null ? productoCanal.getMargenFijo() : BigDecimal.ZERO;
        BigDecimal margenPromocion = productoCanal.getMargenPromocion() != null ? productoCanal.getMargenPromocion() : BigDecimal.ZERO;
        BigDecimal margenOferta = productoCanal.getMargenOferta() != null ? productoCanal.getMargenOferta() : BigDecimal.ZERO;

        // 4) Separar conceptos de gasto según aplicaSobre
        List<CanalConcepto> gastosSobreCosto = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO)
                .collect(Collectors.toList());

        List<CanalConcepto> gastosSobreCostoMargen = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO_MARGEN)
                .collect(Collectors.toList());

        List<CanalConcepto> gastosSobreCostoIva = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.COSTO_IVA)
                .collect(Collectors.toList());

        List<CanalConcepto> gastosSobrePVP = conceptos.stream()
                .filter(cc -> cc.getConcepto().getAplicaSobre() == AplicaSobre.PVP)
                .collect(Collectors.toList());

        // 5) Calcular gastos sobre COSTO (se suman al costo base)
        BigDecimal gastosSobreCostoTotal = calcularGastosPorcentaje(gastosSobreCosto);
        BigDecimal costoConGastos = costo.multiply(
                BigDecimal.ONE.add(gastosSobreCostoTotal.divide(cien, 6, RoundingMode.HALF_UP))
        );

        // 6) Calcular costo con ganancia (margen porcentual)
        BigDecimal margenFrac = margenPorcentaje.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal costoConGanancia = costoConGastos.multiply(BigDecimal.ONE.add(margenFrac));

        // 7) Aplicar gastos sobre COSTO_MARGEN
        BigDecimal gastosSobreCostoMargenTotal = calcularGastosPorcentaje(gastosSobreCostoMargen);
        costoConGanancia = costoConGanancia.multiply(
                BigDecimal.ONE.add(gastosSobreCostoMargenTotal.divide(cien, 6, RoundingMode.HALF_UP))
        );

        // 8) Aplicar impuestos (IVA)
        BigDecimal costoConImpuestos = costoConGanancia.multiply(imp);

        // 9) Aplicar gastos sobre COSTO_IVA
        BigDecimal gastosSobreCostoIvaTotal = calcularGastosPorcentaje(gastosSobreCostoIva);
        costoConImpuestos = costoConImpuestos.multiply(
                BigDecimal.ONE.add(gastosSobreCostoIvaTotal.divide(cien, 6, RoundingMode.HALF_UP))
        );

        // 10) Calcular gastos totales sobre PVP (para el denominador)
        BigDecimal gastosSobrePVPTotal = calcularGastosPorcentaje(gastosSobrePVP);
        BigDecimal gastosSobrePVPFrac = gastosSobrePVPTotal.divide(cien, 6, RoundingMode.HALF_UP);

        // 11) Calcular PVP base: PVP = costoConImpuestos / (1 - gastosSobrePVP)
        BigDecimal denominador = BigDecimal.ONE.subtract(gastosSobrePVPFrac);
        if (denominador.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Los gastos sobre PVP son >= 100%, lo cual es inválido");
        }

        BigDecimal pvpBase = costoConImpuestos.divide(denominador, 6, RoundingMode.HALF_UP);

        // 12) Aplicar márgenes adicionales (fijo, promoción, oferta)
        // Estos se aplican como incrementos sobre el PVP base
        BigDecimal pvp = pvpBase;
        if (margenFijo.compareTo(BigDecimal.ZERO) > 0) {
            pvp = pvp.add(margenFijo);
        }
        if (margenPromocion.compareTo(BigDecimal.ZERO) > 0) {
            pvp = pvp.multiply(BigDecimal.ONE.add(margenPromocion.divide(cien, 6, RoundingMode.HALF_UP)));
        }
        if (margenOferta.compareTo(BigDecimal.ZERO) > 0) {
            pvp = pvp.multiply(BigDecimal.ONE.add(margenOferta.divide(cien, 6, RoundingMode.HALF_UP)));
        }

        // Redondear PVP final
        pvp = pvp.setScale(2, RoundingMode.HALF_UP);

        // 13) Calcular costo total (costo base + gastos sobre costo)
        BigDecimal costoTotal = costoConGastos.setScale(2, RoundingMode.HALF_UP);

        // 14) Calcular ganancia absoluta y porcentaje
        BigDecimal gananciaAbs = pvp.subtract(costoConImpuestos).setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal gananciaPorcentaje = BigDecimal.ZERO;
        if (costoConImpuestos.compareTo(BigDecimal.ZERO) > 0) {
            gananciaPorcentaje = gananciaAbs
                    .multiply(cien)
                    .divide(costoConImpuestos, 2, RoundingMode.HALF_UP);
        }

        // 15) Calcular gastos totales porcentaje (suma de todos los gastos)
        BigDecimal gastosTotalPorcentaje = gastosSobreCostoTotal
                .add(gastosSobreCostoMargenTotal)
                .add(gastosSobreCostoIvaTotal)
                .add(gastosSobrePVPTotal);

        return new PrecioCalculadoDTO(
                pvp,
                costoTotal,
                gananciaAbs,
                gananciaPorcentaje,
                gastosTotalPorcentaje
        );
    }

    /**
     * Suma el porcentaje total de gastos de una lista de conceptos.
     */
    private BigDecimal calcularGastosPorcentaje(List<CanalConcepto> conceptos) {
        return conceptos.stream()
                .map(cc -> cc.getConcepto().getPorcentaje())
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ====================================================
    //  HELPERS
    // ====================================================

    private Producto obtenerProducto(Integer idProducto) {
        return productoRepository.findById(idProducto)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
    }

    private ProductoCanal obtenerProductoCanal(Integer idProducto, Integer idCanal) {
        return productoCanalRepository
                .findByProductoIdAndCanalId(idProducto, idCanal)
                .orElseThrow(() -> new NotFoundException("No existe configuración de canal para este producto"));
    }

}