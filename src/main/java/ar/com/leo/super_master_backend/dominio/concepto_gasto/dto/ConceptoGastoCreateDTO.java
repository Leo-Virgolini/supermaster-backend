package ar.com.leo.super_master_backend.dominio.concepto_gasto.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ConceptoGastoCreateDTO(
        @NotNull
        @Size(max = 45)
        String concepto,
        @NotNull
        @DecimalMin(value = "0.0", inclusive = true, message = "El porcentaje debe ser mayor o igual a 0")
        @DecimalMax(value = "100.0", inclusive = true, message = "El porcentaje debe ser menor o igual a 100")
        BigDecimal porcentaje, // Almacenado como número: 29 para 29%, no 0.29
        @Pattern(regexp = "COSTO|PVP|COSTO_IVA|AUMENTA_MARGEN_PTS|REDUCE_MARGEN_PTS|AUMENTA_MARGEN_PROP|REDUCE_MARGEN_PROP|IMP|RECARGO_CUPON|DESCUENTO|ENVIO|INFLACION|PROVEEDOR_FIN|COSTO_GANANCIA|IVA|SOBRE_PVP_BASE",
                message = "aplicaSobre debe ser uno de: COSTO, PVP, COSTO_IVA, AUMENTA_MARGEN_PTS, REDUCE_MARGEN_PTS, AUMENTA_MARGEN_PROP, REDUCE_MARGEN_PROP, IMP, RECARGO_CUPON, DESCUENTO, ENVIO, INFLACION, PROVEEDOR_FIN, COSTO_GANANCIA, IVA, SOBRE_PVP_BASE")
        String aplicaSobre,
        @Size(max = 255)
        String descripcion
) {
}