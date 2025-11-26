package ar.com.leo.super_master_backend.dominio.marca.dto;

public record MarcaCreateDTO(
        String nombre,
        Integer padreId
) {
}