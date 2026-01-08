package ar.com.leo.super_master_backend.dominio.producto.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoCanalPrecioDTO(
        Integer productoId,
        Integer canalId,
        BigDecimal pvp,
        BigDecimal costoTotal,
        BigDecimal gananciaAbs,
        BigDecimal gananciaPorcentaje,
        BigDecimal gananciaRealPorcentaje,
        BigDecimal gastosTotalPorcentaje,
        LocalDateTime fechaUltimoCalculo
) {
}