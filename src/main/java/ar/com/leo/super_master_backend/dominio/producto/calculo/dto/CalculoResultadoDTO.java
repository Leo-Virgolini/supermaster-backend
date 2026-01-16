package ar.com.leo.super_master_backend.dominio.producto.calculo.dto;

import ar.com.leo.super_master_backend.dominio.producto.dto.CanalPreciosDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO unificado para respuestas de cálculo de precios.
 * Puede representar:
 * - Cálculo de un producto en un canal específico
 * - Cálculo de un producto en todos sus canales
 * - Cálculo masivo de todos los productos
 */
public record CalculoResultadoDTO(
        Integer totalPreciosCalculados,
        LocalDateTime fechaCalculo,
        List<CanalPreciosDTO> canales
) {
    /**
     * Crea resultado para un solo canal.
     */
    public static CalculoResultadoDTO of(CanalPreciosDTO canal) {
        return new CalculoResultadoDTO(
                canal.precios().size(),
                LocalDateTime.now(),
                List.of(canal)
        );
    }

    /**
     * Crea resultado para múltiples canales.
     */
    public static CalculoResultadoDTO of(List<CanalPreciosDTO> canales) {
        int total = canales.stream().mapToInt(c -> c.precios().size()).sum();
        return new CalculoResultadoDTO(total, LocalDateTime.now(), canales);
    }

    /**
     * Crea resultado para cálculo masivo (sin detalle de canales).
     */
    public static CalculoResultadoDTO masivo(int total) {
        return new CalculoResultadoDTO(total, LocalDateTime.now(), null);
    }
}
