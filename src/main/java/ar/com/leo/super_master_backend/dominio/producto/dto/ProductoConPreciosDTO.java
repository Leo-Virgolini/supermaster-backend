package ar.com.leo.super_master_backend.dominio.producto.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record ProductoConPreciosDTO(
        // Identificación
        Integer id,
        String sku,

        // MLA
        String mla,
        String mlau,
        BigDecimal precioEnvio,

        String codExt,
        String descripcion,
        String tituloWeb,
        Boolean esCombo,
        Boolean esMaquina,

        // Relaciones (nombres)
        String marcaNombre,
        String origenNombre,
        String clasifGralNombre,
        String clasifGastroNombre,
        String tipoNombre,
        String proveedorNombre,
        String materialNombre,

        // Dimensiones y atributos
        Integer uxb,
        String capacidad,
        BigDecimal largo,
        BigDecimal ancho,
        BigDecimal alto,
        String diamboca,
        String diambase,
        String espesor,

        // Precios y costos
        BigDecimal costo,
        Instant fechaUltCosto,
        BigDecimal iva,

        // Fechas
        Instant fechaCreacion,
        Instant fechaModificacion,

        // Precios por canal
        List<CanalPrecioDTO> preciosCanales
) {
    public record CanalPrecioDTO(
            Integer canalId,
            String canalNombre,
            Integer cuotas,
            BigDecimal pvp,
            BigDecimal pvpInflado,
            BigDecimal costoTotal,
            BigDecimal gananciaAbs,
            BigDecimal gananciaPorcentaje,
            BigDecimal gananciaRealPorcentaje,
            BigDecimal gastosTotalPorcentaje,
            LocalDateTime fechaUltimoCalculo
    ) {
    }
}
