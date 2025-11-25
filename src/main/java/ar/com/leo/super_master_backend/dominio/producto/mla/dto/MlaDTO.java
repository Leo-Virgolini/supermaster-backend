package ar.com.leo.super_master_backend.dominio.producto.mla.dto;

import java.math.BigDecimal;

public record MlaDTO(
        Integer id,
        Integer productoId,
        String mla,
        BigDecimal precioEnvio
) {
}