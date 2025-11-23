package ar.com.leo.super_master_backend.dominio.producto.dto;

import java.math.BigDecimal;

public record ProductoUpdateDTO(
        String sku,
        String codExt,
        String descripcion,
        String tituloWeb,
        Boolean esCombo,
        Integer uxb,

        Integer marcaId,
        Integer origenId,
        Integer clasifGralId,
        Integer clasifGastroId,
        Integer tipoId,
        Integer proveedorId,
        Integer materialId,

        String capacidad,
        BigDecimal largo,
        BigDecimal ancho,
        BigDecimal alto,
        String diamboca,
        String diambase,
        String espesor,

        BigDecimal costo,
        BigDecimal iva
) {
}
