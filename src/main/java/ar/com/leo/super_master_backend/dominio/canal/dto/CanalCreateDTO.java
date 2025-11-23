package ar.com.leo.super_master_backend.dominio.canal.dto;

import jakarta.validation.constraints.NotNull;

public record CanalCreateDTO(
        @NotNull String canal,
        Integer canalBaseId
) {
}