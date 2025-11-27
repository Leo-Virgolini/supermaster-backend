package ar.com.leo.super_master_backend.dominio.origen.dto;

import jakarta.validation.constraints.Size;

public record OrigenUpdateDTO(
        @Size(max = 45)
        String origen
) {
}