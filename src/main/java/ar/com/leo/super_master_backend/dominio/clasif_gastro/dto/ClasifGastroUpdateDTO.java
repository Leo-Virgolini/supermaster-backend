package ar.com.leo.super_master_backend.dominio.clasif_gastro.dto;

import jakarta.validation.constraints.Size;

public record ClasifGastroUpdateDTO(
        @Size(max = 45)
        String nombre,
        Boolean esMaquina,
        Integer padreId
) {
}