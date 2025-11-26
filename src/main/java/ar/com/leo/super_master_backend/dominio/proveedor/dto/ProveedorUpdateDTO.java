package ar.com.leo.super_master_backend.dominio.proveedor.dto;

public record ProveedorUpdateDTO(
        String proveedor,
        String apodo,
        String plazoPago,
        Boolean entrega
) {
}