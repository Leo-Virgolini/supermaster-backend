package ar.com.leo.super_master_backend.dominio.tipo.dto;

public record TipoUpdateDTO(
        String nombre,
        Integer padreId
) {
}