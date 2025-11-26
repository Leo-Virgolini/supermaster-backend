package ar.com.leo.super_master_backend.dominio.proveedor.dto;

public record ProveedorCreateDTO(
        String proveedor,
        String apodo,
        String plazoPago,
        Boolean entrega
) {
}