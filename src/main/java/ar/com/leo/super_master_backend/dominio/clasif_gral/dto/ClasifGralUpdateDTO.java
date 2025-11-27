package ar.com.leo.super_master_backend.dominio.clasif_gral.dto;

import jakarta.validation.constraints.Size;

public record ClasifGralUpdateDTO(
        @Size(max = 45)
        String nombre,
        Integer padreId
) {}