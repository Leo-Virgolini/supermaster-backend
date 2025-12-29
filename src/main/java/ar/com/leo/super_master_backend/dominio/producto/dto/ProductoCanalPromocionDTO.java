package ar.com.leo.super_master_backend.dominio.producto.dto;

import ar.com.leo.super_master_backend.dominio.promocion.dto.PromocionDTO;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ProductoCanalPromocionDTO(
        Integer id,
        Integer productoId,
        Integer canalId,
        @NotNull(message = "La promoción es obligatoria")
        PromocionDTO promocion,
        Boolean activa,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        @Size(max = 255, message = "Las notas no pueden exceder 255 caracteres")
        String notas
) {
}
