package ar.com.leo.super_master_backend.dominio.producto.dto;

import jakarta.validation.constraints.NotNull;

public record ProductoCatalogoDTO(
        @NotNull Integer catalogoId
) {
}