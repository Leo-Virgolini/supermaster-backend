package ar.com.leo.super_master_backend.dominio.producto.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductoConPreciosDTO(
        Integer id,
        String sku,
        String codExt,
        String descripcion,
        String tituloWeb,
        BigDecimal costo,
        BigDecimal iva,
        String marcaNombre,
        String origenNombre,
        String tipoNombre,
        String proveedorNombre,
        List<CanalPrecioDTO> preciosCanales
) {
    public record CanalPrecioDTO(
            Integer canalId,
            String canalNombre,
            BigDecimal pvp,
            BigDecimal costoTotal,
            BigDecimal gananciaAbs,
            BigDecimal gananciaPorcentaje,
            BigDecimal gastosTotalPorcentaje,
            Instant fechaUltimoCalculo
    ) {}
}
