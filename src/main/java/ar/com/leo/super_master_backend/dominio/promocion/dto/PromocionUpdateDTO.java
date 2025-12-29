package ar.com.leo.super_master_backend.dominio.promocion.dto;

import ar.com.leo.super_master_backend.dominio.promocion.entity.TipoPromocionTabla;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PromocionUpdateDTO(
        @Size(max = 20, message = "El código no puede exceder 20 caracteres")
        String codigo,
        TipoPromocionTabla tipo,
        @PositiveOrZero(message = "El valor debe ser mayor o igual a 0")
        BigDecimal valor
) {
}
