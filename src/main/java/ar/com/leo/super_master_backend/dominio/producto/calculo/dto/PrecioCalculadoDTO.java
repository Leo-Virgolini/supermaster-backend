package ar.com.leo.super_master_backend.dominio.producto.calculo.dto;

import java.math.BigDecimal;

public record PrecioCalculadoDTO(
        BigDecimal pvp,
        BigDecimal pvpInflado,
        BigDecimal costoTotal,
        BigDecimal gananciaAbs,
        BigDecimal gananciaPorcentaje,
        BigDecimal gastosTotalPorcentaje
) {
}