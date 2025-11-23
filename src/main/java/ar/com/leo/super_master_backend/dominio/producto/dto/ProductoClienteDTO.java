package ar.com.leo.super_master_backend.dominio.producto.dto;

import jakarta.validation.constraints.NotNull;

public record ProductoClienteDTO(
        @NotNull Integer clienteId
) {
}