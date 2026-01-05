package ar.com.leo.super_master_backend.dominio.canal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CanalCreateDTO(
        @NotNull
        @Size(max = 45)
        String canal,
        Integer canalBaseId
) {
}