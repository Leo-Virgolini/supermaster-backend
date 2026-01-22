package ar.com.leo.super_master_backend.dominio.promocion.dto;

import ar.com.leo.super_master_backend.dominio.promocion.entity.TipoPromocionTabla;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PromocionCreateDTO(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 20, message = "El código no puede exceder 20 caracteres")
        String codigo,
        @NotNull(message = "El tipo de promoción es obligatorio")
        TipoPromocionTabla tipo,
        @NotNull(message = "El valor es obligatorio")
        @PositiveOrZero(message = "El valor debe ser mayor o igual a 0")
        BigDecimal valor
) {
}
