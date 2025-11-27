package ar.com.leo.super_master_backend.dominio.impuesto.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ImpuestoUpdateDTO(
        @Size(max = 45)
        String codigo,
        @Size(max = 45)
        String nombre,
        @DecimalMin(value = "0.0", inclusive = true, message = "El porcentaje debe ser mayor o igual a 0")
        @DecimalMax(value = "100.0", inclusive = true, message = "El porcentaje debe ser menor o igual a 100")
        BigDecimal porcentaje
) {
}