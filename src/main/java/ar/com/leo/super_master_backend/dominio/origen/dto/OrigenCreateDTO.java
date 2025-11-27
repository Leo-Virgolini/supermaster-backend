package ar.com.leo.super_master_backend.dominio.origen.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrigenCreateDTO(
        @NotNull
        @Size(max = 45)
        String origen
) {
}