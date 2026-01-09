package ar.com.leo.super_master_backend.dominio.producto.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductoCreateDTO(
        @NotNull
        @Size(max = 45)
        String sku,
        @Size(max = 45)
        String codExt,
        @NotNull
        @Size(max = 100)
        String descripcion,
        @NotNull
        @Size(max = 100)
        String tituloWeb,
        Boolean esCombo,
        @Positive(message = "UXB debe ser mayor a 0")
        Integer uxb,
        @Size(max = 500)
        String imagenUrl,
        @PositiveOrZero(message = "El stock debe ser mayor o igual a 0")
        Integer stock,
        Boolean activo,

        Integer marcaId,
        @NotNull
        @Positive(message = "El ID de origen debe ser positivo")
        Integer origenId,
        @NotNull
        @Positive(message = "El ID de clasificación general debe ser positivo")
        Integer clasifGralId,
        @Positive(message = "El ID de clasificación gastro debe ser positivo")
        Integer clasifGastroId,
        @NotNull
        @Positive(message = "El ID de tipo debe ser positivo")
        Integer tipoId,
        @Positive(message = "El ID de proveedor debe ser positivo")
        Integer proveedorId,
        @Positive(message = "El ID de material debe ser positivo")
        Integer materialId,

        @Size(max = 45)
        String capacidad,
        @PositiveOrZero(message = "El largo debe ser mayor o igual a 0")
        BigDecimal largo,
        @PositiveOrZero(message = "El ancho debe ser mayor o igual a 0")
        BigDecimal ancho,
        @PositiveOrZero(message = "El alto debe ser mayor o igual a 0")
        BigDecimal alto,
        @Size(max = 45)
        String diamboca,
        @Size(max = 45)
        String diambase,
        @Size(max = 45)
        String espesor,

        @NotNull
        @PositiveOrZero(message = "El costo debe ser mayor o igual a 0")
        BigDecimal costo,
        @NotNull
        @DecimalMin(value = "0.0", inclusive = true, message = "El IVA debe ser mayor o igual a 0")
        @DecimalMax(value = "100.0", inclusive = true, message = "El IVA debe ser menor o igual a 100")
        BigDecimal iva
) {
}
