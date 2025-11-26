package ar.com.leo.super_master_backend.dominio.producto.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductoDTO(
        Integer id,
        String sku,
        String codExt,
        String descripcion,
        String tituloWeb,
        Boolean esCombo,
        Integer uxb,

        // Relaciones: solo IDs
        Integer marcaId,
        Integer origenId,
        Integer clasifGralId,
        Integer clasifGastroId,
        Integer tipoId,
        Integer proveedorId,
        Integer materialId,

        // Atributos extra
        String capacidad,
        BigDecimal largo,
        BigDecimal ancho,
        BigDecimal alto,
        String diamboca,
        String diambase,
        String espesor,
        BigDecimal costo,
        Instant fechaUltCosto,
        BigDecimal iva,

        // NUEVAS FECHAS
        Instant fechaCreacion,
        Instant fechaModificacion
) {
}