package ar.com.leo.super_master_backend.dominio.producto.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductoCanalDTO(
        @NotNull Integer canalId,
        @NotNull BigDecimal margenPorcentaje,
        BigDecimal margenFijo,
        BigDecimal margenPromocion,
        BigDecimal margenOferta
) {
}