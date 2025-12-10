package ar.com.leo.super_master_backend.dominio.concepto_gasto.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ConceptoGastoCreateDTO(
        @NotNull
        @Size(max = 45)
        String concepto,
        @NotNull
        @DecimalMin(value = "0.0", inclusive = true, message = "El porcentaje debe ser mayor o igual a 0")
        @DecimalMax(value = "100.0", inclusive = true, message = "El porcentaje debe ser menor o igual a 100")
        BigDecimal porcentaje, // Almacenado como número: 29 para 29%, no 0.29
        @Pattern(regexp = "COSTO|PVP|COSTO_IVA|COSTO_MARGEN|IMP", 
                 message = "aplicaSobre debe ser uno de: COSTO, PVP, COSTO_IVA, COSTO_MARGEN, IMP")
        String aplicaSobre,
        @Size(max = 2)
        String cuotas
) {
}