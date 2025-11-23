package ar.com.leo.super_master_backend.dominio.producto.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductoCreateDTO(
        @NotNull String sku,
        String codExt,
        @NotNull String descripcion,
        @NotNull String tituloWeb,
        Boolean esCombo,
        Integer uxb,

        @NotNull Integer marcaId,
        @NotNull Integer origenId,
        @NotNull Integer clasifGralId,
        Integer clasifGastroId,
        @NotNull Integer tipoId,
        Integer proveedorId,
        Integer materialId,

        String capacidad,
        BigDecimal largo,
        BigDecimal ancho,
        BigDecimal alto,
        String diamboca,
        String diambase,
        String espesor,

        @NotNull BigDecimal costo,
        @NotNull BigDecimal iva
) {
}
