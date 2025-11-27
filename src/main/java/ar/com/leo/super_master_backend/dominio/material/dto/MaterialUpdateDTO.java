package ar.com.leo.super_master_backend.dominio.material.dto;

import jakarta.validation.constraints.Size;

public record MaterialUpdateDTO(
        @Size(max = 45)
        String material
) {
}