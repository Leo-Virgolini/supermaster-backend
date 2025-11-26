package ar.com.leo.super_master_backend.dominio.producto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProductoFilter(

        // 1) BÚSQUEDA POR TEXTO
        String texto,

        // 2) BOOLEANOS / NUMÉRICOS
        Boolean esCombo,
        Integer uxb,

        // 3) MANY-TO-ONE
        Integer marcaId,
        Integer origenId,
        Integer tipoId,
        Integer clasifGralId,
        Integer clasifGastroId,
        Integer proveedorId,
        Integer materialId,

        // 4) RANGOS (costo / IVA)
        BigDecimal costoMin,
        BigDecimal costoMax,
        BigDecimal ivaMin,
        BigDecimal ivaMax,

        // 5) FECHAS EXISTENTES (fechaUltCosto)
        LocalDate desdeFechaUltCosto,
        LocalDate hastaFechaUltCosto,

        // 6) NUEVO: FECHAS DE CREACIÓN
        LocalDate desdeFechaCreacion,
        LocalDate hastaFechaCreacion,

        // 7) NUEVO: FECHAS DE MODIFICACIÓN
        LocalDate desdeFechaModificacion,
        LocalDate hastaFechaModificacion,

        // 8) MANY-TO-MANY
        List<Integer> aptoIds,
        List<Integer> canalIds,
        List<Integer> catalogoIds,
        List<Integer> clienteIds,
        List<Integer> mlaIds
) {
}