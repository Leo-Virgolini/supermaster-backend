package ar.com.leo.super_master_backend.dominio.producto.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ProductoCanalPromocionCreateDTO(
        @NotNull(message = "El ID de producto es obligatorio")
        @Positive(message = "El ID de producto debe ser positivo")
        Integer productoId,
        @NotNull(message = "El ID de canal es obligatorio")
        @Positive(message = "El ID de canal debe ser positivo")
        Integer canalId,
        @NotNull(message = "El ID de promoción es obligatorio")
        @Positive(message = "El ID de promoción debe ser positivo")
        Integer promocionId,
        Boolean activa,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        @Size(max = 255, message = "Las notas no pueden exceder 255 caracteres")
        String notas
) {
}
