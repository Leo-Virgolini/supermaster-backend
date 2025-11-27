package ar.com.leo.super_master_backend.dominio.tipo.dto;

import jakarta.validation.constraints.Size;

public record TipoUpdateDTO(
        @Size(max = 45)
        String nombre,
        Integer padreId
) {
}