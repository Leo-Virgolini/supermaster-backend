package ar.com.leo.super_master_backend.dominio.catalogo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CatalogoCreateDTO(
        @NotNull
        @Size(max = 45)
        String catalogo,
        Boolean exportarConIva,
        @DecimalMin(value = "0.0", inclusive = true, message = "El recargo porcentaje debe ser mayor o igual a 0")
        @DecimalMax(value = "100.0", inclusive = true, message = "El recargo porcentaje debe ser menor o igual a 100")
        BigDecimal recargoPorcentaje
) {}