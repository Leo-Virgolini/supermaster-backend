package ar.com.leo.super_master_backend.dominio.producto.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para un precio individual (sin información del canal).
 * Se usa dentro de CanalPreciosDTO para agrupar precios por canal.
 */
public record PrecioDTO(
        Integer cuotas,
        BigDecimal pvp,
        BigDecimal pvpInflado,
        BigDecimal costoTotal,
        BigDecimal gananciaAbs,
        BigDecimal gananciaPorcentaje,
        BigDecimal markupPorcentaje,
        LocalDateTime fechaUltimoCalculo
) {
}
