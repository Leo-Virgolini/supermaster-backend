package ar.com.leo.super_master_backend.dominio.impuesto.dto;

import java.math.BigDecimal;

public record ImpuestoUpdateDTO(
        String codigo,
        String nombre,
        BigDecimal porcentaje
) {
}