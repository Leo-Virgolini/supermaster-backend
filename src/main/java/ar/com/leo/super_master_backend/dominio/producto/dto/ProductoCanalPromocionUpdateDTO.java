package ar.com.leo.super_master_backend.dominio.producto.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ProductoCanalPromocionUpdateDTO(
        @Positive(message = "El ID de promoción debe ser positivo")
        Integer promocionId,
        Boolean activa,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        @Size(max = 255, message = "Las notas no pueden exceder 255 caracteres")
        String notas
) {
}
