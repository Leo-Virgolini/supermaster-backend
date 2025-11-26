package ar.com.leo.super_master_backend.dominio.producto.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductoCanalPrecioDTO(
        Integer productoId,
        Integer canalId,
        BigDecimal pvp,
        BigDecimal costoTotal,
        BigDecimal gananciaAbs,
        BigDecimal gananciaPorcentaje,
        BigDecimal gastosTotalPorcentaje,
        Instant fechaUltimoCalculo
) {
}