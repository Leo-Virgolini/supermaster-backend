package ar.com.leo.super_master_backend.dominio.producto.calculo.dto;

import java.time.LocalDateTime;

public record RecalculoMasivoDTO(
        int preciosRecalculados,
        LocalDateTime fechaEjecucion
) {
}
