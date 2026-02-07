package ar.com.leo.super_master_backend.apis.ml.dto;

import java.time.LocalDateTime;

public record ProcesoMasivoEstadoDTO(
        boolean enEjecucion,
        int total,
        int procesados,
        int exitosos,
        int errores,
        String estado,  // "idle", "ejecutando", "completado", "cancelado"
        LocalDateTime iniciadoEn,
        LocalDateTime finalizadoEn,
        String mensaje
) {
    public static ProcesoMasivoEstadoDTO idle() {
        return new ProcesoMasivoEstadoDTO(
                false, 0, 0, 0, 0, "idle", null, null,
                "No hay proceso en ejecución");
    }

    public static ProcesoMasivoEstadoDTO iniciado(int total, LocalDateTime iniciadoEn) {
        return new ProcesoMasivoEstadoDTO(
                true, total, 0, 0, 0, "ejecutando", iniciadoEn, null,
                "Proceso iniciado");
    }
}
