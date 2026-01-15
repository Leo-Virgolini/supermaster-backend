package ar.com.leo.super_master_backend.dominio.producto.dto;

import java.math.BigDecimal;
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
        String imagenUrl,
        Integer stock,
        Boolean activo,

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
        LocalDateTime fechaUltCosto,
        BigDecimal iva,

        // Márgenes (de producto_margen)
        BigDecimal margenMinorista,
        BigDecimal margenMayorista,

        // Fechas
        LocalDateTime fechaCreacion,
        LocalDateTime fechaModificacion,

        // Canales con sus precios
        List<CanalPreciosDTO> canales
) {
}
