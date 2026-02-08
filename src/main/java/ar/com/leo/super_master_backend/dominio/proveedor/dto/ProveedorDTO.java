package ar.com.leo.super_master_backend.dominio.proveedor.dto;

import java.math.BigDecimal;

public record ProveedorDTO(
        Integer id,
        String proveedor,
        String apodo,
        String plazoPago,
        Boolean entrega,
        BigDecimal financiacionPorcentaje
) {
}