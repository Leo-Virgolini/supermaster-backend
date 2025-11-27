package ar.com.leo.super_master_backend.dominio.marca.dto;

import jakarta.validation.constraints.Size;

public record MarcaUpdateDTO(
        @Size(max = 45)
        String nombre,
        Integer padreId
) {
}