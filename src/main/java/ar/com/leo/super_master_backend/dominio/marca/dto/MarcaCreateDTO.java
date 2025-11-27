package ar.com.leo.super_master_backend.dominio.marca.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MarcaCreateDTO(
        @NotNull
        @Size(max = 45)
        String nombre,
        Integer padreId
) {
}