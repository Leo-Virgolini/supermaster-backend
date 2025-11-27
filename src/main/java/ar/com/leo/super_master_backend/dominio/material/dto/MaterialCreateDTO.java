package ar.com.leo.super_master_backend.dominio.material.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MaterialCreateDTO(
        @NotNull
        @Size(max = 45)
        String material
) {
}