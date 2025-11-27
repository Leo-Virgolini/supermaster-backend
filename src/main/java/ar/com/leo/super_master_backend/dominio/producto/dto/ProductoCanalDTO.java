package ar.com.leo.super_master_backend.dominio.producto.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductoCanalDTO(
        Integer productoId,
        Integer canalId,
        @DecimalMin(value = "0.0", inclusive = true, message = "El margen porcentaje debe ser mayor o igual a 0")
        @DecimalMax(value = "99.99", inclusive = false, message = "El margen porcentaje debe ser menor a 100")
        BigDecimal margenPorcentaje,
        @PositiveOrZero(message = "El margen fijo debe ser mayor o igual a 0")
        BigDecimal margenFijo,
        @DecimalMin(value = "0.0", inclusive = true, message = "El margen promoción debe ser mayor o igual a 0")
        @DecimalMax(value = "100.0", inclusive = true, message = "El margen promoción debe ser menor o igual a 100")
        BigDecimal margenPromocion,
        @DecimalMin(value = "0.0", inclusive = true, message = "El margen oferta debe ser mayor o igual a 0")
        @DecimalMax(value = "100.0", inclusive = true, message = "El margen oferta debe ser menor o igual a 100")
        BigDecimal margenOferta,
        Boolean usaCanalBase,
        Boolean aplicaCuotas,
        Boolean aplicaComision,
        @Size(max = 300)
        String notas
) {
}