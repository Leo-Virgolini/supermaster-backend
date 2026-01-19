package ar.com.leo.super_master_backend.dominio.excel.dto;

public record ExportCatalogoResultDTO(
        byte[] archivo,
        String nombreArchivo
) {
}
