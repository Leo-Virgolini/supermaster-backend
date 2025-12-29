package ar.com.leo.super_master_backend.dominio.canal.dto;

import java.math.BigDecimal;

public record CanalDTO(
        Integer id,
        String canal,
        Integer canalBaseId,
        BigDecimal porcentajeInflacion
) {
}