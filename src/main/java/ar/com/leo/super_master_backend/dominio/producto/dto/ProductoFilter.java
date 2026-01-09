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
        Boolean esMaquina,
        Boolean tieneMla,
        Boolean activo,

        // 3) MANY-TO-ONE
        Integer marcaId,
        Integer origenId,
        Integer tipoId,
        Integer clasifGralId,
        Integer clasifGastroId,
        Integer proveedorId,
        Integer materialId,

        // 4) RANGOS (costo / IVA / stock)
        BigDecimal costoMin,
        BigDecimal costoMax,
        BigDecimal ivaMin,
        BigDecimal ivaMax,
        Integer stockMin,
        Integer stockMax,

        // 5) RANGOS PVP (requiere pvpCanalId)
        BigDecimal pvpMin,
        BigDecimal pvpMax,
        Integer pvpCanalId,

        // 6) FECHAS EXISTENTES (fechaUltCosto)
        LocalDate desdeFechaUltCosto,
        LocalDate hastaFechaUltCosto,

        // 7) FECHAS DE CREACIÓN
        LocalDate desdeFechaCreacion,
        LocalDate hastaFechaCreacion,

        // 8) FECHAS DE MODIFICACIÓN
        LocalDate desdeFechaModificacion,
        LocalDate hastaFechaModificacion,

        // 9) MANY-TO-MANY
        List<Integer> aptoIds,
        List<Integer> canalIds,
        List<Integer> catalogoIds,
        List<Integer> clienteIds,
        List<Integer> mlaIds,

        // 10) ORDENAMIENTO ESPECIAL
        String sortBy,       // "pvp", "costo", "mla", "esMaquina"
        String sortDir,      // "asc", "desc"
        Integer sortCanalId  // canal para ordenar por PVP
) {
}