package ar.com.leo.super_master_backend.dominio.producto.dto;

import java.math.BigDecimal;

public record ProductoCanalDTO(
        Integer productoId,
        Integer canalId,
        BigDecimal margenPorcentaje,
        BigDecimal margenFijo,
        BigDecimal margenPromocion,
        BigDecimal margenOferta,
        Boolean usaCanalBase,
        Boolean aplicaCuotas,
        Boolean aplicaComision,
        String notas
) {
}