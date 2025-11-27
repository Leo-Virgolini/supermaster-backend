package ar.com.leo.super_master_backend.dominio.clasif_gral.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClasifGralCreateDTO(
        @NotNull
        @Size(max = 45)
        String nombre,
        Integer padreId
) {}