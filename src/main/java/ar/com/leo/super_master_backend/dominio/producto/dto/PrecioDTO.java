package ar.com.leo.super_master_backend.dominio.producto.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para un precio individual (sin información del canal).
 * Se usa dentro de CanalPreciosDTO para agrupar precios por canal.
 */
public record PrecioDTO(
        Integer cuotas,
        String descripcion,
        BigDecimal pvp,
        BigDecimal pvpInflado,
        BigDecimal costoProducto,
        BigDecimal costosVenta,
        BigDecimal ingresoNetoVendedor,
        BigDecimal ganancia,
        BigDecimal margenSobreIngresoNeto,
        BigDecimal margenSobrePvp,
        BigDecimal markupPorcentaje,
        LocalDateTime fechaUltimoCalculo
) {
}
