package ar.com.leo.super_master_backend.dominio.canal.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CanalConceptoCuotaCreateDTO(
        @NotNull(message = "El ID del canal es obligatorio")
        @Positive(message = "El ID del canal debe ser positivo")
        Integer canalId,

        @NotNull(message = "El número de cuotas es obligatorio")
        @Positive(message = "El número de cuotas debe ser positivo")
        Integer cuotas,

        @Pattern(regexp = "NORMAL|PROMO",
                 message = "tipo debe ser uno de: NORMAL, PROMO")
        String tipo,

        @NotNull(message = "El porcentaje es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El porcentaje debe ser mayor o igual a 0")
        @DecimalMax(value = "100.0", inclusive = true, message = "El porcentaje debe ser menor o igual a 100")
        BigDecimal porcentaje // Almacenado como número: 29 para 29%, no 0.29
) {
}

