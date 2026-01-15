package ar.com.leo.super_master_backend.dominio.excel.dto;

import java.util.List;

public record ExportResultDTO(
        byte[] archivo,
        List<String> advertencias
) {
    public static ExportResultDTO of(byte[] archivo) {
        return new ExportResultDTO(archivo, List.of());
    }

    public static ExportResultDTO of(byte[] archivo, List<String> advertencias) {
        return new ExportResultDTO(archivo, advertencias);
    }

    public boolean tieneAdvertencias() {
        return advertencias != null && !advertencias.isEmpty();
    }
}
