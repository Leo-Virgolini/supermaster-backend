package ar.com.leo.super_master_backend.dominio.ml.dto;

import java.math.BigDecimal;

public record CostoVentaResponseDTO(
        String mla,
        String status,
        BigDecimal precioConsultado,
        BigDecimal comisionVenta,
        BigDecimal costoFijo,
        BigDecimal totalCostos,
        String listingTypeId,
        String mensaje
) {}
