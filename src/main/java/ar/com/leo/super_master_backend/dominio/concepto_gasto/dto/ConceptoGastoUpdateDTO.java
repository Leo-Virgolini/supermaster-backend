package ar.com.leo.super_master_backend.dominio.concepto_gasto.dto;

import java.math.BigDecimal;

public record ConceptoGastoUpdateDTO(
        String concepto,
        BigDecimal porcentaje,
        String aplicaSobre,
        String cuotas
) {
}