package ar.com.leo.super_master_backend.dominio.nube.dto;

public record StockNubeDTO(
        String sku,
        int stock,
        String store
) {}
