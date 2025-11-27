package ar.com.leo.super_master_backend.dominio.apto.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AptoCreateDTO(
        @NotNull
        @Size(max = 45)
        String apto
) {
}

