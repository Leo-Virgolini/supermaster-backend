package ar.com.leo.super_master_backend.dominio.proveedor.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProveedorUpdateDTO(
        @Size(max = 100)
        String proveedor,
        @Size(max = 50)
        String apodo,
        @Size(max = 45)
        String plazoPago,
        Boolean entrega,
        @DecimalMin(value = "0.0", inclusive = true, message = "El porcentaje debe ser mayor o igual a 0")
        @DecimalMax(value = "100.0", inclusive = true, message = "El porcentaje debe ser menor o igual a 100")
        BigDecimal porcentaje
) {
}