package ar.com.leo.super_master_backend.dominio.producto.calculo.service;

import ar.com.leo.super_master_backend.dominio.canal.entity.CanalConcepto;
import ar.com.leo.super_master_backend.dominio.canal.repository.CanalConceptoRepository;
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

    private PrecioCalculadoDTO calcularPrecioInterno(
            Producto producto,
            ProductoCanal productoCanal,
            List<CanalConcepto> conceptos
    ) {
        if (producto.getCosto() == null) {
            throw new BadRequestException("El producto no tiene costo cargado");
        }

        BigDecimal costo = producto.getCosto();

        // 1) Gastos totales (%), a partir de conceptos_gastos asociados al canal
        BigDecimal gastosTotalPorcentaje = calcularGastosPorcentaje(conceptos);

        // 2) Margen propio del canal
        BigDecimal margenPorcentaje = productoCanal.getMargenPorcentaje();
        if (margenPorcentaje == null) {
            margenPorcentaje = BigDecimal.ZERO;
        }

        BigDecimal cien = BigDecimal.valueOf(100);

        BigDecimal gastosFrac = gastosTotalPorcentaje.divide(cien, 6, RoundingMode.HALF_UP);
        BigDecimal margenFrac = margenPorcentaje.divide(cien, 6, RoundingMode.HALF_UP);

        // 3) Costo total = costo * (1 + gastos%)
        BigDecimal costoTotal = costo
                .multiply(BigDecimal.ONE.add(gastosFrac))
                .setScale(2, RoundingMode.HALF_UP);

        // 4) PVP = costoTotal / (1 - margen%)
        BigDecimal denominador = BigDecimal.ONE.subtract(margenFrac);
        if (denominador.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Margen inválido (>= 100%) para el canal");
        }

        BigDecimal pvp = costoTotal
                .divide(denominador, 2, RoundingMode.HALF_UP);

        // 5) Ganancia absoluta y %
        BigDecimal gananciaAbs = pvp
                .subtract(costoTotal)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal gananciaPorcentaje = gananciaAbs
                .multiply(cien)
                .divide(costoTotal, 2, RoundingMode.HALF_UP);

        return new PrecioCalculadoDTO(
                pvp,
                costoTotal,
                gananciaAbs,
                gananciaPorcentaje,
                gastosTotalPorcentaje
        );
    }

    /**
     * Suma el porcentaje total de gastos de los conceptos_gastos asociados al canal.
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