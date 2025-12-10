package ar.com.leo.super_master_backend.dominio.proveedor.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProveedorCreateDTO(
        @NotNull
        @Size(max = 100)
        String proveedor,
        @NotNull
        @Size(max = 50)
        String apodo,
        @Size(max = 45)
        String plazoPago,
        Boolean entrega,
        BigDecimal porcentaje
) {
}