package ar.com.leo.super_master_backend.dominio.cliente.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClienteCreateDTO(
        @NotNull
        @Size(max = 45)
        String cliente
) {}