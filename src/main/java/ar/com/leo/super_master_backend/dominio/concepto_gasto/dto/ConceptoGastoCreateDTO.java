package ar.com.leo.super_master_backend.dominio.concepto_gasto.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ConceptoGastoCreateDTO(
        @NotNull
        @Size(max = 45)
        String concepto,
        @NotNull
        @DecimalMin(value = "-100.0", inclusive = true, message = "El porcentaje debe ser mayor o igual a -100")
        @DecimalMax(value = "100.0", inclusive = true, message = "El porcentaje debe ser menor o igual a 100")
        BigDecimal porcentaje, // Almacenado como número: 29 para 29%, -20 para -20%. Signo negativo para MARGEN_PTS/MARGEN_PROP indica reducción.
        @Pattern(regexp = "COSTO|PVP|COSTO_IVA|MARGEN_PTS|MARGEN_PROP|IMP|RECARGO_CUPON|DESCUENTO|ENVIO|INFLACION|PROVEEDOR_FIN|COSTO_GANANCIA|IVA|SOBRE_PVP_BASE|MARGEN_MINORISTA|MARGEN_MAYORISTA|PROMOCION",
                message = "aplicaSobre debe ser uno de: COSTO, PVP, COSTO_IVA, MARGEN_PTS, MARGEN_PROP, IMP, RECARGO_CUPON, DESCUENTO, ENVIO, INFLACION, PROVEEDOR_FIN, COSTO_GANANCIA, IVA, SOBRE_PVP_BASE, MARGEN_MINORISTA, MARGEN_MAYORISTA, PROMOCION")
        String aplicaSobre,
        @Size(max = 255)
        String descripcion
) {
}