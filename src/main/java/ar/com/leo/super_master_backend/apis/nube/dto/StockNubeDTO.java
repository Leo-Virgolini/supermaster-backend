package ar.com.leo.super_master_backend.apis.nube.dto;

public record StockNubeDTO(
        String sku,
        int stock,
        String store
) {}
