package ar.com.leo.super_master_backend.dominio.producto.dto;

import java.math.BigDecimal;

public record ProductoResumenDTO(
        Integer id,
        String sku,
        String descripcion,
        BigDecimal costo,
        BigDecimal iva
) {
}
