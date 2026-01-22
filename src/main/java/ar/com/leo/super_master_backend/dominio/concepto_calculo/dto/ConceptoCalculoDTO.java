package ar.com.leo.super_master_backend.dominio.concepto_calculo.dto;

import java.math.BigDecimal;

public record ConceptoCalculoDTO(
        Integer id,
        String concepto,
        BigDecimal porcentaje,
        String aplicaSobre,
        String descripcion
) {
}
