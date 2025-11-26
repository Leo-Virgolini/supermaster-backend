package ar.com.leo.super_master_backend.dominio.marca.dto;

public record MarcaUpdateDTO(
        String nombre,
        Integer padreId
) {
}