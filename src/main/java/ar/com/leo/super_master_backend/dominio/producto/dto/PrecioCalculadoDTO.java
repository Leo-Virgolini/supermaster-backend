package ar.com.leo.super_master_backend.dominio.producto.dto;

import java.math.BigDecimal;

public record PrecioCalculadoDTO(
        BigDecimal pvp,
        BigDecimal costoTotal,
        BigDecimal gastos,
        BigDecimal gananciaAbs,
        BigDecimal gananciaPorcentaje,
        BigDecimal comision,
        String canal
) {
}