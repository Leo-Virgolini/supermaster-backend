package ar.com.leo.super_master_backend.dominio.producto.calculo.dto;

import java.math.BigDecimal;
import java.util.List;

public record FormulaCalculoDTO(
        String nombreCanal,
        Integer numeroCuotas,
        String formulaGeneral,
        List<PasoCalculo> pasos,
        BigDecimal resultadoFinal
) {
    public record PasoCalculo(
            Integer numeroPaso,
            String descripcion,
            String formula,
            BigDecimal valor,
            String detalle
    ) {}
}
