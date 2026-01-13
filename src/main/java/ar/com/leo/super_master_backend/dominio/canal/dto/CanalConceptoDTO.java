package ar.com.leo.super_master_backend.dominio.canal.dto;

import java.math.BigDecimal;

public record CanalConceptoDTO(
        Integer canalId,
        Integer conceptoId,
        String concepto,
        BigDecimal porcentaje,
        String aplicaSobre,
        String descripcion
) {
}