package ar.com.leo.super_master_backend.dominio.producto.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoCanalPrecioDTO(
        Integer productoId,
        Integer canalId,
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